/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;

public class FTA {
	TObjectIntMap<Transition> transitionCount = new TObjectIntHashMap<>();
	TIntIntMap finalStateCount = new TIntIntHashMap(11, 0.75f, -1, -1);
	private final TimedInput input;
	private final Set<ZeroProbTransition> transitions = new LinkedHashSet<>();
	private final Logger logger = LoggerFactory.getLogger(FTA.class);
	int nextStateIndex = PDFA.START_STATE + 1;
	TIntStack determinizeStack = new TIntArrayStack();
	public FTA(TimedInput input) {
		this.input = input;
		finalStateCount.put(PDFA.START_STATE, 0);
		for (final TimedWord word : input) {
			this.add(word);
		}
	}

	public Transition getTransition(int currentState, String event) {
		Transition result = null;
		if (event.equals(Transition.STOP_TRAVERSING_SYMBOL)) {
			result = getFinalTransition(currentState);
		} else {
			for (final Transition t : transitions) {
				if (t.getFromState() == currentState && t.getSymbol().equals(event)) {
					if (result != null) {
						logger.error("Found more than one transition for state " + currentState + " and event " + event);
					}
					result = t;
				}
			}
		}
		return result;
	}

	private void add(TimedWord word) {
		int currentState = PDFA.START_STATE;
		for (int i = 0; i < word.length(); i++) {
			final String symbol = word.getSymbol(i);
			Transition t = getTransition(currentState, symbol);
			if (t == null) {
				finalStateCount.put(nextStateIndex, 0);
				t = addTransition(currentState, nextStateIndex, symbol, 0);
				nextStateIndex++;
			}
			transitionCount.adjustOrPutValue(t.toZeroProbTransition(), 1, 1);
			currentState = t.getToState();
		}
		if (!finalStateCount.adjustValue(currentState, 1)) {
			throw new IllegalStateException("Tried to increment the counter for a state that does not exist.");
		}
	}

	public Transition addTransition(int fromState, int toState, String symbol, double probability) {
		final Transition t = new Transition(fromState, toState, symbol, probability);
		transitions.add(t.toZeroProbTransition());
		return t;
	}

	/**
	 * Merges two states. The second state will be removed.
	 * 
	 * @param i
	 * the first state to merge
	 * @param j
	 * the second state to merge
	 */
	public void merge(int i, int j) {
		logger.debug("Merging state {} and {}", i, j);
		// final Pair<List<Transition>, List<Transition>> inOutI = getInOutTransitions(i, false);
		final Pair<List<Transition>, List<Transition>> inOutJ = getInOutTransitions(j, false);
		// final List<Transition> inTransitionsI = inOutI.getKey();
		// final List<Transition> outTransitionsI = inOutI.getValue();
		final List<Transition> inTransitionsJ = inOutJ.getKey();
		final List<Transition> outTransitionsJ = inOutJ.getValue();

		// int iOutCount = 0;
		// for (final Transition t : outTransitionsI) {
		// iOutCount += transitionCount.get(t.toZeroProbTransition());
		// }
		// iOutCount += finalStateCount.get(i);
		//
		// int jOutCount = 0;
		// for (final Transition t : outTransitionsJ) {
		// jOutCount += transitionCount.get(t.toZeroProbTransition());
		// }
		// jOutCount += finalStateCount.get(j);
		//
		// int iInCount = 0;
		// for (final Transition t : inTransitionsI) {
		// iInCount += transitionCount.get(t.toZeroProbTransition());
		// }
		//
		// int jInCount = 0;
		// for (final Transition t : inTransitionsJ) {
		// jInCount += transitionCount.get(t.toZeroProbTransition());
		// }
		// if (j == 573) {
		// System.out.println();
		// }
		// inputs into j will be inputs into i
		for (final Transition t : inTransitionsJ) {
			removeTransition(t);
			// if (outTransitionsJ.contains(t)) {
			// outTransitionsJ.remove(t);
			// }
			final int jCount = transitionCount.remove(t.toZeroProbTransition());
			final Transition newTrans;
			if (t.getFromState() == j && t.getToState() == j) {
				// transition goes from j into j
				newTrans = new Transition(i, i, t.getSymbol(), 0);
				// only need to handle this transition once, so remove it from outTransitionsJ
				outTransitionsJ.remove(t);

				// final boolean removed = outTransitionsJ.remove(t);
				// System.out.println("" + removed);
			} else {
				newTrans = new Transition(t.getFromState(), i, t.getSymbol(), 0);
			}
			addTransition(newTrans);
			transitionCount.adjustOrPutValue(newTrans.toZeroProbTransition(), jCount, jCount);
		}
		// outputs from j will be outputs from i
		for (final Transition t : outTransitionsJ) {
			if (t.getToState() != j) {
				removeTransition(t);
			}
			final int jCount = transitionCount.remove(t.toZeroProbTransition());
			final Transition newTrans = new Transition(i, t.getToState(), t.getSymbol(), 0);
			addTransition(newTrans);
			transitionCount.adjustOrPutValue(newTrans.toZeroProbTransition(), jCount, jCount);
		}

		final int stopCount = finalStateCount.remove(j);
		if (stopCount < 0) {
			throw new IllegalStateException("Trying to add negative stopCount (" + stopCount + ")to finalStateCount");
		}
		finalStateCount.adjustOrPutValue(i, stopCount, stopCount);
		removeState(j);
		determinizeStack.push(i);
	}

