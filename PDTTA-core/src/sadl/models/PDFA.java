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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.structure.AbnormalTransition;
import sadl.structure.Transition;
import sadl.structure.UntimedSequence;
import sadl.utils.MasterSeed;

/**
 * A Probabilistic Deterministic Finite Automaton (PDFA).
 * 
 * @author Timo Klerx
 *
 */
public class PDFA implements AutomatonModel, Serializable {

	private static final long serialVersionUID = -3584763240370878883L;

	public static final int START_STATE = 0;

	transient private static Logger logger = LoggerFactory.getLogger(PDFA.class);
	// TODO maybe change Set<Transition> transitions to Map<State,Set<Transition>>
	protected Random r = MasterSeed.nextRandom();

	public static final double NO_TRANSITION_PROBABILITY = 0;

	protected TimedInput alphabet;
	protected Set<Transition> transitions = new HashSet<>();
	protected TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
	protected TIntSet abnormalFinalStates = new TIntHashSet();

	protected boolean immutable = false;

	protected void makeMutable() {
		immutable = false;
	}

	public void makeImmutable() {
		immutable = true;
	}

	public boolean isImmutable() {
		return immutable;
	}

	/**
	 * 
	 * @return true if was not consistent and consistency was restored.
	 */
	public boolean checkAndRestoreConsistency() {
		if (!isConsistent()) {
			logger.info("Probabilities do not match, but will be corrected");
			return restoreConsistency();
		}
		return false;
	}

	public boolean restoreConsistency() {
		return fixProbabilities();
	}

	protected boolean isConsistent() {
		final boolean probabilities = finalStateProbabilities.keySet().forEach(state -> checkProbability(state));
		return probabilities;
	}

	protected boolean checkProbability(int state) {
		final List<Transition> outgoingTransitions = getOutTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		final boolean compareResult = Precision.equals(sum, 1);
		logger.trace("Probability sum for state {}: {} (== 1? {})", state, sum, compareResult);
		return compareResult;
	}

	protected boolean fixProbabilities() {
		final boolean fixedProbs = finalStateProbabilities.keySet().forEach(state -> fixProbability(state));
		if (fixedProbs) {
			logger.info("Probabilities were corrected.");
		}
		return fixedProbs;
	}

	protected boolean fixProbability(int state) {
		List<Transition> outgoingTransitions = getOutTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		// divide every probability by the sum of probabilities s.t. they sum up to 1
		if (!Precision.equals(sum, 1)) {
			logger.debug("Sum of transition probabilities for state {} is {}", state, sum);
			outgoingTransitions = getOutTransitions(state, true);
			outgoingTransitions.forEach(t -> changeTransitionProbability(t, t.getProbability() / sum));
			outgoingTransitions = getOutTransitions(state, true);
			final double newSum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
			logger.debug("Corrected sum of transition probabilities is {}", newSum);
			if (!Precision.equals(newSum, 1.0)) {
				logger.debug("Probabilities do not sum up to one, so doing it again with the Fraction class");
				final List<BigFraction> probabilities = new ArrayList<>(outgoingTransitions.size());
				for (int i = 0; i < outgoingTransitions.size(); i++) {
					probabilities.add(i, new BigFraction(outgoingTransitions.get(i).getProbability()));
				}
				BigFraction fracSum = BigFraction.ZERO;
				for (final BigFraction f : probabilities) {
					try {
						fracSum = fracSum.add(f);
					} catch (final MathArithmeticException e) {
						logger.error("Arithmetic Exception for fracSum={}, FractionToAdd={}", fracSum, f, e);
						throw e;
					}
				}
				for (int i = 0; i < outgoingTransitions.size(); i++) {
					changeTransitionProbability(outgoingTransitions.get(i), probabilities.get(i).divide(fracSum).doubleValue());
					// outgoingTransitions.get(i).setProbability(probabilities.get(i).divide(fracSum).doubleValue());
				}
				final double tempSum = getOutTransitions(state, true).stream().mapToDouble(t -> t.getProbability()).sum();
				if (!Precision.equals(tempSum, 1.0)) {
					BigFraction preciseSum = BigFraction.ZERO;
					for (final BigFraction f : probabilities) {
						preciseSum = preciseSum.add(f.divide(fracSum));
					}
					if (!preciseSum.equals(BigFraction.ONE)) {
						throw new IllegalStateException("Probabilities do not sum up to one, but instead to " + tempSum);
					} else {
						logger.warn(
								"Probabilities do not sum up to one, but instead to {}. This is due to double underflows, but they sum up to one if using BigFraction. This small error will be ignored.",
								tempSum);
					}
				}
			}
		}
		return true;
	}

