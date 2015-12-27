package sadl.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.structure.Transition;

public class FTA {
	TObjectIntMap<Transition> transitionCount = new TObjectIntHashMap<>();
	TIntIntMap finalStateCount = new TIntIntHashMap();
	private final TimedInput input;
	private Set<Transition> transitions;
	private Logger logger;
	int nextStateIndex = PDFA.START_STATE + 1;
	public FTA(TimedInput input) {
		this.input = input;
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
		final int currentState = PDFA.START_STATE;
		for (int i = 0; i < word.length(); i++) {
			final String symbol = word.getSymbol(i);
			Transition t = getTransition(currentState, symbol);
			if (t == null) {
				t = addTransition(currentState, nextStateIndex, symbol, 0);
			}
			transitionCount.adjustOrPutValue(t.toZeroProbTransition(), 1, 1);
		}
		finalStateCount.adjustOrPutValue(currentState, 1, 1);
	}

	public Transition addTransition(int fromState, int toState, String symbol, double probability) {
		final Transition t = new Transition(fromState, toState, symbol, probability);
		transitions.add(t);
		return t;
	}

	/**
	 * Merges two states.
	 * 
	 * @param i
	 *            the first state to merge
	 * @param j
	 *            the second state to merge
	 */
	public void merge(int i, int j) {
		final Pair<List<Transition>, List<Transition>> inOutI = getInOutTransitions(i, false);
		final Pair<List<Transition>, List<Transition>> inOutJ = getInOutTransitions(j, false);
		final List<Transition> inTransitionsI = inOutI.getKey();
		final List<Transition> outTransitionsI = inOutI.getValue();
		final List<Transition> inTransitionsJ = inOutJ.getKey();
		final List<Transition> outTransitionsJ = inOutJ.getValue();

		int iOutCount = 0;
		for (final Transition t : outTransitionsI) {
			iOutCount += transitionCount.get(t.toZeroProbTransition());
		}
		iOutCount += finalStateCount.get(i);

		int jOutCount = 0;
		for (final Transition t : outTransitionsJ) {
			jOutCount += transitionCount.get(t.toZeroProbTransition());
		}
		jOutCount += finalStateCount.get(j);

		int iInCount = 0;
		for (final Transition t : inTransitionsI) {
			iInCount += transitionCount.get(t.toZeroProbTransition());
		}

		int jInCount = 0;
		for (final Transition t : inTransitionsJ) {
			jInCount += transitionCount.get(t.toZeroProbTransition());
		}
		// inputs from j will be inputs into i
		for (final Transition t : inTransitionsJ) {
			removeTransition(t);
			final int jCount = transitionCount.remove(t.toZeroProbTransition());
			final Transition newTrans = new Transition(t.getFromState(), i, t.getSymbol(), 0);
			transitionCount.adjustOrPutValue(newTrans.toZeroProbTransition(), jCount, jCount);
		}
		// outputs from j will be outputs from i
		for (final Transition t : outTransitionsJ) {
			removeTransition(t);
			final int jCount = transitionCount.remove(t.toZeroProbTransition());
			final Transition newTrans = new Transition(i, t.getToState(), t.getSymbol(), 0);
			transitionCount.adjustOrPutValue(newTrans.toZeroProbTransition(), jCount, jCount);
		}
		final int stopCount = finalStateCount.remove(j);
		finalStateCount.adjustOrPutValue(i, stopCount, stopCount);
		removeState(j);
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

		final int[] states = finalStateCount.keys();
		Arrays.sort(states);
		for (final int state : states) {
			for (final String event : getAlphabet().getSymbols()) {
				final List<Transition> nonDetTransitions = getTransitions(state, event);
				if (nonDetTransitions.size() == 2) {
					final int firstState = nonDetTransitions.get(0).getToState();
					final int secondState = nonDetTransitions.get(1).getToState();
					merge(firstState, secondState);
				}
			}
		}

	}

	private TimedInput getAlphabet() {
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

}
