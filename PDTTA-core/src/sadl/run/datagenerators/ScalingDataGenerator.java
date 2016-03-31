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
package sadl.run.datagenerators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Uniform;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.models.PDTTA;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;
import sadl.tau_estimation.IdentityEstimator;
import sadl.utils.CollectionUtils;

public class ScalingDataGenerator {
	private static final int INITIAL_ALPHABET_SIZE = 50;
	private static final int INITIAL_STATE_SIZE = 50;
	private static final int INITIAL_TRANSITION_SIZE = 625;
	private static final int INITIAL_SAMPLES = 5000;
	private static final int INITIAL_TIME_BASED_TRANSITION_SIZE = 0;

	private static final int MAX_EVENT_BASED_TRANSITION_SIZE = 2500;
	private static final int MAX_TIME_BASED_TRANSITION_SIZE = MAX_EVENT_BASED_TRANSITION_SIZE - INITIAL_TRANSITION_SIZE;
	private static final int MAX_ALPHABET_SIZE = 625;
	private static final int MAX_STATE_SIZE = 575;
	private static final int MAX_SAMPLES = 25000;
	private static final int TIME_LOW = 1;
	private static final int TIME_HIGH = 1000;
	private static final int SCALING_STEPS = 10;

	public static void main(String[] args) throws IOException {
		final ScalingDataGenerator sdg = new ScalingDataGenerator();
		sdg.run();
	}

