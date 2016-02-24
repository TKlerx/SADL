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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.GaussianMixture;
import jsat.distributions.Normal;
import jsat.distributions.Uniform;
import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.models.PDTTA;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.run.datagenerators.AlgoWeaknessesDataGenerator.AlphIn;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class AlgoWeakTwo {

	enum AnomalyType {
		ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7);
		private final int index;

		private AnomalyType(int index) {
			this.index = index;
		}
	}

	enum Determinism {
		TRUE(true), FALSE(false);

		private final boolean determinism;

		private Determinism(boolean determinism) {
			this.determinism = determinism;
		}
	}

	private static final int TRAIN_FILES = 10;
	private static final int TEST_FILES = 10;
	private static final int FILE_SIZE = 1000;
	private static final int ANOMALY_COUNT = 300;
	private static final double ANOMALY_RATE = 0.5;


	public static void main(String[] args) throws IOException {
		Path outputFolder = Paths.get("output3");
		IoUtils.cleanDir(outputFolder);

		final List<Function<Boolean, String>> pndttaFunctions = new ArrayList<>();

		final AlgoWeakTwo foo = new AlgoWeakTwo();
		pndttaFunctions.add(foo::sampleAbnormalPndttaA1);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA2);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA3);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA4);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA5);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA6);
		pndttaFunctions.add(foo::sampleAbnormalPndttaA7);

		Path p = Paths.get("tk-pdrta-normal.txt");
		Supplier<String> supp = foo::sampleNormalPdrtaSequence;
		writeNormalData(p, supp);

		p = Paths.get("tk-pntta-nonDet-normal.txt");
		supp = () -> foo.sampleNormalPndtta(false);
		writeNormalData(p, supp);

		p = Paths.get("tk-pdtta-det-normal.txt");
		supp = () -> foo.sampleNormalPndtta(true);
		writeNormalData(p, supp);
		final List<Supplier<String>> anomalyCreationFunctions = new ArrayList<>();
		outputFolder = outputFolder.resolve("smac-data").resolve("synthetic-direct");
		for (final AnomalyType type : AnomalyType.values()) {
			anomalyCreationFunctions.clear();
			anomalyCreationFunctions.add(() -> foo.sampleAbnormalPdrtaSequence(type));
			writeTrainTest(outputFolder.resolve("pdrta"), "A" + type.index, foo::sampleNormalPdrtaSequence, anomalyCreationFunctions);
			for (final Determinism det : Determinism.values()) {
				final String folderName = det.determinism ? "pDtta" : "pNtta";
				anomalyCreationFunctions.clear();
				anomalyCreationFunctions.add(() -> pndttaFunctions.get(type.index - 1).apply(det.determinism));
				writeTrainTest(outputFolder.resolve(folderName), "A" + type.index, () -> foo.sampleNormalPndtta(det.determinism), anomalyCreationFunctions);
			}
		}
		anomalyCreationFunctions.clear();
		Arrays.stream(AnomalyType.values()).forEach(type -> anomalyCreationFunctions.add(() -> foo.sampleAbnormalPdrtaSequence(type)));
		writeTrainTest(outputFolder.resolve("pdrta"), "A-mixed", foo::sampleNormalPdrtaSequence, anomalyCreationFunctions);
		for (final Determinism det : Determinism.values()) {
			anomalyCreationFunctions.clear();
			Arrays.stream(AnomalyType.values()).forEach(type -> anomalyCreationFunctions.add(() -> pndttaFunctions.get(type.index - 1).apply(det.determinism)));
			final String folderName = det.determinism ? "pDtta" : "pNtta";
			writeTrainTest(outputFolder.resolve(folderName), "A-mixed", () -> foo.sampleNormalPndtta(det.determinism), anomalyCreationFunctions);
		}
	}

	private static void writeTrainTest(Path outputFolder, String fileName, Supplier<String> supplier, List<Supplier<String>> anomalyCreationFunctions)
			throws IOException {
		final Path p = outputFolder.resolve(fileName);
		if (Files.notExists(p)) {
			Files.createDirectories(p);
		}
		final Path train = p.resolve("train");
		final Path test = p.resolve("test");
		Files.createDirectories(train);
		Files.createDirectories(test);

		// TODO write to train and test!
		final Random r = MasterSeed.nextRandom();
		for (int i = 0; i < TRAIN_FILES; i++) {
			writeFile(fileName + "-" + i + ".txt", supplier, anomalyCreationFunctions, train, r);
		}
		for (int i = TRAIN_FILES + 1; i < TRAIN_FILES + TEST_FILES; i++) {
			writeFile(fileName + "-" + i + ".txt", supplier, anomalyCreationFunctions, test, r);
		}

	}

	public static void writeFile(String fileName, Supplier<String> supplier, List<Supplier<String>> anomalyCreationFunctions, Path outputDir, final Random r)
			throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(outputDir.resolve(fileName))) {
			final int normalSequences = FILE_SIZE - ANOMALY_COUNT;
			int roundRobin = 0;
			for (int i = 0; i < normalSequences; i++) {
				bw.append(supplier.get());
				bw.append('\n');
			}
			bw.append(Temp.TRAIN_TEST_SEP);
			bw.append('\n');
			for (int i = 0; i < ANOMALY_COUNT; i++) {
				if (r.nextDouble() > ANOMALY_RATE) {
					bw.append(supplier.get());
				} else {
					bw.append(anomalyCreationFunctions.get(roundRobin).get());
					roundRobin++;
					roundRobin %= anomalyCreationFunctions.size();
				}
				bw.append('\n');
			}
		}
	}


	public static void writeNormalData(Path p, Supplier<String> supp) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(p)) {
			for (int i = 0; i < FILE_SIZE; i++) {
				bw.write(supp.get());
				bw.write('\n');
			}
		}
	}

	Map<String, PDRTA> pdrtas = new HashMap<>();

	public String sampleNormalPdrtaSequence() {
		final String key = "normal";
		PDRTA normalPDRTA = pdrtas.get(key);
		if (normalPDRTA == null) {
			try {
				normalPDRTA = PDRTA.parse(new File("resources/pdrta/algWeakTwo/normal.pdrta"));
				pdrtas.put(key, normalPDRTA);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String seq;
		final TimedWord sampleSequence = sampleSeq(normalPDRTA, ClassLabel.NORMAL);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPdrtaSequence(AnomalyType type) {
		final String key = "A" + type.index;
		PDRTA abnormalPDRTA = pdrtas.get(key);
		if (abnormalPDRTA == null) {
			try {
				abnormalPDRTA = PDRTA.parse(new File("resources/pdrta/algWeakTwo/abnormal" + type.index + ".pdrta"));
				pdrtas.put(key, abnormalPDRTA);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String seq;
		final TimedWord sampleSequence = sampleSeq(abnormalPDRTA, ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	private TimedWord sampleSeq(PDRTA a, ClassLabel label) {
		final List<String> symbols = new ArrayList<>();
		final TIntList delays = new TIntArrayList();
		PDRTAState s = a.getRoot();
		while (s != null) {
			final Pair<Integer, Integer> pair = samplePair(s);
			if (pair == null) {
				s = null;
			} else {
				symbols.add(a.getSymbol(pair.getLeft().intValue()));
				delays.add(pair.getRight().intValue());
				s = s.getTarget(pair.getLeft().intValue(), pair.getRight().intValue());
			}
		}

		return new TimedWord(symbols, delays, label);
	}

	private Pair<Integer, Integer> samplePair(PDRTAState s) {
		final List<AlphIn> transitions = new ArrayList<>();
		for (int i = 0; i < s.getPDRTA().getAlphSize(); i++) {
			final Collection<Interval> ins = s.getIntervals(i).values();
			for (final Interval in : ins) {
				final double transProb = s.getProbabilityTrans(i, in);
				if (transProb > 0.0) {
					transitions.add(new AlphIn(i, in, transProb));
				}
			}
		}
		transitions.add(new AlphIn(-1, null, s.getSequenceEndProb()));

		Collections.sort(transitions, Collections.reverseOrder());
		final int idx = drawInstance(transitions.stream().map(a -> new Double(a.prob)).collect(Collectors.toList()));
		final AlphIn trans = transitions.get(idx);
		if (trans.symIdx == -1) {
			return null;
		}
		return Pair.of(new Integer(trans.symIdx), new Integer(chooseUniform(trans.in.getBegin(), trans.in.getEnd())));
	}

	private Random rndm;

	private int chooseUniform(int min, int max) {
		if (rndm == null) {
			rndm = MasterSeed.nextRandom();
		}
		return min + rndm.nextInt((max - min) + 1);
	}

	private int drawInstance(List<Double> instances) {
		if (rndm == null) {
			rndm = MasterSeed.nextRandom();
		}
		final double random = rndm.nextDouble();
		double summedProbs = 0;
		int index = -1;
		for (int i = 0; i < instances.size(); i++) {
			summedProbs += instances.get(i).doubleValue();
			if (random < summedProbs) {
				index = i;
				break;
			}
		}
		if (index == -1 && !Precision.equals(summedProbs, 1.0)) {
			throw new IllegalStateException("Probabilities do not sum upt to 1.0! --> " + summedProbs);
		}
		if (index == -1) {
			throw new IllegalStateException("No instance could be drawn!");
		}
		return index;
	}


	Map<String, PDTTA> automata = new HashMap<>();

	public String sampleAbnormalPndttaA1(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A1Det";
		} else {
			key = "A1NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "d", "c", "a" };
			} else {
				symbols = new String[] { "b", "d", "c", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}

		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA2(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A2Det";
		} else {
			key = "A2NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a2", "a1" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			String t1Symbol;
			if (deterministicAutomaton) {
				t1Symbol = "b";
			} else {
				t1Symbol = "a2";
			}
			final Transition t1 = new Transition(0, 1, t1Symbol, 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = "a";
			} else {
				t2Symbol = "a1";
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA3(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A3Det";
		} else {
			key = "A3NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t2.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t1.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA4(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A4Det";
		} else {
			key = "A4NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA5(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A5Det";
		} else {
			key = "A5NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.25);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.25);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 1);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(0, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA6(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A6Det";
		} else {
			key = "A6NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(2, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(1, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleAbnormalPndttaA7(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "A7Det";
		} else {
			key = "A7NonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 2, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 1, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

	public String sampleNormalPndtta(boolean deterministicAutomaton) {
		String key = null;
		if (deterministicAutomaton) {
			key = "NormalDet";
		} else {
			key = "NormalNonDet";
		}
		PDTTA automaton = automata.get(key);
		if (automaton == null) {
			final String[] symbols;
			if (deterministicAutomaton) {
				symbols = new String[] { "b", "c", "d", "a" };
			} else {
				symbols = new String[] { "b", "c", "d", "a1", "a2" };
			}
			final TimedInput alphabet = new TimedInput(symbols);
			final Set<Transition> transitions = new HashSet<>();
			final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
			transitions.add(t1);
			String t2Symbol;
			if (deterministicAutomaton) {
				t2Symbol = symbols[0];
			} else {
				t2Symbol = symbols[4];
			}
			final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
			transitions.add(t2);
			final Transition t3 = new Transition(1, 3, symbols[1], 1);
			transitions.add(t3);
			final Transition t4 = new Transition(2, 3, symbols[2], 1);
			transitions.add(t4);
			final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
			transitions.add(t5);
			final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
			finalStateProbabilities.put(3, 0.5);
			final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
			transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
			transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
			transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
			transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
			transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
			automaton = new PDTTA(normalPdfa, transitionDistributions, null);
			automata.put(key, automaton);
		}
		String seq;
		final TimedWord sampleSequence = automaton.sampleSequence();
		seq = sampleSequence.toString();
		seq = seq.replaceAll("a\\d", "a");
		return seq;
	}

}
