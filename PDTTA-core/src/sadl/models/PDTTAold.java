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

package sadl.models;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import jsat.distributions.Distribution;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.structure.AbnormalTransition;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;
import sadl.utils.MasterSeed;

/**
 * A PDTTA with two thresholds for anomaly detection (aggregated event and aggregated time probability).
 * 
 * @author Timo Klerx
 *
 */
public class PDTTAold implements AutomatonModel, Serializable {

	protected static final int START_STATE = 0;

	private static final long serialVersionUID = 3017416753740710943L;

	transient private static Logger logger = LoggerFactory.getLogger(PDTTAold.class);
	// TODO maybe change Set<Transition> transitions to Map<State,Set<Transition>>
	protected Random r = MasterSeed.nextRandom();

	protected static final double NO_TRANSITION_PROBABILITY = 0;

	private static final boolean DELETE_NO_TIME_INFORMATION_TRANSITIONS = true;
	Set<Transition> transitions = new HashSet<>();
	TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
	Map<ZeroProbTransition, Distribution> transitionDistributions = null;
	TIntSet abnormalFinalStates = new TIntHashSet();

	public Map<ZeroProbTransition, Distribution> getTransitionDistributions() {
		return transitionDistributions;
	}

	public void setTransitionDistributions(Map<ZeroProbTransition, Distribution> transitionDistributions) {
		this.transitionDistributions = transitionDistributions;
		restoreConsistency();
	}

	protected void restoreConsistency() {
		if (!isConsistent()) {
			if (DELETE_NO_TIME_INFORMATION_TRANSITIONS) {
				deleteIrrelevantTransitions();
			}
		}
	}

	private boolean isConsistent() {
		if (transitions.size() != transitionDistributions.size()) {
			logger.warn("transitions and transitionDistributions must be of same size! {}!={}", transitions.size(), transitionDistributions.size());
			return false;
		}
		final boolean probabilities = finalStateProbabilities.keySet().forEach(state -> checkProbability(state));
		if (!probabilities) {
			logger.info("Probabilities do not match, but will be corrected");
		}
		return probabilities;
	}

	private boolean checkProbability(int state) {
		final List<Transition> outgoingTransitions = getTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		final boolean compareResult = Precision.equals(sum, 1);
		logger.trace("Probability sum for state {}: {} (== 1? {})", state, sum, compareResult);
		return compareResult;
	}

	private void deleteIrrelevantTransitions() {
		logger.debug("There are {} many transitions before removing irrelevant ones", transitions.size());
		// there may be more transitions than transitionDistributions
		final boolean removedTransitions = transitions.removeIf(t -> !transitionDistributions.containsKey(t.toZeroProbTransition()));
		if (removedTransitions) {
			logger.info("Removed some unnecessary transitions");
		}
		fixProbabilities();
		if (transitions.size() != transitionDistributions.size()) {
			logger.error("This should never happen because trainsitions.size() and transitionDistributions.size() should be equal now, but are not! {}!={}",
					transitions.size(), transitionDistributions.size());
		}
		logger.debug("There are {} many transitions after removing irrelevant ones", transitions.size());
	}

	void fixProbabilities() {
		final boolean fixedProbs = finalStateProbabilities.keySet().forEach(state -> fixProbability(state));
		if (fixedProbs) {
			logger.info("Probabilities were corrected.");
		}
	}

