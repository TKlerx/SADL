/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.models.pta;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import jsat.utils.Pair;
import sadl.constants.EventsCreationStrategy;

public class PTAState implements Cloneable {

	protected int id;
	protected PTA pta;
	protected String word;
	protected PTAState father;
	protected LinkedHashMap<String, LinkedHashMap<Integer, PTATransition>> inTransitions = new LinkedHashMap<>();
	protected LinkedHashMap<String, PTATransition> outTransitions = new LinkedHashMap<>();

	protected PTAState mergedWith;
	protected boolean marked = false;

	protected HashMap<Integer, PTAState> compatibilityCheckingStates = new HashMap<>();

	private static int idCounter = 0;

	public PTAState(String word, PTAState father, PTA pta) {
		this.id = idCounter++;
		this.word = word;
		this.father = father;
		this.pta = pta;

		for (final Event event : pta.getEvents().values()) {
			for (final SubEvent subEvent : event) {
				inTransitions.put(subEvent.getSymbol(), new LinkedHashMap<Integer, PTATransition>());
			}
		}
	}

	public PTA getPTA() {

		return pta;
	}

	public PTATransition getTransition(String symbol) {

		return outTransitions.get(symbol);
	}

	public String getWord() {

		return word;
	}

	public PTAState getNextState(String symbol) {

		final PTATransition transition = this.getTransition(symbol);

		if (transition == null) {
			return null;
		}

		return transition.getTarget();
	}

	public int getInTransitionsCount() {

		int sum = 0;
		for (final LinkedHashMap<Integer, PTATransition> transitionsByEvent : inTransitions.values()) {
			for (final PTATransition transition : transitionsByEvent.values()) {
				sum += transition.getCount();
			}
		}

		return sum;
	}

	public int getInTransitionsCount(String eventSymbol) {

		final LinkedHashMap<Integer, PTATransition> transitions = inTransitions.get(eventSymbol);

		if (transitions == null) {
			return 0;
		}

		int sum = 0;
		for (final PTATransition transition : transitions.values()) {
			sum += transition.getCount();
		}

		return sum;
	}

	public int getOutTransitionsCount() {

		int sum = 0;
		for (final PTATransition transition : outTransitions.values()) {
			sum += transition.getCount();
		}

		return sum;
	}

	public int getOutTransitionsCount(String eventSymbol) {

		final PTATransition transition = outTransitions.get(eventSymbol);

		if (transition == null) {
			return 0;
		} else {
			return transition.getCount();
		}
	}

	public PTAState getFatherState() {

		return father;
	}

	public double getEndProbability() {

		final int inTransitionsCount = this.getInTransitionsCount();
		final int outTransitionsCount = this.getOutTransitionsCount();
		final int endCount = inTransitionsCount - outTransitionsCount;

		if (endCount <= 0) {
			return 0.0d;
		} else if (outTransitionsCount == 0) {
			return 1.0d;
		}

		return (double) endCount / outTransitionsCount;
	}

	public boolean isRemoved() {

		if (mergedWith == null) {
			return false;
		}

		return true;
	}

	public boolean isMarked() {

		return marked;
	}

	public void mark() {

		marked = true;
	}