	@SuppressWarnings("boxing")
	public void run() throws IOException {
		final String[] alphabetStrings = new String[INITIAL_ALPHABET_SIZE];
		for (int i = 0; i < INITIAL_ALPHABET_SIZE; i++) {
			alphabetStrings[i] = Integer.toString(i);
		}
		final TimedInput alphabet = new TimedInput(alphabetStrings);

		final TIntDoubleMap states = new TIntDoubleHashMap();
		final TreeMap<Integer, Integer> stateTransitionCount = new TreeMap<>();
		states.put(0, 0.0);
		stateTransitionCount.put(0, 0);
		for (int i = 1; i < INITIAL_STATE_SIZE; i++) {
			states.put(i, 1);
			stateTransitionCount.put(i, 0);
		}
		final HashSet<Transition> transitions = new HashSet<>();
		final Random r = new Random(1234);
		for (int i = 0; i < INITIAL_TRANSITION_SIZE; i++) {
			final Integer currentState = stateTransitionCount.entrySet().stream().min((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
			final Integer currentValue = stateTransitionCount.get(currentState);
			Transition t = null;
			do {
				final int toState = r.nextInt(states.size());
				final int symbol = r.nextInt(alphabet.getAlphSize());
				t = new Transition(currentState, toState, Integer.toString(symbol), 1.0);
			} while (transitions.contains(t));
			stateTransitionCount.put(currentState, currentValue + 1);
			transitions.add(t);
		}

		final PDFA structure = new PDFA(alphabet, transitions, states);
		for (int i = 1; i < states.size(); i++) {
			final int outCount = structure.getOutTransitions(i, false).size();
			states.put(i, 0.05 * outCount);
		}
		states.put(0, 0.0);
		structure.checkAndRestoreConsistency();
		structure.toGraphvizFile(Paths.get("pdfa.gv"), false);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		for (final Transition t : transitions) {
			transitionDistributions.put(t.toZeroProbTransition(), new Uniform(TIME_LOW, TIME_HIGH));
		}
		final PDTTA initialAutomaton = new PDTTA(structure, transitionDistributions, new IdentityEstimator());
		// sample data from the initial automaton
		final List<TimedWord> initialWords = new ArrayList<>();
		for (int j = 0; j < INITIAL_SAMPLES; j++) {
			initialWords.add(initialAutomaton.sampleSequence());
		}
		TimedInput input = new TimedInput(initialWords);
		BufferedWriter bw = Files.newBufferedWriter(Paths.get("initial-data.txt"));
		input.toFile(bw, true);
		bw.close();
		{
			final double scalingStepSize = (double) (MAX_SAMPLES - INITIAL_SAMPLES) / (SCALING_STEPS - 1);
			for (int i = 1; i < SCALING_STEPS; i++) {
				for (int j = 0; j < scalingStepSize; j++) {
					initialWords.add(initialAutomaton.sampleSequence());
				}
				input = new TimedInput(initialWords);
				bw = Files.newBufferedWriter(Paths.get("data-inc-samples-" + i + ".txt"));
				input.toFile(bw, true);
				bw.close();
			}
		}
		{
			// add event based transitions
			final double eventStepSize = (double) (MAX_EVENT_BASED_TRANSITION_SIZE - INITIAL_TRANSITION_SIZE) / (SCALING_STEPS - 1);
			for (int i = 1; i < SCALING_STEPS; i++) {
				final Set<Transition> eventTransitions = new HashSet<>();
				for (final Transition t : transitions) {
					eventTransitions.add(new Transition(t.getFromState(), t.getToState(), t.getSymbol(), 1.0));
				}
				final TreeMap<Integer, Integer> transitionCount = (TreeMap<Integer, Integer>) stateTransitionCount.clone();
				final int elementsToAdd = (int) (eventStepSize * i);
				for (int j = 0; j < elementsToAdd; j++) {
					final Integer currentState = transitionCount.entrySet().stream().min((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
					final Integer currentValue = transitionCount.get(currentState);
					if (currentValue >= alphabet.getAlphSize()) {
						transitionCount.put(currentState, Integer.MAX_VALUE);
						j--;
						continue;
					}
					Transition t = null;
					do {
						final int toState = r.nextInt(states.size());
						final int symbol = r.nextInt(alphabet.getAlphSize());
						t = new Transition(currentState, toState, Integer.toString(symbol), 1.0);
					} while (eventTransitions.contains(t));
					transitionCount.put(currentState, currentValue + 1);
					eventTransitions.add(t);
				}
				final PDFA newStructure = new PDFA(alphabet, eventTransitions, states);
				for (int k = 1; k < states.size(); k++) {
					final int outCount = newStructure.getOutTransitions(k, false).size();
					states.put(k, 0.05 * outCount);
				}
				states.put(0, 0.0);
				newStructure.checkAndRestoreConsistency();
				final Map<ZeroProbTransition, ContinuousDistribution> distributions = new HashMap<>();
				for (final Transition t : eventTransitions) {
					distributions.put(t.toZeroProbTransition(), new Uniform(TIME_LOW, TIME_HIGH));
				}
				final PDTTA eventPdtta = new PDTTA(newStructure, distributions, new IdentityEstimator());
				initialWords.clear();
				for (int j = 0; j < INITIAL_SAMPLES; j++) {
					initialWords.add(eventPdtta.sampleSequence());
				}
				input = new TimedInput(initialWords);
				bw = Files.newBufferedWriter(Paths.get("data-event-transitions-" + i + ".txt"));
				input.toFile(bw, true);
				bw.close();
			}
		}
		{
			// add time based transitions
			final double timeStepSize = (double) (MAX_TIME_BASED_TRANSITION_SIZE - INITIAL_TIME_BASED_TRANSITION_SIZE) / (SCALING_STEPS - 1);

			for (int i = 1; i < SCALING_STEPS; i++) {
				final Map<ZeroProbTransition, ContinuousDistribution> distributions = new HashMap<>();
				final Set<Transition> timeTransitions = new HashSet<>();
				for (final Transition t : transitions) {
					timeTransitions.add(new Transition(t.getFromState(), t.getToState(), t.getSymbol(), 1.0));
					distributions.put(t.toZeroProbTransition(), new Uniform(TIME_LOW, TIME_HIGH));
				}
				final TreeMap<Integer, Integer> transitionCount = (TreeMap<Integer, Integer>) stateTransitionCount.clone();
				final int elementsToAdd = (int) (timeStepSize * i);
				for (int j = 0; j < elementsToAdd; j++) {
					final Integer currentState = transitionCount.entrySet().stream().min((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
					final Integer currentValue = transitionCount.get(currentState);
					final List<Transition> outTransitions = timeTransitions.stream().filter(t -> (t.getFromState() == currentState))
							.collect(Collectors.toList());
					final Transition t = CollectionUtils.chooseRandomObject(outTransitions, r);
					// split t in middle and add both new transitions
					timeTransitions.remove(t);
					final Transition leftHalf = new Transition(t.getFromState(), t.getToState(), t.getSymbol(), 1.0);
					final Transition rightHalf = new Transition(t.getFromState(), t.getToState(), t.getSymbol(), 1.0);
					final Uniform oldUniform = (Uniform) distributions.remove(t.toZeroProbTransition());
					final double mean = oldUniform.mean();
					final int middle = (int) mean;
					timeTransitions.add(leftHalf);
					timeTransitions.add(rightHalf);
					final double[] bounds = oldUniform.getCurrentVariableValues();
					distributions.put(leftHalf.toZeroProbTransition(), new Uniform(bounds[0], middle));
					distributions.put(rightHalf.toZeroProbTransition(), new Uniform(middle + 1, bounds[1]));
					transitionCount.put(currentState, currentValue + 1);

				}
				final PDFA newStructure = new PDFA(alphabet, timeTransitions, states);
				for (int k = 1; k < states.size(); k++) {
					final int outCount = newStructure.getOutTransitions(k, false).size();
					states.put(k, 0.05 * outCount);
				}
				states.put(0, 0.0);
				newStructure.checkAndRestoreConsistency();

				final PDTTA eventPdtta = new PDTTA(newStructure, distributions, new IdentityEstimator());
				initialWords.clear();
				for (int j = 0; j < INITIAL_SAMPLES; j++) {
					initialWords.add(eventPdtta.sampleSequence());
				}
				input = new TimedInput(initialWords);
				bw = Files.newBufferedWriter(Paths.get("data-time-transitions-" + i + ".txt"));
				input.toFile(bw, true);
				bw.close();
			}
		}

		{
			// increase alphabet size
			final double alphabetStepSize = (double) (MAX_ALPHABET_SIZE - INITIAL_ALPHABET_SIZE) / (SCALING_STEPS - 1);
			for (int i = 1; i < SCALING_STEPS; i++) {
				final Set<Transition> alphabetTransitions = new HashSet<>();
				final int alphabetSize = (int) (INITIAL_ALPHABET_SIZE + (alphabetStepSize * i));
				final Map<Integer, Integer> eventOcc = new HashMap<>();
				final List<String> bigAlphabetStrings = new ArrayList<>();
				for (int j = 0; j < alphabetSize; j++) {
					bigAlphabetStrings.add( Integer.toString(j));
					eventOcc.put(j, 0);
				}
				final TimedInput bigAlphabet = new TimedInput(alphabetStrings);
				boolean fillTransitions = true;
				for (final Transition t : transitions) {
					String symbol = CollectionUtils.chooseRandomObject(bigAlphabetStrings, r);
					if (fillTransitions) {
						final Entry<Integer, Integer> smallestOccurrence = eventOcc.entrySet().stream().min((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
								.get();
						if (smallestOccurrence.getValue() == 0) {
							symbol = smallestOccurrence.getKey().toString();
							eventOcc.put(smallestOccurrence.getKey(), 1);
						} else {
							fillTransitions = false;
						}
					}
					alphabetTransitions.add(new Transition(t.getFromState(), t.getToState(), symbol, 1.0));
				}

				final PDFA newStructure = new PDFA(bigAlphabet, alphabetTransitions, states);
				for (int k = 1; k < states.size(); k++) {
					final int outCount = newStructure.getOutTransitions(k, false).size();
					states.put(k, 0.05 * outCount);
				}
				states.put(0, 0.0);
				newStructure.checkAndRestoreConsistency();
				final Map<ZeroProbTransition, ContinuousDistribution> distributions = new HashMap<>();
				final TObjectIntMap<String> occ = new TObjectIntHashMap<>();
				for (final Transition t : alphabetTransitions) {
					distributions.put(t.toZeroProbTransition(), new Uniform(TIME_LOW, TIME_HIGH));
					occ.adjustOrPutValue(t.getSymbol(), 1, 1);
				}
				for (final String symbol : occ.keySet()) {
					if (occ.get(symbol) <= 0) {
						System.err.println(symbol);
					}
				}
				final PDTTA eventPdtta = new PDTTA(newStructure, distributions, new IdentityEstimator());
				initialWords.clear();
				for (int j = 0; j < INITIAL_SAMPLES; j++) {
					initialWords.add(eventPdtta.sampleSequence());
				}
				input = new TimedInput(initialWords);
				bw = Files.newBufferedWriter(Paths.get("data-alphabet-" + i + ".txt"));
				input.toFile(bw, true);
				bw.close();
			}
		}
		{
			// increase number of states
			// check whether every state is still reachable after adding a state (bending a transition to a new state)

		}
	}
}