	protected void changeTransitionProbability(Transition transition, double newProbability) {
		checkImmutable();
		if (!transition.isStopTraversingTransition()) {
			removeTransition(transition);
			addTransition(transition.getFromState(), transition.getToState(), transition.getSymbol(), newProbability);
		} else {
			final double adjusted = finalStateProbabilities.put(transition.getFromState(), newProbability);
			if (Double.doubleToLongBits(adjusted) == Double.doubleToLongBits(finalStateProbabilities.getNoEntryValue())) {
				logger.warn("Was not possible to adjust final state prob for transition {} with new probability={}", transition, newProbability);
			}
		}
	}

	protected void checkImmutable() {
		if (isImmutable()) {
			throw new IllegalStateException(this.getClass() + " is immutable and cannot be changed anymore");
		}
	}

	protected PDFA() {
	}

	public PDFA(Path trebaPath, TimedInput trainingSequences) throws IOException {
		try (BufferedReader inputReader = Files.newBufferedReader(trebaPath, StandardCharsets.UTF_8)) {
			this.alphabet = trainingSequences;
			String line = "";
			// 172 172 3 0,013888888888888892
			// from state ; to state ; symbol ; probability
			while ((line = inputReader.readLine()) != null) {
				final String[] lineSplit = line.split(" ");
				if (lineSplit.length == 4) {
					final int fromState = Integer.parseInt(lineSplit[0]);
					final int toState = Integer.parseInt(lineSplit[1]);
					final String symbol;
					if (alphabet == null) {
						symbol = lineSplit[2];
					} else {
						symbol = trainingSequences.getSymbol(Integer.parseInt(lineSplit[2]));
					}
					final double probability = Double.parseDouble(lineSplit[3]);
					addTransition(fromState, toState, symbol, probability);
				} else if (lineSplit.length == 2) {
					final int state = Integer.parseInt(lineSplit[0]);
					final double finalProb = Double.parseDouble(lineSplit[1]);
					addFinalState(state, finalProb);
				}
			}
			inputReader.close();
		}
	}

	public PDFA(Path trebaPath) throws IOException {
		this(trebaPath, null);
	}

	protected PDFA(PDFA pdfa) {
		this.alphabet = pdfa.alphabet;
		this.transitions = pdfa.transitions;
		this.finalStateProbabilities = pdfa.finalStateProbabilities;
		this.abnormalFinalStates = pdfa.abnormalFinalStates;
	}

	public PDFA(TimedInput alphabet, Set<Transition> transitions, TIntDoubleMap finalStateProbabilities) {
		this(alphabet, transitions, finalStateProbabilities, null);
	}

	public PDFA(TimedInput alphabet, Set<Transition> transitions, TIntDoubleMap finalStateProbabilities, TIntSet abnormalFinalStates) {
		this.alphabet = alphabet;
		this.transitions = transitions;
		this.finalStateProbabilities = finalStateProbabilities;
		if (abnormalFinalStates == null) {
			this.abnormalFinalStates = new TIntHashSet();
		} else {
			this.abnormalFinalStates = abnormalFinalStates;
		}
	}