	public static void merge(PTAState firstState, PTAState secondState, EventsCreationStrategy strategy) {

		if (firstState.isRemoved()) {
			firstState = firstState.isMergedWith();
		}

		if (secondState.isRemoved()) {
			secondState = secondState.isMergedWith();
		}

		if (firstState == secondState) {
			return;
		}

		LinkedList<PTATransition> transitionsToAdd = new LinkedList<>();
		LinkedList<PTATransition> transitionsToRemove = new LinkedList<>();
		final LinkedList<Pair<PTAState, PTAState>> statesToMerge = new LinkedList<>();

		// Merge incoming transitions
		for (final LinkedHashMap<Integer, PTATransition> secondStateEventInTransitions : secondState.inTransitions.values()) {
			for (final PTATransition redundantInTransition : secondStateEventInTransitions.values()) {
				transitionsToAdd.add(new PTATransition(redundantInTransition.getSource(), firstState, redundantInTransition.getEvent(), redundantInTransition
						.getCount()));
				transitionsToRemove.add(redundantInTransition);
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);

		transitionsToAdd = new LinkedList<>();
		transitionsToRemove = new LinkedList<>();

		// Merge outgoing transitions
		for (final PTATransition redundantTransition : secondState.outTransitions.values()) {
			final String eventSymbol = redundantTransition.getEvent().getSymbol();
			PTATransition transition = firstState.outTransitions.get(eventSymbol);

			if (transition == null) {
				transition = new PTATransition(firstState, redundantTransition.getTarget(), redundantTransition.getEvent(), redundantTransition.getCount());
				transitionsToAdd.add(transition);
				transitionsToRemove.add(redundantTransition);
			} else {
				transition.incrementCount(redundantTransition.getCount());
				transitionsToRemove.add(redundantTransition);
				statesToMerge.add(new Pair<>(transition.getTarget(), redundantTransition.getTarget()));
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);
		secondState.mergedWith = firstState;
		PTAState.merge(statesToMerge, strategy);

		if (strategy == EventsCreationStrategy.SplitEventsIsolateCriticalAreasMergeInProcess) {
			firstState.removeCriticalTransitions();
		}
	}

	public void removeCriticalTransitions(){

		final LinkedList<PTATransition> transitionsToRemove = new LinkedList<>();
		final LinkedList<Pair<PTAState,PTAState>> statesToMerge = new LinkedList<>();

		for (final PTATransition transition : outTransitions.values()) {
			final SubEvent event = transition.getEvent();
			if (event instanceof SubEventCriticalArea) {
				final SubEventCriticalArea criticalEvent = (SubEventCriticalArea) event;

				if (transition.getCount() > criticalEvent.getAlmostSurelyCount()) {

					final SubEvent leftEvent = criticalEvent.getPreviousSubEvent();
					final SubEvent rightEvent = criticalEvent.getNextSubEvent();
					final PTATransition leftEventTransition = outTransitions.get(leftEvent.getSymbol());
					final PTATransition rightEventTransition = outTransitions.get(rightEvent.getSymbol());

					if (leftEventTransition == null) {
						if (rightEventTransition != null) {
							transitionsToRemove.add(transition);
							statesToMerge.add(new Pair<>(rightEventTransition.target, transition.target));
						}
					} else if (rightEventTransition == null) {
						transitionsToRemove.add(transition);
						statesToMerge.add(new Pair<>(leftEventTransition.target, transition.target));
					}
				}

			}
		}

		PTATransition.remove(transitionsToRemove);
		PTAState.merge(statesToMerge, EventsCreationStrategy.SplitEventsIsolateCriticalAreasMergeInProcess);
	}

	public int getId() {

		return id;
	}

	public PTAState isMergedWith() {

		if (mergedWith.isRemoved()) {
			return mergedWith.isMergedWith();
		}

		return mergedWith;
	}



	public static void merge(Collection<Pair<PTAState, PTAState>> statesToMerge, EventsCreationStrategy strategy) {
		for (final Pair<PTAState, PTAState> statePair : statesToMerge) {
			PTAState.merge(statePair.getFirstItem(), statePair.getSecondItem(), strategy);
		}
	}

	public static void setCompatibilityChecking(PTAState firstState, PTAState secondState) {

		if (firstState.getId() < secondState.getId() ) {
			firstState.compatibilityCheckingStates.put(secondState.getId(), secondState);
		} else if (secondState.getId() < firstState.getId() ) {
			secondState.compatibilityCheckingStates.put(firstState.getId(), firstState);
		}

	}

	public static void unsetCompatibilityChecking(PTAState firstState, PTAState secondState) {

		final PTAState removedState = null;

		if (firstState.getId() < secondState.getId()) {
			firstState.compatibilityCheckingStates.remove(secondState.getId());
		} else if (secondState.getId() < firstState.getId()) {
			secondState.compatibilityCheckingStates.remove(firstState.getId());
		}

	}

	public static boolean compatibilityIsChecking(PTAState firstState, PTAState secondState) {

		if (firstState.getId() < secondState.getId() && firstState.compatibilityCheckingStates.containsKey(secondState.getId())) {
			return true;
		} else if (secondState.getId() < firstState.getId() && secondState.compatibilityCheckingStates.containsKey(firstState.getId())) {
			return true;
		}

		return false;
	}

	@Override
	public String toString(){

		final StringBuilder stringbuilder = new StringBuilder(50);

		stringbuilder.append("State " + this.id + "(in: ");

		for (final LinkedHashMap<Integer, PTATransition> transitions : inTransitions.values()) {
			for (final PTATransition transition : transitions.values()) {
				stringbuilder.append(transition + " ");
			}
		}

		stringbuilder.append(", out: ");

		for (final PTATransition transition : outTransitions.values()) {
			stringbuilder.append(transition + " ");
		}

		stringbuilder.append(")");

		return stringbuilder.toString();
	}

}