	private void addTransition(Transition newTrans) {
		transitions.add(newTrans.toZeroProbTransition());

	}

	private void removeState(int j) {
		// also remove all transitions from and to state j (at this point there should be no more such transitions)
		for (final String symbol : getAlphabet().getSymbols()) {
			final List<Transition> transList = getTransitions(j, symbol);
			if (!transList.isEmpty()) {
				logger.error("Transition list not empty for state {} and symbol {}", j, symbol);
			}
		}
	}

	protected boolean removeTransition(Transition t) {
		final boolean wasRemoved = transitions.remove(t);
		if (!wasRemoved) {
			logger.warn("Tried to remove a non existing transition={}", t);
		}
		return wasRemoved;
	}


	public void determinize() {
		while (determinizeStack.size() != 0) {
			final int state = determinizeStack.pop();
			if (containsState(state)) {
				logger.trace("Determinizing state {}.", state);
				for (final String event : getAlphabet().getSymbols()) {
					final List<Transition> nonDetTransitions = getTransitions(state, event);
					Collections.sort(nonDetTransitions);
					if (nonDetTransitions.size() >= 2) {
						final int firstState = nonDetTransitions.get(0).getToState();
						logger.debug("Found {} outgoing transititions for state {} and symbol {}", nonDetTransitions.size(), state, event);
						for (int i = 1; i < nonDetTransitions.size(); i++) {
							final int secondState = nonDetTransitions.get(i).getToState();
							merge(firstState, secondState);
						}
					}
				}
			} else {
				logger.debug("Tried to determinize state {}, but it does not exist", state);
			}
		}
		// checkDeterminism();
	}

	public TimedInput getAlphabet() {
		return input;
	}

	/**
	 * Returns all outgoing transitions for a given state
	 * 
	 * @param currentState
	 *            the given state
	 * @param includeStoppingTransition
	 *            whether to include final transition probabilities
	 * @return the outgoing transitions
	 */
	public Pair<List<Transition>, List<Transition>> getInOutTransitions(int currentState, boolean includeStoppingTransition) {
		final List<Transition> outTransitions = new ArrayList<>();
		final List<Transition> inTransitions = new ArrayList<>();

		for (final Transition t : transitions) {
			if (t.getFromState() == currentState) {
				outTransitions.add(t);
			}
			if (t.getToState() == currentState) {
				inTransitions.add(t);
			}
		}
		if (includeStoppingTransition) {
			for (final int state : finalStateCount.keys()) {
				if (state == currentState) {
					outTransitions.add(getFinalTransition(state));
				}
			}
		}
		return Pair.create(inTransitions, outTransitions);
	}

	protected Transition getFinalTransition(int state) {
		return new Transition(state, state, Transition.STOP_TRAVERSING_SYMBOL, 0);
	}

	protected List<Transition> getTransitions(int state, String event) {
		final List<Transition> result = new ArrayList<>();
		if (event.equals(Transition.STOP_TRAVERSING_SYMBOL)) {
			result.add(getFinalTransition(state));
		} else {
			for (final Transition t : transitions) {
				if (t.getFromState() == state && t.getSymbol().equals(event)) {
					result.add(t);
				}
			}
		}
		return result;
	}

