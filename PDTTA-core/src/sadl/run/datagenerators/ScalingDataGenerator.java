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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Uniform;
import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.models.PDTTA;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;
import sadl.tau_estimation.IdentityEstimator;
import sadl.utils.CollectionUtils;
import sadl.utils.IoUtils;

public class ScalingDataGenerator {
	private static Logger logger = LoggerFactory.getLogger(ScalingDataGenerator.class);

	public static final int INITIAL_ALPHABET_SIZE = 25;
	public static final int INITIAL_STATE_SIZE = 25;
	public static final int INITIAL_TRANSITION_SIZE = 156;
	public static final int INITIAL_SAMPLES = 1000;
	public static final int INITIAL_TIME_BASED_TRANSITION_SIZE = 0;

	public static final int MAX_EVENT_BASED_TRANSITION_SIZE = INITIAL_STATE_SIZE * INITIAL_STATE_SIZE;
	public static final int MAX_TIME_BASED_TRANSITION_SIZE = MAX_EVENT_BASED_TRANSITION_SIZE - INITIAL_TRANSITION_SIZE;
	public static final int MAX_ALPHABET_SIZE = INITIAL_TRANSITION_SIZE;
	public static final int MAX_STATE_SIZE = INITIAL_TRANSITION_SIZE - INITIAL_STATE_SIZE;
	public static final int MAX_SAMPLES = 20000;
	public static final int TIME_LOW = 1;
	public static final int TIME_HIGH = 1000;
	public static final int SCALING_STEPS = 10;
	final Random r = new Random(1234);

	public static void main(String[] args) throws IOException {
		final ScalingDataGenerator sdg = new ScalingDataGenerator();
		final Path dataOutputDir = Paths.get(args[0]);
		sdg.generateData(dataOutputDir);
		final Path confOutputDir = Paths.get(args[1]);
		sdg.createConfs(dataOutputDir, confOutputDir);
	}

