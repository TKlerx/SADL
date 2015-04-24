/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.upb.timok.models;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
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
import java.util.Random;
import java.util.Set;

import jsat.distributions.Distribution;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.timok.constants.AnomalyInsertionType;
import de.upb.timok.constants.ClassLabel;
import de.upb.timok.interfaces.AutomatonModel;
import de.upb.timok.structure.AbnormalTransition;
import de.upb.timok.structure.TimedSequence;
import de.upb.timok.structure.Transition;
import de.upb.timok.structure.ZeroProbTransition;
import de.upb.timok.utils.MasterSeed;

/**
 * A PDTTA with two thresholds for anomaly detection (aggregated event and aggregated time probability).
 * 
 * @author timok
 *
 */
public class PDTTA implements AutomatonModel, Serializable {



	protected static final int START_STATE = 0;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3017416753740710943L;

	// TODO implement PDTTA as an extension of a PDFA
	transient private static Logger logger = LoggerFactory.getLogger(PDTTA.class);
	transient private static final double ANOMALY_TYPE_TWO_P_1 = 0.7;
	transient private static final double ANOMALY_TYPE_TWO_P_2 = 0.9;

	protected static final double NO_TRANSITION_PROBABILITY = 0;

	private static final boolean DELETE_NO_TIME_INFORMATION_TRANSITIONS = true;
	TIntHashSet alphabet = new TIntHashSet();
	Set<Transition> transitions = new HashSet<>();
	TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
	Map<ZeroProbTransition, Distribution> transitionDistributions = null;

	public Map<ZeroProbTransition, Distribution> getTransitionDistributions() {
		return transitionDistributions;
	}

	public void setTransitionDistributions(Map<ZeroProbTransition, Distribution> transitionDistributions) {
		this.transitionDistributions = transitionDistributions;
		restoreConsistency();
	}

	protected void restoreConsistency() {
		if (!isConsistent()) {
			logger.warn("Model is not consistent; restoring consistency...");
			if (DELETE_NO_TIME_INFORMATION_TRANSITIONS) {
				deleteIrrelevantTransitions();
				fixProbabilities();
			}
		}
	}

	private boolean isConsistent() {
		if (transitions.size() != transitionDistributions.size()) {
			logger.warn("transitions and transitionDistributions must be of same size! {}!={}", transitions.size(), transitionDistributions.size());
			return false;
		}
		return finalStateProbabilities.keySet().forEach(state -> checkProbability(state));
	}

	private boolean checkProbability(int state) {
		final List<Transition> outgoingTransitions = getTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		final boolean compareResult = Precision.equals(sum, 1);
		logger.trace("Probability sum for state {}: {} (== 1? {})", state, sum, compareResult);
		return compareResult;
	}

	private void deleteIrrelevantTransitions() {
		logger.info("There are {} many transitions before removing irrelevant ones", transitions.size());
		// there may be more transitions than transitionDistributions
		transitions.removeIf(t -> !transitionDistributions.containsKey(t.toZeroProbTransition()));
		fixProbabilities();
		if (transitions.size() != transitionDistributions.size()) {
			logger.error("This should never happen because trainsitions.size() and transitionDistributions.size() should be equal now, but are not! {}!={}",
					transitions.size(), transitionDistributions.size());
		}
		logger.info("There are {} many transitions after removing irrelevant ones", transitions.size());
	}

	void fixProbabilities() {
		finalStateProbabilities.keySet().forEach(state -> fixProbability(state));

	}

	boolean fixProbability(int state) {
		// TODO use Fraction or BigFraction here!
		final List<Transition> outgoingTransitions = getTransitions(state, true);
		final double sum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
		logger.info("Sum of transition probabilities for state {} is {}", state, sum);
		// divide every probability by the sum of probabilities s.t. they sum up to 1
		if (!Precision.equals(sum, 1)) {
			outgoingTransitions.forEach(t -> t.setProbability(t.getProbability() / sum));
			final double newSum = outgoingTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
			logger.info("Corrected sum of transition probabilities is {}", newSum);
			if (!Precision.equals(newSum, 1.0)) {
				//TODO instead try first with apache commons math fraction/bigfraction and if they still do not sum up to one, throw an exception
				throw new IllegalStateException("Probabilities do not sum up to one, but instead to " + newSum);
			}
		}
		return true;
	}