	public PDFA toPdfa() {
		final Set<Transition> probTransitions = new HashSet<>();
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		final TIntIntMap stateOcurrenceCount = new TIntIntHashMap(finalStateCount.size());
		for (final Transition t : transitions) {
			final int value = transitionCount.get(t.toZeroProbTransition());
			stateOcurrenceCount.adjustOrPutValue(t.getFromState(), value, value);
		}
		for (final int state : finalStateCount.keys()) {
			final int value = finalStateCount.get(state);
			stateOcurrenceCount.adjustOrPutValue(state, value, value);
			final int stateVisits = stateOcurrenceCount.get(state);
			final double finalProb = (double) finalStateCount.get(state) / stateVisits;
			if (finalProb < 0) {
				throw new IllegalStateException("Final probability=" + finalProb + " for state=" + state + " with finalStateCount=" + finalStateCount.get(state)
				+ " and stateVisits=" + stateVisits);
			}
			finalStateProbabilities.put(state, finalProb);

		}
		for (final Transition t : transitions) {
			final int stateVisits = stateOcurrenceCount.get(t.getFromState());
			final int transitionVisits = transitionCount.get(t.toZeroProbTransition());
			probTransitions.add(new Transition(t.getFromState(), t.getToState(), t.getSymbol(), (double) transitionVisits / stateVisits));
		}
		return new PDFA(getAlphabet(), probTransitions, finalStateProbabilities, null);
	}

	public void cleanUp() {
		logger.info("{} states before cleanup", getStateCount());

		final TIntSet reachableStates = new TIntHashSet(finalStateCount.size());

		final TIntStack stateStack = new TIntArrayStack();

		stateStack.push(PDFA.START_STATE);

		while (stateStack.size() != 0) {
			final int currentState = stateStack.pop();
			logger.trace("Processing state {}.", currentState);
			reachableStates.add(currentState);
			for (final String event : getAlphabet().getSymbols()) {
				final Transition t = getTransition(currentState, event);
				if (t != null && getTransitionCount(t) > 0 && t.getToState() != currentState && !reachableStates.contains(t.getToState())) {
					stateStack.push(t.getToState());
				}
			}
		}
		logger.debug("Found {} reachable states. Removing the others.", reachableStates.size());
		final TIntSet statesToRemove = new TIntHashSet();
		for (final int state : finalStateCount.keys()) {
			if (!reachableStates.contains(state)) {
				statesToRemove.add(state);
			}
		}
		for (final int state : statesToRemove.toArray()) {
			finalStateCount.remove(state);
		}
		if (finalStateCount.size() != reachableStates.size()) {
			final TIntSet missingStates = new TIntHashSet();
			logger.error("Number of reachable states ({}) and all states ({}) must be the same", reachableStates.size(), finalStateCount.size());
			if (finalStateCount.size() > reachableStates.size()) {
				missingStates.addAll(finalStateCount.keys());
				missingStates.removeAll(reachableStates);
				throw new IllegalStateException("The following states were not reachable, but final: " + missingStates);
			} else {
				missingStates.addAll(reachableStates);
				missingStates.removeAll(finalStateCount.keys());
				throw new IllegalStateException("The following states were not final, but reachable: " + missingStates);

			}

		}
		logger.info("{} states after cleanup", getStateCount());
	}

	public int getStateCount() {
		return finalStateCount.size();
	}

	public int getTransitionCount(Transition transition) {
		return transitionCount.get(transition);
	}

	public Collection<ZeroProbTransition> getAllTransitions() {
		return transitions;
	}

	public int getFinalStateCount(int qu) {
		return finalStateCount.get(qu);
	}

	public TObjectIntMap<Transition> getTransitionCount() {
		return transitionCount;
	}

	public boolean containsState(int i) {
		return finalStateCount.containsKey(i);
	}

	public List<Transition> getTransitionsToSucc(int state) {
		final List<Transition> result = new ArrayList<>();
		final Pair<List<Transition>, List<Transition>> succTransitions = getInOutTransitions(state, false);
		for (final Transition outTransition : succTransitions.getValue()) {
			if (outTransition.getToState() != state) {
				result.add(outTransition);
			}
		}
		return result;
	}

	public void checkDeterminism(){
		for(final int state : finalStateCount.keys()){
			for(final String event : getAlphabet().getSymbols()){
				if(getTransitions(state, event).size()>1){
					throw new IllegalStateException("PTA is not deterministic because more than one transition was found for state=" + state + " and event="
							+ event + " (" + getTransitions(state, event) + ")");
				}
			}
		}
	}
}