	@Override
	public int getTransitionCount() {
		return transitions.size();
	}

	public Transition addTransition(int fromState, int toState, String symbol, double probability) {
		checkImmutable();
		addState(fromState);
		addState(toState);
		final Transition t = new Transition(fromState, toState, symbol, probability);
		transitions.add(t);
		return t;
	}

	protected Transition addAbnormalTransition(int fromState, int toState, String symbol, double probability, AnomalyInsertionType anomalyType) {
		checkImmutable();
		addState(fromState);
		addState(toState);
		final Transition t = new AbnormalTransition(fromState, toState, symbol, probability, anomalyType);
		transitions.add(t);
		return t;
	}

	protected Transition addAbnormalTransition(Transition t, AnomalyInsertionType anomalyType) {
		return addAbnormalTransition(t.getFromState(), t.getToState(), t.getSymbol(), t.getProbability(), anomalyType);
	}

	public void addState(int state) {
		checkImmutable();
		if (!finalStateProbabilities.containsKey(state)) {
			// finalStateProbabilities is also the set of states. so add the state to this set with a probability of zero
			addFinalState(state, NO_TRANSITION_PROBABILITY);
		}
	}

	protected Transition getFinalTransition(int state) {
		if (abnormalFinalStates.contains(state)) {
			return new AbnormalTransition(state, state, Transition.STOP_TRAVERSING_SYMBOL, finalStateProbabilities.get(state), AnomalyInsertionType.TYPE_FIVE);
		} else {
			return new Transition(state, state, Transition.STOP_TRAVERSING_SYMBOL, finalStateProbabilities.get(state));
		}
	}