	protected PDTTA() {
	}

	public PDTTA(Path trebaPath) throws IOException {
		final BufferedReader inputReader = Files.newBufferedReader(trebaPath, StandardCharsets.UTF_8);
		String line = "";
		// 172 172 3 0,013888888888888892
		// from state ; to state ; symbol ; probability
		while ((line = inputReader.readLine()) != null) {
			final String[] lineSplit = line.split(" ");
			if (lineSplit.length == 4) {
				final int fromState = Integer.parseInt(lineSplit[0]);
				final int toState = Integer.parseInt(lineSplit[1]);
				final int symbol = Integer.parseInt(lineSplit[2]);
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

	public Transition addTransition(int fromState, int toState, int symbol, double probability) {
		addState(fromState);
		addState(toState);
		alphabet.add(symbol);
		final Transition t = new Transition(fromState, toState, symbol, probability);
		transitions.add(t);
		return t;
	}

	public Transition addAbnormalTransition(int fromState, int toState, int symbol, double probability, AnomalyInsertionType anomalyType) {
		addState(fromState);
		addState(toState);
		alphabet.add(symbol);
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

	public void toGraphvizFile(Path graphvizResult, boolean compressed) throws IOException {
		// TODO make type 5 states red
		final BufferedWriter writer = Files.newBufferedWriter(graphvizResult, StandardCharsets.UTF_8);
		writer.write("digraph G {\n");
		// start states
		writer.write("qi [shape = point ];");
		// write states
		for (final int state : finalStateProbabilities.keys()) {
			writer.write(Integer.toString(state));
			writer.write(" [shape=");
			final double finalProb = finalStateProbabilities.get(state);
			if (finalProb > 0 || (compressed && finalProb > 0.01)) {
				writer.write("double");
			}
			writer.write("circle, label=\"");
			writer.write(Integer.toString(state));
			if (finalProb > 0 || (compressed && finalProb > 0.01)) {
				writer.write(" - p= ");
				writer.write(Double.toString(Precision.round(finalProb, 2)));
			}
			writer.write("\"];\n");
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
			writer.write(Integer.toString(t.getSymbol()));
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

	public double getFinalStateProbability(int state) {
		return finalStateProbabilities.get(state);
	}

	public double getTransitionProbability(int fromState, int toState, int symbol) {
		for (final Transition t : transitions) {
			if (t.getFromState() == fromState && t.getToState() == toState && t.getSymbol() == symbol) {
				return t.getProbability();
			}
		}
		return NO_TRANSITION_PROBABILITY;
	}

	public Transition getTransition(int currentState, int event) {
		Transition result = null;
		for (final Transition t : transitions) {
			if (t.getFromState() == currentState && t.getSymbol() == event) {
				if (result != null) {
					logger.error("Found more than one transition for state " + currentState + " and event " + event);
				}
				result = t;
			}
		}
		// if (result == null) {
		// System.err.println("Found no transition for state " + currentState + " and event " + event);
		// }
		return result;
	}

	protected void bindTransitionDistribution(Transition newTransition, Distribution d) {
		transitionDistributions.put(newTransition.toZeroProbTransition(), d);
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
			return transitionDistributions.remove(t.toZeroProbTransition());
		} else {
			return null;
		}
	}

	protected static final int MAX_SEQUENCE_LENGTH = 1000;

	public TimedSequence sampleSequence() {
		int currentState = START_STATE;

		final TIntList eventList = new TIntArrayList();
		final TDoubleList timeList = new TDoubleArrayList();
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
			if (chosenTransition.isStopTraversingTransition() ){
				choseFinalState = true;
			}else if (eventList.size() > MAX_SEQUENCE_LENGTH) {
				throw new IllegalStateException("A sequence longer than "+MAX_SEQUENCE_LENGTH+" events should have been generated");
			} else {
				currentState = chosenTransition.getToState();
				final Distribution d = transitionDistributions.get(chosenTransition.toZeroProbTransition());
				if (d == null) {
					// XXX maybe this happens because the automaton is more general than the data. So not every possible path in the automaton is represented in
					// the training data.
					throw new IllegalStateException("This should never happen for transition " + chosenTransition);
				}
				final double timeValue = d.sample(1, r)[0];
				eventList.add(chosenTransition.getSymbol());
				timeList.add(timeValue);
			}
		}
		return new TimedSequence(eventList, timeList, ClassLabel.NORMAL);
	}


	@Deprecated
	public TimedSequence createAbnormalEventSequence(Random mutation) {
		// choose very unlikely sequences
		final TIntList eventList = new TIntArrayList();
		final TDoubleList timeList = new TDoubleArrayList();
		boolean choseFinalState = false;
		int currentState = 0;
		while (!choseFinalState) {
			final List<Transition> possibleTransitions = getTransitions(currentState, true);
			possibleTransitions.sort((o1, o2) -> Double.compare(o1.getProbability(), o2.getProbability()));
			int listIndex = 3;
			if (possibleTransitions.size() <= listIndex) {
				listIndex = possibleTransitions.size() - 1;

			}
			int tempListIndex = Math.min(3, possibleTransitions.size() - 1);
			if (tempListIndex != listIndex) {
				throw new IllegalStateException();
			}
			final List<Transition> topThree = possibleTransitions.subList(0, listIndex);
			final double randomValue = mutation.nextDouble();
			int chosenTransitionIndex = -1;
			if (randomValue <= ANOMALY_TYPE_TWO_P_1) {
				chosenTransitionIndex = 0;
			} else if (randomValue > ANOMALY_TYPE_TWO_P_1 && randomValue < ANOMALY_TYPE_TWO_P_2) {
				chosenTransitionIndex = 1;
			} else {
				chosenTransitionIndex = 2;
			}
			int indexToTake = chosenTransitionIndex;
			if (indexToTake >= topThree.size()) {
				indexToTake = topThree.size() - 1;
			}
			tempListIndex = Math.min(chosenTransitionIndex, topThree.size() - 1);
			if (tempListIndex != indexToTake) {
				throw new IllegalStateException();
			}
			final Transition chosenTransition = topThree.get(indexToTake);
			if (chosenTransition.isStopTraversingTransition() || eventList.size() > MAX_SEQUENCE_LENGTH) {
				choseFinalState = true;
			} else {
				currentState = chosenTransition.getToState();
				final Distribution d = transitionDistributions.get(chosenTransition.toZeroProbTransition());
				if (d == null) {
					// just do it again with other random sampling
					return createAbnormalEventSequence(mutation);
				}
				final double timeValue = d.sample(1, mutation)[0];
				eventList.add(chosenTransition.getSymbol());
				timeList.add(timeValue);
			}
		}
		return new TimedSequence(eventList, timeList, ClassLabel.ANOMALY);
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
					result.add(new Transition(currentState, currentState, Transition.STOP_TRAVERSING_SYMBOL, finalStateProbabilities.get(state)));
				}
			}
		}
		return result;
	}

	Random r = new Random(MasterSeed.nextLong());



	public Random getRandom() {
		return r;
	}

	public void setRandom(Random r) {
		this.r = r;
	}

	protected boolean isInAutomaton(TimedSequence s) {
		int currentState = START_STATE;
		for (int i = 0; i < s.length(); i++) {
			final int nextEvent = s.getEvent(i);
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

	@Override
	public String toString() {
		return "PDTTA [alphabet=" + alphabet + "]";
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

}
