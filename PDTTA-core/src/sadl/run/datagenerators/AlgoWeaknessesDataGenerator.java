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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;

import com.beust.jcommander.IVariableArity;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.utils.MasterSeed;

public class AlgoWeaknessesDataGenerator implements IVariableArity {

	private Random rndm;

	@Parameter(names = "-seed", description = "Seed for initializing the random generator", arity = 1, required = true)
	private long seed;

	@Parameter(names = "-normalPDRTA", description = "File containing the normal PDRTA", arity = 1, required = true)
	private Path normalPdrtaPath;

	@Parameter(names = "-anomalyPDRTAs", description = "Files containing the anomaly PDRTAs", variableArity = true, required = true)
	private List<Path> anomalyPDRTAs;

	@Parameter(names = "-outDir", description = "Directory to save the generated sets in", arity = 1, required = true)
	private Path outDir;

	@Parameter(names = "-numSets", description = "Number of sets to be generated per anomaly PDRTA", arity = 1, required = true)
	private int numSets;

	@Parameter(names = "-setSize", description = "Number of sequences per set", arity = 1, required = true)
	private int setSize;

	@Parameter(names = "-testAmount", description = "Relative amount of test sequences per set", arity = 1, required = true)
	private double testAmount;

	@Parameter(names = "-anomalyAmount", description = "Relative amount of anomaly sequences among the test sequences", arity = 1, required = true)
	private double anomalyAmount;

	public static void main(String[] args) throws URISyntaxException, IOException {
		final AlgoWeaknessesDataGenerator awdg = new AlgoWeaknessesDataGenerator();
		final JCommander jc = new JCommander(awdg);
		if (args.length == 0) {
			args = new String[] {
					("@" + Paths.get(AlgoWeaknessesDataGenerator.class.getResource("/pdrta/algo_weaknesses/algo_weaknesses_data_generator.args").toURI())) };
		}
		jc.parse(args);
		awdg.run();
	}

	@SuppressWarnings("null")
	private void run() throws IOException {
		MasterSeed.setSeed(seed);
		rndm = MasterSeed.nextRandom();

		PDRTA normalPDRTA = null;
		List<PDRTA> abnormalPDRTAs = null;

		try {
			normalPDRTA = PDRTA.parse(normalPdrtaPath.toFile());
			abnormalPDRTAs = parse(anomalyPDRTAs);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final int numTrain = (int) Math.rint(setSize * (1.0 - testAmount));
		final int numTestNormal = (int) Math.rint((setSize - numTrain) * (1.0 - anomalyAmount));
		final int numTestAnomalies = setSize - (numTrain + numTestNormal);

		{
			final Path p = Paths.get("fw-pdrta-normal.txt");
			final TimedInput inpTrain = sample(setSize, ClassLabel.NORMAL, normalPDRTA);
			try (BufferedWriter bw = Files.newBufferedWriter(p)) {
				inpTrain.toFile(bw, false);
			}
		}

		for (int i = 0; i < abnormalPDRTAs.size(); i++) {
			for (int j = 0; j < numSets; j++) {
				final TimedInput inpTrain = sample(numTrain, ClassLabel.NORMAL, normalPDRTA);
				final TimedInput inpTestNeg = sample(numTestNormal, ClassLabel.NORMAL, normalPDRTA);
				final TimedInput inpTestPos = sample(numTestAnomalies, ClassLabel.ANOMALY, abnormalPDRTAs.get(i));
				final Path outFile = outDir.resolve(anomalyPDRTAs.get(i).getFileName().toString().replaceAll("\\.[^\\.]+$", "") + "_" + j + ".txt");
				write(inpTrain, inpTestNeg, inpTestPos, outFile);
			}
		}
		for (int j = 0; j < numSets; j++) {
			final TimedInput inpTrain = sample(numTrain, ClassLabel.NORMAL, normalPDRTA);
			final TimedInput inpTestNeg = sample(numTestNormal, ClassLabel.NORMAL, normalPDRTA);
			final TimedInput inpTestPos = sample(numTestAnomalies, ClassLabel.ANOMALY, abnormalPDRTAs.toArray(new PDRTA[0]));
			final Path outFile = outDir.resolve("algo_weaknesses_pdrta_mixed_" + j + ".txt");
			write(inpTrain, inpTestNeg, inpTestPos, outFile);
		}

	}

	private List<PDRTA> parse(List<Path> pdrtaFiles) throws IOException {
		final List<PDRTA> pdrtas = new ArrayList<>(pdrtaFiles.size());
		for (final Path p : pdrtaFiles) {
			pdrtas.add(PDRTA.parse(p.toFile()));
		}
		return pdrtas;
	}

	private void write(TimedInput inpTrain, TimedInput inpTestNeg, TimedInput inpTestPos, Path outFile) {
		try {
			Files.createDirectories(outFile.getParent());
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try (BufferedWriter bw = Files.newBufferedWriter(outFile)) {
			inpTrain.toFile(bw, true);
			bw.append('\n');
			bw.append(Temp.TRAIN_TEST_SEP);
			bw.append('\n');
			inpTestNeg.toFile(bw, true);
			inpTestPos.toFile(bw, true);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private TimedInput sample(int numSeq, ClassLabel label, PDRTA... a) {
		final List<TimedWord> seqs = new ArrayList<>(numSeq);
		int roundRobin = 0;
		for (int i = 0; i < numSeq; i++) {
			seqs.add(sampleSeq(a[roundRobin], label));
			roundRobin %= a.length;
		}
		return new TimedInput(seqs);
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

	private int chooseUniform(int min, int max) {
		return min + rndm.nextInt((max - min) + 1);
	}

	private int drawInstance(List<Double> instances) {
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

	public static class AlphIn implements Comparable<AlphIn> {
		final int symIdx;
		final Interval in;
		final double prob;

		public AlphIn(int symIdx, Interval in, double prob) {
			this.symIdx = symIdx;
			this.in = in;
			this.prob = prob;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(prob);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			if (getClass() != obj.getClass()) {
				return false;
			}
			final AlphIn other = (AlphIn) obj;
			if (Double.doubleToLongBits(prob) != Double.doubleToLongBits(other.prob)) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(AlphIn o) {
			return Double.compare(prob, o.prob);
		}
	}

	@Override
	public int processVariableArity(String optionName, String[] options) {
		for (int i = 0; i < options.length; i++) {
			if (options[i].startsWith("-")) {
				return i;
			}
		}
		return options.length;
	}

}