	public void toGraphvizFile(Path graphvizResult, boolean compressed, Map<String, String> idReplacement) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(graphvizResult, StandardCharsets.UTF_8)) {
			writer.write("digraph G {\n");
			// start states
			writer.write("qi [shape = point ];");
			// write states
			for (final int state : finalStateProbabilities.keys()) {
				writer.write(Integer.toString(state));
				writer.write(" [shape=");
				final boolean abnormal = getFinalTransition(state).isAbnormal();
				final double finalProb = getFinalStateProbability(state);
				if (finalProb > 0 || (compressed && finalProb > 0.01)) {
					writer.write("double");
				}
				writer.write("circle");
				if (abnormal) {
					writer.write(", color=red");
				}
				if (finalProb > 0 || (compressed && finalProb > 0.01)) {
					writer.write(", label=\"");
					writer.write(Integer.toString(state));
					writer.write("&#92;np= ");
					writer.write(Double.toString(Precision.round(finalProb, 2)));
					writer.write("\"");
				}
				writer.write("];\n");
			}
			writer.write("qi -> 0;");
			// write transitions
			for (final Transition t : transitions) {
				if (compressed && t.getProbability() <= 0.01) {
					continue;
				}
				// 0 -> 0 [label=0.06];
				writer.write(Integer.toString(t.getFromState()));
				writer.write(" -> ");
				writer.write(Integer.toString(t.getToState()));
				writer.write(" [label=<");
				if (idReplacement != null && idReplacement.containsKey(t.getSymbol())) {
					writer.write(idReplacement.get(t.getSymbol()));
				} else {
					writer.write(t.getSymbol());
				}
				if (t.getProbability() > 0) {
					writer.write(" p=");
					writer.write(Double.toString(Precision.round(t.getProbability(), 2)));
				}
				if (t.isAbnormal()) {
					writer.write("<BR/>");
					writer.write("<FONT COLOR=\"red\">");
					writer.write(Integer.toString(t.getAnomalyInsertionType().getTypeIndex()));
					writer.write("</FONT>");
				}
				writer.write(">");
				if (t.isAbnormal()) {
					writer.write(" color=\"red\"");
				}
				writer.write(";];\n");

			}
			writer.write("}");
			writer.flush();
			writer.close();
		}

	}

	public void toGraphvizFile(Path graphvizResult, boolean compressed) throws IOException {
		toGraphvizFile(graphvizResult, compressed, Collections.emptyMap());
	}

	public void addFinalState(int state, double probability) {
		checkImmutable();
		finalStateProbabilities.put(state, probability);
	}

	protected void addAbnormalFinalState(int state, double probability) {
		checkImmutable();
		addFinalState(state, probability);
		abnormalFinalStates.add(state);
	}

	public double getFinalStateProbability(int state) {
		return finalStateProbabilities.get(state);
	}

	public double getTransitionProbability(int fromState, int toState, String symbol) {
		for (final Transition t : transitions) {
			if (t.getFromState() == fromState && t.getToState() == toState && t.getSymbol().equals(symbol)) {
				return t.getProbability();
			}
		}
		return NO_TRANSITION_PROBABILITY;
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

	/**
	 * Returns all outgoing transitions for a given state
	 * 
	 * @param currentState
	 *            the given state
	 * @param includeStoppingTransition
	 *            whether to include final transition probabilities
	 * @return the outgoing transitions
	 */
	public List<Transition> getOutTransitions(int currentState, boolean includeStoppingTransition) {
		final List<Transition> result = new ArrayList<>();
		for (final Transition t : transitions) {
			if (t.getFromState() == currentState) {
				result.add(t);
			}
		}
		if (includeStoppingTransition) {
			for (final int state : finalStateProbabilities.keys()) {
				if (state == currentState) {
					result.add(getFinalTransition(state));
				}
			}
		}
		return result;
	}

	public boolean removeTransition(Transition t) {
		checkImmutable();
		final boolean wasRemoved = transitions.remove(t);
		if (!wasRemoved) {
			logger.warn("Tried to remove a non existing transition={}", t);
		}
		return wasRemoved;
	}

	protected static final int MAX_SEQUENCE_LENGTH = 1000;

	public TimedWord sampleSequence() {
		int currentState = START_STATE;

		final List<String> eventList = new ArrayList<>();
		boolean choseFinalState = false;
		while (!choseFinalState) {
			final Transition chosenTransition = chooseNextTransition(currentState);
			if (chosenTransition.isStopTraversingTransition()) {
				choseFinalState = true;
			} else if (eventList.size() > MAX_SEQUENCE_LENGTH) {
				throw new IllegalStateException("A sequence longer than " + MAX_SEQUENCE_LENGTH + " events should have been generated");
			} else {
				currentState = chosenTransition.getToState();
				eventList.add(chosenTransition.getSymbol());
			}
		}
		return new TimedWord(eventList, null, ClassLabel.NORMAL);
	}

	protected Transition chooseNextTransition(int currentState) {
		final List<Transition> possibleTransitions = getOutTransitions(currentState, true);
		Collections.sort(possibleTransitions, (t1, t2) -> -Double.compare(t2.getProbability(), t1.getProbability()));
		final double random = r.nextDouble();
		double summedProbs = 0;
		int index = -1;
		for (int i = 0; i < possibleTransitions.size(); i++) {
			summedProbs += possibleTransitions.get(i).getProbability();
			if (random < summedProbs) {
				index = i;
				break;
			}
		}

		final Transition chosenTransition = possibleTransitions.get(index);
		return chosenTransition;
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
			for (final int state : finalStateProbabilities.keys()) {
				if (state == currentState) {
					outTransitions.add(getFinalTransition(state));
				}
			}
		}
		return Pair.create(inTransitions, outTransitions);
	}

	public Random getRandom() {
		return r;
	}

	public void setRandom(Random r) {
		this.r = r;
	}

	public boolean isInAutomaton(TimedWord s) {
		int currentState = START_STATE;
		for (int i = 0; i < s.length(); i++) {
			final String nextEvent = s.getSymbol(i);
			final Transition t = getTransition(currentState, nextEvent);
			if (t == null) {
				return false;
			}
			currentState = t.getToState();
		}
		if (getFinalStateProbability(currentState) > NO_TRANSITION_PROBABILITY) {
			return true;
		} else {
			return false;
		}
	}

	public int getStartState() {
		return START_STATE;
	}

	@Override
	public int getStateCount() {
		return finalStateProbabilities.size();
	}

	protected void removeState(int i) {
		checkImmutable();
		finalStateProbabilities.remove(i);
	}

	public void cleanUp() {
		logger.debug("{} states before cleanup", finalStateProbabilities.size());

		final TIntSet reachableStates = new TIntHashSet(finalStateProbabilities.size());

		final TIntStack stateStack = new TIntArrayStack();

		stateStack.push(PDFA.START_STATE);

		while (stateStack.size() != 0) {
			final int currentState = stateStack.pop();
			logger.trace("Processing state {}.", currentState);
			reachableStates.add(currentState);
			for (final String event : getAlphabet().getSymbols()) {
				final Transition t = getTransition(currentState, event);
				if (t != null && t.getProbability() > 0 && t.getToState() != currentState && !reachableStates.contains(t.getToState())) {
					stateStack.push(t.getToState());
				}
			}
		}
		logger.debug("Found {} reachable states. Removing the others.", reachableStates.size());
		final TIntSet statesToRemove = new TIntHashSet();
		for (final int state : finalStateProbabilities.keys()) {
			if (!reachableStates.contains(state)) {
				statesToRemove.add(state);
			}
		}
		for (final int state : statesToRemove.toArray()) {
			finalStateProbabilities.remove(state);
		}
		if (finalStateProbabilities.size() != reachableStates.size()) {
			final TIntSet missingStates = new TIntHashSet();
			logger.error("Number of reachable states ({}) and all states ({}) must be the same", reachableStates.size(), finalStateProbabilities.size());
			if (finalStateProbabilities.size() > reachableStates.size()) {
				missingStates.addAll(finalStateProbabilities.keys());
				missingStates.removeAll(reachableStates);
				throw new IllegalStateException("The following states were not reachable, but final: " + missingStates);
			} else {
				missingStates.addAll(reachableStates);
				missingStates.removeAll(finalStateProbabilities.keys());
				throw new IllegalStateException("The following states were not final, but reachable: " + missingStates);

			}

		}
		logger.debug("{} states after cleanup", finalStateProbabilities.size());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abnormalFinalStates == null) ? 0 : abnormalFinalStates.hashCode());
		result = prime * result + ((alphabet == null) ? 0 : alphabet.hashCode());
		result = prime * result + ((finalStateProbabilities == null) ? 0 : finalStateProbabilities.hashCode());
		result = prime * result + ((transitions == null) ? 0 : transitions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PDFA)) {
			return false;
		}
		final PDFA other = (PDFA) obj;
		if (abnormalFinalStates == null) {
			if (other.abnormalFinalStates != null) {
				return false;
			}
		} else if (!abnormalFinalStates.equals(other.abnormalFinalStates)) {
			return false;
		}
		if (alphabet == null) {
			if (other.alphabet != null) {
				return false;
			}
		} else if (!alphabet.equals(other.alphabet)) {
			return false;
		}
		if (finalStateProbabilities == null) {
			if (other.finalStateProbabilities != null) {
				return false;
			}
		} else if (!finalStateProbabilities.equals(other.finalStateProbabilities)) {
			return false;
		}
		if (transitions == null) {
			if (other.transitions != null) {
				return false;
			}
		} else if (!transitions.equals(other.transitions)) {
			int count = 0;
			for (final Transition t : transitions) {
				if (!other.transitions.contains(t)) {
					logger.trace("Transition {} not contained in other.transitions", t);
					count++;
				}
			}
			for (final Transition t : other.transitions) {
				if (!transitions.contains(t)) {
					logger.trace("Transition {} not contained in transitions", t);
					count++;
				}
			}
			if (count > 0) {
				logger.debug("{} out of {} transitions did not match", count, transitions.size());
			}
			return false;
		}
		return true;
	}

	protected Set<UntimedSequence> getAllSequences() {
		return getAllSequences(0, new UntimedSequence());
	}

	/**
	 * returns all possible sequences concatenated with the given sequence
	 * 
	 * @param fromState the state from where to gather the sequences
	 * @param s the sequence taken from the root node so far
	 * @return test
	 */
	private Set<UntimedSequence> getAllSequences(int fromState, UntimedSequence s) {
		final Set<UntimedSequence> result = new HashSet<>();
		final List<Transition> outgoingTransitions = getOutTransitions(fromState, true);
		for (final Transition t : outgoingTransitions) {
			if (t.getProbability() > 0) {
				try {
					final UntimedSequence copy = s.clone();
					if (t.isStopTraversingTransition()) {
						result.add(copy);
					} else {
						copy.addEvent(t.getSymbol());
						result.addAll(getAllSequences(t.getToState(), copy));
					}
				} catch (final CloneNotSupportedException e) {
					logger.error("This should never happen.", e);
				}
			}
		}
		return result;
	}

	public int[] getStates() {
		return finalStateProbabilities.keys();
	}

	public TimedInput getAlphabet() {
		return alphabet;
	}

	public void setAlphabet(TimedInput alphabet) {
		this.alphabet = alphabet;
	}

	/**
	 * 
	 * @return the list up to the last probability that exists. list may be shorter than the events list, iff there is an event which has no transition
	 */
	protected TDoubleList computeEventLikelihoods(TimedWord s) {

		final TDoubleList list = new TDoubleArrayList();
		int currentState = START_STATE;
		for (int i = 0; i < s.length(); i++) {
			final Transition t = getTransition(currentState, s.getSymbol(i));
			// DONE this is crap, isnt it? why not return an empty list or null iff there is no transition for the given sequence? or at least put a '0' in the
			// last slot.
			if (t == null) {
				list.add(0);
				return list;
			}
			list.add(t.getProbability());
			currentState = t.getToState();
		}
		list.add(getFinalStateProbability(currentState));
		return list;
	}

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {
		return Pair.create(computeEventLikelihoods(s), new TDoubleArrayList());
	}

	// public boolean isConnected2() {
	// final TIntStack openList = new TIntArrayStack();
	// final TIntSet closedList = new TIntHashSet();
	//
	// openList.push(START_STATE);
	// while (openList.size() > 0) {
	// final int currentState = openList.pop();
	// closedList.add(currentState);
	// final List<Transition> currentTransitions = getOutTransitions(currentState, false);
	// currentTransitions.stream().filter(t -> !closedList.contains(t.getToState())).forEach(t -> openList.push(t.getToState()));
	// }
	// for (final int state : getStates()) {
	// if (!closedList.contains(state)) {
	// return false;
	// }
	// }
	// return true;
	// }
	public boolean isConnected() {
		final TIntStack openList = new TIntArrayStack();
		final TIntSet closedList = new TIntHashSet();

		openList.push(START_STATE);
		while (openList.size() > 0) {
			final int currentState = openList.pop();
			closedList.add(currentState);
			final List<Transition> currentTransitions = getOutTransitions(currentState, false);
			for (final Transition t : currentTransitions) {
				if (!closedList.contains(t.getToState())) {
					openList.push(t.getToState());
				}
			}
		}
		for (final int state : getStates()) {
			if (!closedList.contains(state)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Not yet implemented!
	 * 
	 * @param t
	 * @return
	 */
	public boolean isBridgeTransition(Transition t) {
		throw new UnsupportedOperationException("not yet implemented...");
	}

	public Set<Transition> getTransitions() {
		return transitions;
	}

}