	@SuppressWarnings("boxing")
	public void generateData(Path outputFolder) throws IOException {
		Files.createDirectories(outputFolder);
		final String[] alphabetStrings = new String[INITIAL_ALPHABET_SIZE];
		for (int i = 0; i < INITIAL_ALPHABET_SIZE; i++) {
			alphabetStrings[i] = Integer.toString(i);
		}
		final TimedInput alphabet = new TimedInput(alphabetStrings);

		final TIntDoubleHashMap states = new TIntDoubleHashMap();
		final TreeMap<Integer, Integer> stateTransitionCount = new TreeMap<>();
		states.put(0, 0.0);
		stateTransitionCount.put(0, 0);
		for (int i = 1; i < INITIAL_STATE_SIZE; i++) {
			states.put(i, 1);
			stateTransitionCount.put(i, 0);
		}
		final HashSet<Transition> transitions = new HashSet<>();
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
		structure.toGraphvizFile(outputFolder.resolve("pdfa.gv"), false);
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
		try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("initial-data.txt"))) {
			input.toFile(bw, true);
		}
		{
			final double scalingStepSize = (double) (MAX_SAMPLES - INITIAL_SAMPLES) / (SCALING_STEPS - 1);
			logger.info("Scaling step size for more data samples={}", scalingStepSize);
			for (int i = 1; i < SCALING_STEPS; i++) {
				for (int j = 0; j < scalingStepSize; j++) {
					initialWords.add(initialAutomaton.sampleSequence());
				}
				input = new TimedInput(initialWords);
				try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("data-inc-samples-" + i + ".txt"))) {
					input.toFile(bw, true);
				}
			}
		}
		{
			// add event based transitions
			final double eventStepSize = (double) (MAX_EVENT_BASED_TRANSITION_SIZE - INITIAL_TRANSITION_SIZE) / (SCALING_STEPS - 1);
			logger.info("Scaling step size for more event transitions={}", eventStepSize);
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
				try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("data-event-transitions-" + i + ".txt"))) {
					input.toFile(bw, true);
				}
			}
		}
		{
			// add time based transitions
			final double timeStepSize = (double) (MAX_TIME_BASED_TRANSITION_SIZE - INITIAL_TIME_BASED_TRANSITION_SIZE) / (SCALING_STEPS - 1);
			logger.info("Scaling step size for more time transitions={}", timeStepSize);
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
				try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("data-time-transitions-" + i + ".txt"))) {
					input.toFile(bw, true);
				}
			}
		}

		{
			// increase alphabet size
			final double alphabetStepSize = (double) (MAX_ALPHABET_SIZE - INITIAL_ALPHABET_SIZE) / (SCALING_STEPS - 1);
			logger.info("Scaling step size for more symbols={}", alphabetStepSize);
			for (int i = 1; i < SCALING_STEPS; i++) {
				final Set<Transition> alphabetTransitions = new HashSet<>();
				final int alphabetSize = (int) (INITIAL_ALPHABET_SIZE + (alphabetStepSize * i));
				final Map<Integer, Integer> eventOcc = new HashMap<>();
				final List<String> bigAlphabetStrings = new ArrayList<>();
				for (int j = 0; j < alphabetSize; j++) {
					bigAlphabetStrings.add(Integer.toString(j));
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
						logger.error("Symbol {} was never used", symbol);
					}
				}
				final PDTTA eventPdtta = new PDTTA(newStructure, distributions, new IdentityEstimator());
				initialWords.clear();
				for (int j = 0; j < INITIAL_SAMPLES; j++) {
					initialWords.add(eventPdtta.sampleSequence());
				}
				input = new TimedInput(initialWords);
				try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("data-alphabet-" + i + ".txt"))) {
					input.toFile(bw, true);
				}
			}
		}
		{
			// increase number of states
			// check whether every state is still reachable after adding a state (bending a transition to a new state)
			final double stateStepSize = (double) (MAX_STATE_SIZE - INITIAL_STATE_SIZE) / (SCALING_STEPS - 1);
			logger.info("Scaling step size for more states={}", stateStepSize);
			for (int i = 1; i < SCALING_STEPS; i++) {
				final TreeMap<Integer, Integer> transitionCount = (TreeMap<Integer, Integer>) stateTransitionCount.clone();
				final int elementsToAdd = (int) (stateStepSize * i);
				final HashSet<Transition> importantTransitions = new HashSet<>();
				final HashSet<Transition> stateTransitions = new HashSet<>();
				for (final Transition t : transitions) {
					stateTransitions.add(new Transition(t.getFromState(), t.getToState(), t.getSymbol(), 1.0));
				}
				final PDFA newStructure = new PDFA(alphabet, stateTransitions, SerializationUtils.clone(states));
				for (int k = 1; k < states.size(); k++) {
					final int outCount = newStructure.getOutTransitions(k, false).size();
					states.put(k, 0.05 * outCount);
				}
				for (int j = 0; j < elementsToAdd; j++) {

					final Entry<Integer, Integer> entry = transitionCount.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get();
					final Integer currentState = entry.getKey();
					final Integer currentCount = entry.getValue();
					if (currentCount == Integer.MIN_VALUE) {
						logger.error("Was not able to bend any transition for state {}", currentState);
						break;
					}
					final List<Transition> currentTransitions = newStructure.getOutTransitions(currentState, false);
					if (importantTransitions.containsAll(currentTransitions)) {
						transitionCount.put(currentState, Integer.MIN_VALUE);
						j--;
						continue;
					}
					Transition toRemove;
					do {
						toRemove = CollectionUtils.chooseRandomObject(currentTransitions, r);
					} while (importantTransitions.contains(toRemove));
					newStructure.removeTransition(toRemove);
					if (!newStructure.isConnected()) {
						newStructure.addTransition(toRemove.getFromState(), toRemove.getToState(), toRemove.getSymbol(), toRemove.getProbability());
						importantTransitions.add(toRemove);
						j--;
						continue;
					}
					transitionCount.put(currentState, currentCount - 1);
					final int newState = newStructure.getStateCount();
					newStructure.addFinalState(newState, 1.0);
					newStructure.addTransition(toRemove.getFromState(), newState, toRemove.getSymbol(), toRemove.getProbability());
					newStructure.checkAndRestoreConsistency();
				}
				final Map<ZeroProbTransition, ContinuousDistribution> distributions = new HashMap<>();
				for (final Transition t : newStructure.getTransitions()) {
					distributions.put(t.toZeroProbTransition(), new Uniform(TIME_LOW, TIME_HIGH));
				}
				final PDTTA statePdtta = new PDTTA(newStructure, distributions, new IdentityEstimator());
				logger.info("statePdtta has {} states", statePdtta.getStateCount());
				initialWords.clear();
				for (int j = 0; j < INITIAL_SAMPLES; j++) {
					initialWords.add(statePdtta.sampleSequence());
				}
				input = new TimedInput(initialWords);
				try (BufferedWriter bw = Files.newBufferedWriter(outputFolder.resolve("data-states-" + i + ".txt"))) {
					input.toFile(bw, true);
				}
			}
		}
	}

	final DecimalFormat df = new DecimalFormat("00");

	public void createConfs(Path inputDir, Path outputDir) throws IOException {
		try {
			Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (!attrs.isDirectory() && file.toString().endsWith(".txt")) {
						final TimedInput unsplitTrainingTimedSequences = TimedInput.parse(file);
						final String genFolder = "random";
						final String typeFolderString = "mixed";
						final Consumer<Path> anomalyGenerator = (Path dataOutputFile) -> {
							try {
								generateRandomAnomaly(unsplitTrainingTimedSequences, dataOutputFile, AnomalyInsertionType.ALL);
							} catch (final Exception e) {
								e.printStackTrace();
								logger.error("Unexpected exception", e);
							}
						};
						Temp.createFiles(outputDir, Paths.get(FilenameUtils.removeExtension(file.toString())), df, genFolder, typeFolderString,
								anomalyGenerator);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
					return super.preVisitDirectory(dir, attrs);
				}
			});
		} catch (final IOException e) {
			System.err.println("Unexpected exception occured." + e);
		}
	}

	int[] intIndex;

	public void generateRandomAnomaly(TimedInput trainingTimedSequences, Path dataOutputFile, AnomalyInsertionType type) throws IOException {
		if (type != AnomalyInsertionType.NONE) {
			logger.info("generating random anomalies for type {}...", type);
			final ArrayList<TimedWord> trainSequences = new ArrayList<>();
			final ArrayList<TimedWord> testSequences = new ArrayList<>();
			// final Path p = Paths.get("pta_normal.dot");
			// pta.toGraphvizFile(outputDir.resolve(p), false);
			// final Process ps = Runtime.getRuntime().exec("dot -Tpdf -O " + outputDir.resolve(p));
			// System.out.println(outputDir.resolve(p));
			// ps.waitFor();
			if (intIndex == null || intIndex.length != trainingTimedSequences.size()) {
				intIndex = new int[trainingTimedSequences.size()];
				for (int i = 0; i < trainingTimedSequences.size(); i++) {
					intIndex[i] = i;
				}
			}
			final TIntList shuffledIndex = new TIntArrayList(Arrays.copyOf(intIndex, intIndex.length));
			shuffledIndex.shuffle(r);
			final int split = (int) (trainingTimedSequences.size() * 0.9);
			for (int i = 0; i < split; i++) {
				trainSequences.add(trainingTimedSequences.get(shuffledIndex.get(i)));
			}
			for (int i = split; i < trainingTimedSequences.size(); i++) {
				testSequences.add(trainingTimedSequences.get(shuffledIndex.get(i)));
			}
			// do a deep clone, cloning also the words themselves
			final List<TimedWord> trainSequenceClone = SerializationUtils.clone(trainSequences);
			final List<TimedWord> testSequenceClone = SerializationUtils.clone(testSequences);
			final TimedInput trainSet = new TimedInput(trainSequenceClone);
			TimedInput testSet = new TimedInput(testSequenceClone);
			testSet = testSet.insertRandomAnomalies(type, 0.5);
			IoUtils.writeTrainTestFile(dataOutputFile, trainSet, testSet);
			logger.info("Done!");
		}
	}
}
