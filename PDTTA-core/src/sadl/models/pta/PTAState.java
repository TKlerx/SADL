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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import jsat.utils.Pair;
import sadl.constants.EventsCreationStrategy;

public class PTAState implements Cloneable {

	protected int id;
	protected PTA pta;
	protected String word;
	protected PTAState father;
	protected LinkedHashMap<String, TIntObjectMap<PTATransition>> inTransitions = new LinkedHashMap<>();
	protected LinkedHashMap<String, PTATransition> outTransitions = new LinkedHashMap<>();

	protected PTAState mergedWith;
	protected boolean marked = false;

	protected TIntObjectMap<PTAState> compatibilityCheckingStates = new TIntObjectHashMap<>();

	private static int idCounter = 0;

	public PTAState(String word, PTAState father, PTA pta) {
		this.id = idCounter++;
		this.word = word;
		this.father = father;
		this.pta = pta;
	}

	public PTA getPTA() {

		return pta;
	}

	public int getId() {

		return id;
	}

	public PTAState isMergedWith() {

		if (!mergedWith.exists()) {
			mergedWith = mergedWith.isMergedWith();
		}

		return mergedWith;
	}

	public void setMergedWith(PTAState state) {

		this.mergedWith = state;
	}

	public PTATransition getTransition(String symbol) {

		return outTransitions.get(symbol);
	}

	public Collection<TIntObjectMap<PTATransition>> getInTransitions() {

		return inTransitions.values();
	}

	public Collection<PTATransition> getOutTransitions() {

		return outTransitions.values();
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
		for (final TIntObjectMap<PTATransition> transitionsByEvent : inTransitions.values()) {
			for (final PTATransition transition : transitionsByEvent.valueCollection()) {
				sum += transition.getCount();
			}
		}

		return sum;
	}

	public int getInTransitionsCount(String eventSymbol) {

		final TIntObjectMap<PTATransition> transitions = inTransitions.get(eventSymbol);

		if (transitions == null) {
			return 0;
		}

		int sum = 0;
		for (final PTATransition transition : transitions.valueCollection()) {
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

	public int getEndCount() {

		final int inTransitionsCount = getInTransitionsCount();
		final int outTransitionsCount = getOutTransitionsCount();

		if (inTransitionsCount < outTransitionsCount) {
			return 0;
		}

		return inTransitionsCount - outTransitionsCount;
	}

	public PTAState getFatherState() {

		return father;
	}

	public double getEndProbability() {

		final int outTransitionsCount = this.getOutTransitionsCount();
		final int endCount = this.getEndCount();

		if (outTransitionsCount == 0) {
			return 1.0d;
		}

		return (double) endCount / (outTransitionsCount + endCount);
	}

	public boolean exists() {

		if (mergedWith == null) {
			return true;
		}

		return false;
	}

	public boolean isMarked() {

		return marked;
	}

	public void mark() {

		marked = true;
	}

	public static void merge(PTAState firstState, PTAState secondState, EventsCreationStrategy strategy) {

		if (!firstState.exists()) {
			firstState = firstState.isMergedWith();
		}

		if (!secondState.exists()) {
			secondState = secondState.isMergedWith();
		}

		if (firstState == secondState) {
			return;
		}

		ArrayList<PTATransition> transitionsToAdd = new ArrayList<>();
		ArrayList<PTATransition> transitionsToRemove = new ArrayList<>();
		final ArrayList<Pair<PTAState, PTAState>> statesToMerge = new ArrayList<>();

		// Merge incoming transitions
		for (final TIntObjectMap<PTATransition> secondStateEventInTransitions : secondState.inTransitions.values()) {
			for (final PTATransition redundantInTransition : secondStateEventInTransitions.valueCollection()) {
				transitionsToAdd.add(new PTATransition(redundantInTransition.getSource(), firstState, redundantInTransition.getEvent(), redundantInTransition
						.getCount()));
				transitionsToRemove.add(redundantInTransition);
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);

		transitionsToAdd = new ArrayList<>();
		transitionsToRemove = new ArrayList<>();

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
		secondState.setMergedWith(firstState);
		PTAState.merge(statesToMerge, strategy);

		if (strategy == EventsCreationStrategy.IsolateCriticalAreasMergeInProcess) {
			firstState.removeCriticalTransitions();
		}
	}

	public void removeCriticalTransitions(){

		final ArrayList<PTATransition> transitionsToRemove = new ArrayList<>();
		final ArrayList<Pair<PTAState,PTAState>> statesToMerge = new ArrayList<>();

		for (final PTATransition transition : outTransitions.values()) {
			final SubEvent event = transition.getEvent();
			if (event instanceof SubEventCriticalArea) {
				final SubEventCriticalArea criticalEvent = (SubEventCriticalArea) event;

				final SubEvent leftEvent = criticalEvent.getPreviousSubEvent();
				final SubEvent rightEvent = criticalEvent.getNextSubEvent();
				final PTATransition leftEventTransition = outTransitions.get(leftEvent.getSymbol());
				final PTATransition rightEventTransition = outTransitions.get(rightEvent.getSymbol());

				if (leftEventTransition == null && transition.getCount() * 0.1 > criticalEvent.getAlmostSurelyCountPrev()) {
					if (rightEventTransition != null) {
						transitionsToRemove.add(transition);
						statesToMerge.add(new Pair<>(rightEventTransition.target, transition.target));
					}
				} else if (rightEventTransition == null && transition.getCount() * 0.1 > criticalEvent.getAlmostSurelyCountNext()) {
					if (leftEventTransition != null) {
						transitionsToRemove.add(transition);
						statesToMerge.add(new Pair<>(leftEventTransition.target, transition.target));
					}
				}
			}

		}

		if (!transitionsToRemove.isEmpty()) {
			PTATransition.remove(transitionsToRemove);
			PTAState.merge(statesToMerge, EventsCreationStrategy.IsolateCriticalAreasMergeInProcess);
		}
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

		for (final TIntObjectMap<PTATransition> transitions : inTransitions.values()) {
			for (final PTATransition transition : transitions.valueCollection()) {
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
