package sadl.run.datagenerators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private Path normalPDRTA;

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

	public static void main(String[] args) {

		final AlgoWeaknessesDataGenerator awdg = new AlgoWeaknessesDataGenerator();
		final JCommander jc = new JCommander(awdg);
		jc.parse(args);
		awdg.run();
	}

	@SuppressWarnings("null")
	private void run() {

		MasterSeed.setSeed(seed);
		rndm = MasterSeed.nextRandom();

		PDRTA nPDRTA = null;
		List<PDRTA> aPDRTAs = null;

		try {
			nPDRTA = PDRTA.parse(normalPDRTA.toFile());
			aPDRTAs = parse(anomalyPDRTAs);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final int numTrain = (int) Math.rint(setSize * (1.0 - testAmount));
		final int numTestNeg = (int) Math.rint((setSize - numTrain) * (1.0 - anomalyAmount));
		final int numTestPos = setSize - (numTrain + numTestNeg);

		for (int i = 0; i < aPDRTAs.size(); i++) {
			for (int j = 0; j < numSets; j++) {
				final TimedInput inpTrain = sample(nPDRTA, numTrain, ClassLabel.NORMAL);
				final TimedInput inpTestNeg = sample(nPDRTA, numTestNeg, ClassLabel.NORMAL);
				final TimedInput inpTestPos = sample(aPDRTAs.get(i), numTestPos, ClassLabel.ANOMALY);
				final Path outFile = outDir.resolve(anomalyPDRTAs.get(i).getFileName().toString().replaceAll("\\.[^\\.]+$", "") + "_" + j + ".txt");
				write(inpTrain, inpTestNeg, inpTestPos, outFile);
			}
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
			bw.append("\n");
			bw.append(SmacDataGenerator.TRAIN_TEST_SEP);
			bw.append("\n");
			inpTestNeg.toFile(bw, true);
			inpTestPos.toFile(bw, true);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private TimedInput sample(PDRTA a, int numSeq, ClassLabel label) {

		final List<TimedWord> seqs = new ArrayList<>(numSeq);
		for (int i = 0; i < numSeq; i++) {
			seqs.add(sampleSeq(a, label));
		}
		return new TimedInput(seqs);
	}

	private TimedWord sampleSeq(PDRTA a, ClassLabel label) {

		final List<String> symbols = new ArrayList<>();
		final TIntList delays = new TIntArrayList();
		PDRTAState s = a.getRoot();
		while (s != null) {
			final Pair<Integer, Integer> pair = samplePair(s);
			symbols.add(a.getSymbol(pair.getLeft()));
			delays.add(pair.getRight());
			s = s.getTarget(pair.getLeft(), pair.getRight());
			if (rndm.nextDouble() < s.getSequenceEndProb()) {
				s = null;
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

		Collections.sort(transitions, Collections.reverseOrder());
		final int idx = drawInstance(transitions.stream().map(a -> a.prob).collect(Collectors.toList()));
		final AlphIn trans = transitions.get(idx);
		return Pair.of(trans.symIdx, chooseUniform(trans.in.getBegin(), trans.in.getEnd()));
	}

	private int chooseUniform(int min, int max) {
		return min + rndm.nextInt((max - min) + 1);
	}

	private int drawInstance(List<Double> instances) {

		final double random = rndm.nextDouble();
		double summedProbs = 0;
		int index = -1;
		for (int i = 0; i < instances.size(); i++) {
			summedProbs += instances.get(i);
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

	private static class AlphIn implements Comparable<AlphIn> {
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