	boolean fixProbability(int state) {
		List<Transition> outgoingTransitions = getTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		// divide every probability by the sum of probabilities s.t. they sum up to 1
		if (!Precision.equals(sum, 1)) {
			logger.debug("Sum of transition probabilities for state {} is {}", state, sum);
			outgoingTransitions = getTransitions(state, true);
			outgoingTransitions.forEach(t -> changeTransitionProbability(t, t.getProbability() / sum));
			outgoingTransitions = getTransitions(state, true);
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
				final double tempSum = getTransitions(state, true).stream().mapToDouble(t -> t.getProbability()).sum();
				if (!Precision.equals(tempSum, 1.0)) {
					throw new IllegalStateException("Probabilities do not sum up to one, but instead to " + tempSum);
				}
			}
		}
		return true;
	}

	protected void changeTransitionProbability(Transition transition, double newProbability) {
		changeTransitionProbability(transition, newProbability, true);
	}

	protected void changeTransitionProbability(Transition transition, double newProbability, boolean bindTimeInformation) {
		if (!transition.isStopTraversingTransition()) {
			Distribution d = null;
			d = removeTransition(transition, bindTimeInformation);
			final Transition t = new Transition(transition.getFromState(), transition.getToState(), transition.getSymbol(), newProbability);
			transitions.add(t);
			if (bindTimeInformation) {
				bindTransitionDistribution(t, d);
			}
			if (d == null && bindTimeInformation) {
				logger.warn("Should incorporate time but there was no time distribution associated with transition {}", t);
			}
		} else {
			final double adjusted = finalStateProbabilities.put(transition.getFromState(), newProbability);
			if (Double.doubleToLongBits(adjusted) == Double.doubleToLongBits(finalStateProbabilities.getNoEntryValue())) {
				logger.warn("Was not possible to adjust final state prob for transition {}",transition);
			}
		}
	}

	protected PDTTAold() {
	}

	public PDTTAold(Path trebaPath, TimedInput trainingSequences) throws IOException {
		final BufferedReader inputReader = Files.newBufferedReader(trebaPath, StandardCharsets.UTF_8);
		String line = "";
		// 172 172 3 0,013888888888888892
		// from state ; to state ; symbol ; probability
		while ((line = inputReader.readLine()) != null) {
			final String[] lineSplit = line.split(" ");
			if (lineSplit.length == 4) {
				final int fromState = Integer.parseInt(lineSplit[0]);
				final int toState = Integer.parseInt(lineSplit[1]);
				final String symbol;
				if (trainingSequences == null) {
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
	}

	public int getTransitionCount() {
		return transitions.size();
	}

	public Transition addTransition(int fromState, int toState, String symbol, double probability) {
		addState(fromState);
		addState(toState);
		final Transition t = new Transition(fromState, toState, symbol, probability);
		transitions.add(t);
		return t;
	}

	public Transition addAbnormalTransition(int fromState, int toState, String symbol, double probability, AnomalyInsertionType anomalyType) {
		addState(fromState);
		addState(toState);
		final Transition t = new AbnormalTransition(fromState, toState, symbol, probability, anomalyType);
		transitions.add(t);
		return t;
	}

	public Transition addAbnormalTransition(Transition t, AnomalyInsertionType anomalyType) {
		return addAbnormalTransition(t.getFromState(), t.getToState(), t.getSymbol(), t.getProbability(), anomalyType);
	}

	protected void addState(int state) {
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

	public void toGraphvizFile(Path graphvizResult, boolean compressed) throws IOException {
		final BufferedWriter writer = Files.newBufferedWriter(graphvizResult, StandardCharsets.UTF_8);
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
			writer.write(t.getSymbol());
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
		writer.close();

	}

	public void addFinalState(int state, double probability) {
		finalStateProbabilities.put(state, probability);
	}

	protected void addAbnormalFinalState(int state, double probability) {
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
				if (t.getFromState() == currentState && t.getSymbol() == event) {
					if (result != null) {
						logger.error("Found more than one transition for state " + currentState + " and event " + event);
					}
					result = t;
				}
			}
		}
		return result;
	}

	protected void bindTransitionDistribution(Transition newTransition, Distribution d) {
		if (transitionDistributions != null) {
			transitionDistributions.put(newTransition.toZeroProbTransition(), d);
		} else {
			logger.warn("Trying to add Distribution {} to non existing time transition distributions", d);
		}
	}

	/**
	 * CARE: The distribution to this transition is also removed.
	 * 
	 * @param t
	 */
	protected Distribution removeTransition(Transition t) {
		return removeTransition(t, true);
	}

	protected Distribution removeTransition(Transition t, boolean removeTimeDistribution) {
		final boolean wasRemoved = transitions.remove(t);
		if (!wasRemoved) {
			logger.warn("Tried to remove a non existing transition={}", t);
		}
		if (removeTimeDistribution) {
			if (transitionDistributions != null) {
				return transitionDistributions.remove(t.toZeroProbTransition());
			} else {
				logger.warn("Trying to remove from non existing transition distributions and transition {}", t);
				return null;
			}
		} else {
			return null;
		}
	}

	protected static final int MAX_SEQUENCE_LENGTH = 1000;

	public TimedWord sampleSequence() {
		int currentState = START_STATE;

		final List<String> eventList = new ArrayList<>();
		final TIntList timeList = new TIntArrayList();
		boolean choseFinalState = false;
		while (!choseFinalState) {
			final List<Transition> possibleTransitions = getTransitions(currentState, true);
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
			if (chosenTransition.isStopTraversingTransition()) {
				choseFinalState = true;
			} else if (eventList.size() > MAX_SEQUENCE_LENGTH) {
				throw new IllegalStateException("A sequence longer than " + MAX_SEQUENCE_LENGTH + " events should have been generated");
			} else {
				currentState = chosenTransition.getToState();
				final Distribution d = transitionDistributions.get(chosenTransition.toZeroProbTransition());
				if (d == null) {
					// maybe this happens because the automaton is more general than the data. So not every possible path in the automaton is represented in
					// the training data.
					throw new IllegalStateException("This should never happen for transition " + chosenTransition);
				}
				final int timeValue = (int) d.sample(1, r)[0];
				eventList.add(chosenTransition.getSymbol());
				timeList.add(timeValue);
			}
		}
		return new TimedWord(eventList, timeList, ClassLabel.NORMAL);
	}

	/**
	 * Returns all outgoing probabilities from the given state
	 * 
	 * @param currentState
	 *            the given state
	 * @param includeStoppingTransition
	 *            whether to include final transition probabilities
	 * @return
	 */
	protected List<Transition> getTransitions(int currentState, boolean includeStoppingTransition) {
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

	public Random getRandom() {
		return r;
	}

	public void setRandom(Random r) {
		this.r = r;
	}

	protected boolean isInAutomaton(TimedWord s) {
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

	public int getStateCount() {
		return finalStateProbabilities.size();
	}

	protected void removeState(int i) {
		finalStateProbabilities.remove(i);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abnormalFinalStates == null) ? 0 : abnormalFinalStates.hashCode());
		result = prime * result + ((finalStateProbabilities == null) ? 0 : finalStateProbabilities.hashCode());
		result = prime * result + ((transitionDistributions == null) ? 0 : transitionDistributions.hashCode());
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
		if (!(obj instanceof PDTTAold) && !(obj instanceof PDTTA)) {
			return false;
		}
		if (obj instanceof PDTTA) {
			final PDTTA other = (PDTTA) obj;
			if (abnormalFinalStates == null) {
				if (other.abnormalFinalStates != null) {
					return false;
				}
			} else if (!abnormalFinalStates.equals(other.abnormalFinalStates)) {
				return false;
			}
			if (finalStateProbabilities == null) {
				if (other.finalStateProbabilities != null) {
					return false;
				}
			} else if (!finalStateProbabilities.equals(other.finalStateProbabilities)) {
				return false;
			}
			if (transitionDistributions == null) {
				if (other.transitionDistributions != null) {
					return false;
				}
			} else if (!transitionDistributions.equals(other.transitionDistributions)) {
				final Set<Entry<ZeroProbTransition, Distribution>> e1 = transitionDistributions.entrySet();
				final Set<Entry<ZeroProbTransition, Distribution>> e2 = other.transitionDistributions.entrySet();

				int count = 0;
				for (final Entry<ZeroProbTransition, Distribution> e : e1) {
					if (!e2.contains(e)) {
						logger.error("Entry {} not contained in e2", e);
						final Distribution result = other.transitionDistributions.get(e.getKey());
						if (result != null) {
							final boolean compare = e.getValue().equals(result);
							logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
							logger.info("d1: {}, d2: {}", e.getValue(), result);
							e.getValue().equals(result);
						}
						count++;
					}
				}
				logger.error("");
				for (final Entry<ZeroProbTransition, Distribution> e : e2) {
					if (!e1.contains(e)) {
						logger.error("Entry {} not contained in e1", e);
						final Distribution result = transitionDistributions.get(e.getKey());
						if (result != null) {
							final boolean compare = e.getValue().equals(result);
							logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
							logger.info("d1: {}, d2: {}", e.getValue(), result);
						}
						count++;
					}
				}
				if (count > 0) {
					logger.error("{} out of {} entries did not match (other.size={})", count, transitionDistributions.size(),
							other.transitionDistributions.size());
				}
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
						logger.error("Transition {} not contained in other.transitions", t);
						count++;
					}
				}
				for (final Transition t : other.transitions) {
					if (!transitions.contains(t)) {
						logger.error("Transition {} not contained in transitions", t);
						count++;
					}
				}
				if (count > 0) {
					logger.error("{} out of {} transitions did not match", count, transitions.size());
				}
				return false;
			}
			return true;
		}
		final PDTTAold other = (PDTTAold) obj;
		if (abnormalFinalStates == null) {
			if (other.abnormalFinalStates != null) {
				return false;
			}
		} else if (!abnormalFinalStates.equals(other.abnormalFinalStates)) {
			return false;
		}
		if (finalStateProbabilities == null) {
			if (other.finalStateProbabilities != null) {
				return false;
			}
		} else if (!finalStateProbabilities.equals(other.finalStateProbabilities)) {
			return false;
		}
		if (transitionDistributions == null) {
			if (other.transitionDistributions != null) {
				return false;
			}
		} else if (!transitionDistributions.equals(other.transitionDistributions)) {
			final Set<Entry<ZeroProbTransition, Distribution>> e1 = transitionDistributions.entrySet();
			final Set<Entry<ZeroProbTransition, Distribution>> e2 = other.transitionDistributions.entrySet();
			int count = 0;
			for (final Entry<ZeroProbTransition, Distribution> e : e1) {
				if (!e2.contains(e)) {
					logger.error("Entry {} not contained in e2", e);
					final Distribution result = other.transitionDistributions.get(e.getKey());
					if (result != null) {
						final boolean compare = e.getValue().equals(result);
						logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
						logger.info("d1: {}, d2: {}", e.getValue(), result);
					}
					count++;
				}
			}
			logger.error("");
			for (final Entry<ZeroProbTransition, Distribution> e : e2) {
				if (!e1.contains(e)) {
					logger.error("Entry {} not contained in e1", e);
					final Distribution result = transitionDistributions.get(e.getKey());
					if (result != null) {
						final boolean compare = e.getValue().equals(result);
						logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
						logger.info("d1: {}, d2: {}", e.getValue(), result);
					}
					count++;
				}
			}
			if (count > 0) {
				logger.error("{} out of {} entries did not match", count, transitionDistributions.size());
			}
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
					logger.error("Transition {} not contained in other.transitions", t);
					count++;
				}
			}
			for (final Transition t : other.transitions) {
				if (!transitions.contains(t)) {
					logger.error("Transition {} not contained in transitions", t);
					count++;
				}
			}
			if (count > 0) {
				logger.error("{} out of {} transitions did not match", count, transitions.size());
			}
			return false;
		}
		return true;
	}

	protected TDoubleList computeTimeLikelihoods(TimedWord ts) {
		final TDoubleList list = new TDoubleArrayList();
		int currentState = 0;
		for (int i = 0; i < ts.length(); i++) {
			final Transition t = getTransition(currentState, ts.getSymbol(i));
			// DONE this is crap, isnt it? why not return an empty list or null iff there is no transition for the given sequence? or at least put a '0' in the
			// last slot.
			if (t == null) {
				list.add(0);
				return list;
			}
			final Distribution d = getTransitionDistributions().get(t.toZeroProbTransition());
			if (d == null) {
				// System.out.println("Found no time distribution for Transition "
				// + t);
				list.add(0);
			} else {
				list.add(d.pdf(ts.getTimeValue(i)));
			}
			currentState = t.getToState();
		}
		return list;
	}

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {
		return Pair.create(computeEventLikelihoods(s), computeTimeLikelihoods(s));
	}

	/**
	 * 
	 * @return the list up to the last probability that exists. list may be shorter than the events list, iff there is an event which has no transition
	 */
	protected TDoubleList computeEventLikelihoods(TimedWord s) {

		final TDoubleList list = new TDoubleArrayList();
		int currentState = 0;
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

}
