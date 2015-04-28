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
package de.upb.timok.run;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jsat.distributions.Distribution;
import jsat.distributions.DistributionSearch;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import treba.observations;
import treba.treba;
import treba.trebaConstants;
import treba.wfsa;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import de.upb.timok.constants.AnomalyInsertionType;
import de.upb.timok.constants.ClassLabel;
import de.upb.timok.constants.MergeTest;
import de.upb.timok.constants.ProbabilityAggregationMethod;
import de.upb.timok.detectors.threshold.PdttaAggregatedThresholdDetector;
import de.upb.timok.experiments.PdttaExperimentResult;
import de.upb.timok.models.PDTTA;
import de.upb.timok.structure.TimedSequence;
import de.upb.timok.structure.ZeroProbTransition;

@Deprecated
public class SmacPipeline implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(SmacPipeline.class);

	public static final String TIME_THRESHOLD = "timeThreshold";
	public static final String EVENT_THRESHOLD = "eventThreshold";
	public static final String ANOMALY_INSERTION_TYPE = "anomalyInsertionType";
	public static final String AGGREGATION_TYPE = "aggType";
	public static final String TEMP_FILE_PREFIX = "tempFilePrefix";
	public static final String TIMED_INPUT_FILE = "timedInputFile";
	public static final String JOB_NAME = "jobName";
	public static final String MERGE_ALPHA = "mergeAlpha";
	public static final String RECURSIVE_MERGE_TEST = "recursiveMergeTest";
	public static final String MERGE_TEST = "mergeTest";
	public static final String RESULT_FOLDER = "results";
	/**
	 * 
	 */
	private static final long serialVersionUID = -6230657726489919272L;
	private static final double TEST_PERCENTAGE = 0.3;
	private static final double TRAIN_PERCENTAGE = 0.7;
	private static final double ANOMALY_PERCENTAGE = 0.1;
	private static final double HUGE_TIME_CHANGE = 0.9;
	private static final double SMALL_TIME_CHANGE = 0.1;
	private Random masterSeed = null;

	@Parameter()
	private final List<String> rest = new ArrayList<>();
	@Parameter(names="-1")
	private Boolean bla;

	@Parameter(names = "-mergeTest")
	String mergeTestInput = "ALERGIA";

	@Parameter(names =  "-mergeAlpha")
	private double mergeAlpha;

	@Parameter(names = "-recursiveMergeTest" )
	private boolean recursiveMergeTest;
	@Parameter(names = "-timeThreshold" )
	private double timeThreshold;
	@Parameter(names =  "-eventThreshold" )
	private double eventThreshold;

	String dataString;

	@Parameter(names =  "-probabilityAggregationMethod")
	String aggTypeInput="NORMALIZED_MULTIPLY";
	@Parameter(names =  "-anomalyInsertionType" )
	String anomalyInsertionTypeInput="ALL";

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
	InterruptedException {
		final SmacPipeline sp = new SmacPipeline();
		new JCommander(sp, args);
		System.out.println(Arrays.toString(args));
		sp.dataString = args[0];
		sp.run();

		// p.run("run_1_", MergeTest.ALERGIA, false, 0.05, "rti_input.txt",
		// "treba_temp_", ProbabilityAggregationMethod.NORMALIZED_MULTIPLY,
		// AnomalyInsertionType.TYPE_TWO, 0.2, 0.0001);
		System.exit(0);
	}

	private void run( ) throws IOException, InterruptedException {

		// get the property value and print it out
		final MergeTest mergeTest = MergeTest.valueOf(mergeTestInput);
		final ProbabilityAggregationMethod aggType = ProbabilityAggregationMethod
				.valueOf(aggTypeInput);
		final AnomalyInsertionType anomalyInsertionType = AnomalyInsertionType
				.valueOf(anomalyInsertionTypeInput);
		if (timeThreshold < 0) {
			timeThreshold = Double.NaN;
		}
		run(mergeTest, aggType, anomalyInsertionType);

	}

	// private static double[] parseDoubleArray(String property) {
	// String[] split = property.split(",");
	// double[] result = new double[split.length];
	// for (int i = 0; i < split.length; i++) {
	// String s = split[i];
	// result[i] = Double.parseDouble(s.replaceAll("\\[|\\]", ""));
	// }
	// return result;
	// }

	public void run(MergeTest mergeTest, ProbabilityAggregationMethod aggType,
			AnomalyInsertionType anomalyInsertionType) throws IOException,
			InterruptedException {
		masterSeed = new Random(serialVersionUID);

		final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
		final long jobNumber = Double.doubleToLongBits(Math.random());

		String jobName = Long.toString(jobNumber);
		jobName = jobName.substring(0, 5);
		// create treba input file
		final String tempFilePrefix = tempDir.toString() + File.separator
				+ jobName + getClass().getName();

		treba.log1plus_init_wrapper();
		final String timedInputTrainFile = tempFilePrefix + "trainFile";
		final String timedInputTestFile =  tempFilePrefix + "testFile";
		splitTrainTestFile(dataString, timedInputTrainFile, timedInputTestFile,
				TRAIN_PERCENTAGE, TEST_PERCENTAGE, anomalyInsertionType,
				ANOMALY_PERCENTAGE, false);
		// parse timed sequences
		final List<TimedSequence> trainingTimedSequences = parseTimedSequences(
				timedInputTrainFile, false, false);
		// create treba input file
		final String trebaTrainSetFileString = tempFilePrefix + "train_set";
		createTrebaFile(trainingTimedSequences, trebaTrainSetFileString);
		// train the fsm and write the fsm to 'trebaAutomatonFile'
		final String trebaAutomatonFile =  tempFilePrefix + "fsm.fsm";
		@SuppressWarnings("unused")
		final
		double loglikelihood = trainFsm(trebaTrainSetFileString, mergeAlpha,
				mergeTest, recursiveMergeTest, trebaAutomatonFile);
		// compute paths through the automata for the training set and write to
		// 'trebaResultPathFile'
		final String trebaResultPathFile = tempFilePrefix
				+ "train_likelihood";
		computeAutomatonPaths(trebaAutomatonFile, trebaTrainSetFileString,
				trebaResultPathFile);
		// parse the 'trebaResultPathFile'
		// Fill time interval buckets and fit PDFs for every bucket
		final Map<ZeroProbTransition, TDoubleList> timeValueBuckets = parseAutomatonPaths(
				trebaResultPathFile, trainingTimedSequences);
		// do the fitting
		final Map<ZeroProbTransition, Distribution> transitionDistributions = fit(timeValueBuckets);
		// compute likelihood on test set for automaton and for time PDFs
		final PDTTA automaton = new PDTTA(Paths.get(trebaAutomatonFile));
		automaton.setTransitionDistributions(transitionDistributions);
		final List<TimedSequence> testTimedSequences = parseTimedSequences(
				timedInputTestFile, false, true);
		if (anomalyInsertionType == AnomalyInsertionType.TYPE_TWO
				|| anomalyInsertionType == AnomalyInsertionType.ALL) {
			for (int i = 0; i < testTimedSequences.size(); i++) {
				final TimedSequence ts = testTimedSequences.get(i);
				if (ts.getLabel() == ClassLabel.ANOMALY
						&& ts.getAnomalyType() == AnomalyInsertionType.TYPE_TWO) {
					testTimedSequences.set(i, automaton
							.createAbnormalEventSequence(new Random(masterSeed
									.nextLong())));
				}
			}
		}
		final String trebaTestSetFileString = tempFilePrefix + "test_set";
		final String trebaTestResultPathFile = tempFilePrefix
				+ "test_likelihood";
		createTrebaFile(testTimedSequences, trebaTestSetFileString);
		final PdttaExperimentResult result = testPdtta(testTimedSequences, automaton, aggType);
		//		System.out.println("Quality: " + (1 - result.getfMeasure()));
		System.out.println("Result for SMAC: SUCCESS, 0, 0, "+(1 - result.getfMeasure())+", 0");

		delFiles(new String[] { timedInputTrainFile, timedInputTestFile,
				trebaTrainSetFileString, trebaAutomatonFile,
				trebaResultPathFile, trebaTestSetFileString,
				trebaTestResultPathFile });
		treba.log1plus_free_wrapper();
	}

	private void delFiles(String[] strings) throws IOException {
		for (final String fileName : strings) {
			final Path p = Paths.get(fileName);
			Files.deleteIfExists(p);
		}

	}

	private void splitTrainTestFile(String timedInputFile,
			String timedInputTrainFile, String timedInputTestFile,
			double trainPercentage, double testPercentage,
			AnomalyInsertionType anomalyInsertionType,
			double anomalyPercentage, boolean isRti) throws IOException {
		System.out.println("TimedInputFile="+timedInputFile);
		final File f = new File(timedInputFile);
		System.out.println(f);
		final LineNumberReader lnr = new LineNumberReader(new FileReader(
				timedInputFile));
		lnr.skip(Long.MAX_VALUE);
		int samples = lnr.getLineNumber();
		lnr.close();
		final int trainingSamples = (int) (samples * trainPercentage);
		final int testSamples = (int) (samples * testPercentage);
		final int anomalies = (int) (anomalyPercentage * testSamples);
		final int writtenTrainingSamples = 0;
		final int writtenTestSamples = 0;
		int insertedAnomalies = 0;
		final BufferedReader br = Files.newBufferedReader(Paths.get(timedInputFile),
				StandardCharsets.UTF_8);
		String line = null;
		final BufferedWriter trainWriter = Files.newBufferedWriter(
				Paths.get(timedInputTrainFile), StandardCharsets.UTF_8);
		final BufferedWriter testWriter = Files.newBufferedWriter(
				Paths.get(timedInputTestFile), StandardCharsets.UTF_8);
		final Random r = new Random(masterSeed.nextLong());
		final Random mutation = new Random(masterSeed.nextLong());
		boolean force = false;
		int lineIndex = 0;
		int linesLeft;
		int anomaliesToInsert;
		if (isRti) {
			br.readLine();
			samples--;
		}
		while ((line = br.readLine()) != null) {
			if (writtenTrainingSamples < trainingSamples
					&& writtenTestSamples < testSamples) {
				// choose randomly according to train/test percentage
				if (r.nextDouble() > testPercentage) {
					// write to train
					writeSample(
							new TimedSequence(line, true, false)
							.toTrebaString(),
							trainWriter);
				} else {
					// write to test
					insertedAnomalies = testAndWriteAnomaly(
							anomalyInsertionType, anomalies, insertedAnomalies,
							anomalyPercentage, line, testWriter, mutation,
							force);
				}
			} else if (writtenTrainingSamples >= trainingSamples) {
				insertedAnomalies = testAndWriteAnomaly(anomalyInsertionType,
						anomalies, insertedAnomalies, anomalyPercentage, line,
						testWriter, mutation, force);
			} else if (writtenTestSamples >= testSamples) {
				// only write trainSamples from now on
				writeSample(
						new TimedSequence(line, true, false).toTrebaString(),
						trainWriter);
			}
			lineIndex++;
			linesLeft = samples - lineIndex;
			anomaliesToInsert = anomalies - insertedAnomalies;
			if (linesLeft <= anomaliesToInsert) {
				force = true;
			}
		}
		br.close();
		trainWriter.close();
		testWriter.close();
	}

	private int testAndWriteAnomaly(AnomalyInsertionType anomalyInsertionType,
			int anomalies, int insertedAnomalies, double anomalyPercentage,
			String line, BufferedWriter testWriter, Random mutation,
			boolean force) throws IOException {
		final String newLine = line;
		TimedSequence ts = new TimedSequence(newLine, true, false);
		int insertionCount = 0;
		if (insertedAnomalies < anomalies
				&& (force || mutation.nextDouble() < anomalyPercentage)) {
			ts = mutate(ts, anomalyInsertionType, mutation);
			insertionCount++;
		} else {
			ts.setLabel(ClassLabel.NORMAL);
			ts.setAnomalyType(AnomalyInsertionType.NONE);
		}
		writeSample(ts.toLabeledString(), testWriter);
		return insertedAnomalies + insertionCount;
	}

	int lastInsertionType = -1;

	private TimedSequence mutate(TimedSequence ts,
			AnomalyInsertionType anomalyInsertionType, Random mutation)
					throws IOException {
		if (anomalyInsertionType == AnomalyInsertionType.TYPE_ONE) {
			final int toDelete = mutation.nextInt(ts.getEvents().size());
			ts.remove(toDelete);
			ts.setLabel(ClassLabel.ANOMALY);
			ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_TWO) {
			// for this type of anomaly we need the fsm with distributions and
			// thus, we have to read the testset after the fsm is trained and
			// create the anomalies at that point
			ts.setLabel(ClassLabel.ANOMALY);
			ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_THREE) {
			final int changeIndex = mutation.nextInt(ts.getTimeValues().size());
			final TDoubleList timeValues = ts.getTimeValues();
			final double changePercent = HUGE_TIME_CHANGE;
			changeTimeValue(mutation, timeValues, changePercent, changeIndex);
			ts.setLabel(ClassLabel.ANOMALY);
			ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_FOUR) {
			final TDoubleList timeValues = ts.getTimeValues();
			final double changePercent = SMALL_TIME_CHANGE;
			for (int i = 0; i < timeValues.size(); i++) {
				changeTimeValue(mutation, timeValues, changePercent, i);
			}
			ts.setAnomalyType(anomalyInsertionType);
			ts.setLabel(ClassLabel.ANOMALY);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.ALL) {
			lastInsertionType = (lastInsertionType + 1) % 4;
			if (lastInsertionType == 0) {
				return mutate(ts, AnomalyInsertionType.TYPE_ONE, mutation);
			} else if (lastInsertionType == 1) {
				return mutate(ts, AnomalyInsertionType.TYPE_TWO, mutation);
			} else if (lastInsertionType == 2) {
				return mutate(ts, AnomalyInsertionType.TYPE_THREE, mutation);
			} else if (lastInsertionType == 3) {
				return mutate(ts, AnomalyInsertionType.TYPE_FOUR, mutation);
			}
		}
		return null;
	}

	private void changeTimeValue(Random mutation, TDoubleList timeValues,
			double changePercent, int i) {
		double newValue = timeValues.get(i);
		if (mutation.nextBoolean()) {
			newValue = newValue + newValue * changePercent;
		} else {
			newValue = newValue - newValue * changePercent;
		}
		timeValues.set(i, newValue);
	}

	private void writeSample(String line, BufferedWriter writer)
			throws IOException {
		writer.write(line);
		writer.append('\n');
	}

	private PdttaExperimentResult testPdtta(
			List<TimedSequence> testTimedSequences, PDTTA automaton,
			ProbabilityAggregationMethod aggType) throws IOException {

		// this is not needed because treba and java likelihoods are the same
		// load treba automaton file
		// load treba automaton file
		// wfsa fsm = treba.wfsa_read_file(trebaAutomatonFile);
		// observations o = treba.observations_read(trebaTestSetFileString);
		// // compute ll via treba for loaded fsm
		// int obs_alphabet_size = treba.observations_alphabet_size(o);
		// if (o != null && fsm != null
		// && fsm.getAlphabet_size() < obs_alphabet_size) {
		// System.err
		// .printf("Error: the observations file has symbols outside the FSA alphabet.\n");
		// System.exit(1);
		// }
		// if (0 != trebaConstants.FORMAT_LOG2) {
		// treba.wfsa_to_log2(fsm);
		// }
		// treba.forward_fsm_to_file(fsm, o, trebaConstants.DECODE_FORWARD_PROB,
		// trebaTestResultPathFile);
		// if (o != null) {
		// treba.observations_destroy(o);
		// }
		// if (fsm != null) {
		// treba.wfsa_destroy(fsm);
		// }

		final PdttaAggregatedThresholdDetector tester = new PdttaAggregatedThresholdDetector(aggType, -1, -1);
		tester.setModel(automaton);
		// parse treba computed likelihoods
		// TDoubleList trebaLikelihoods =
		// parseLikelihoods(trebaTestResultPathFile);
		// compute ll via automaton traverse
		// TDoubleList javaLikelihoods =
		// automaton.computeEventsLikelihood(testTimedSequences);
		// java and treba likelihoods should be equal (and it seems as they are)
		// compute time probabilty for every transition taken

		final List<double[]> testResult = tester.computeAggregatedLikelihoods(testTimedSequences);
		// compare with treshold and compute TP, TN, FP, FN
		final TObjectIntMap<DoublePair> tps = new TObjectIntHashMap<>();
		final TObjectIntMap<DoublePair> tns = new TObjectIntHashMap<>();
		final TObjectIntMap<DoublePair> fps = new TObjectIntHashMap<>();
		final TObjectIntMap<DoublePair> fns = new TObjectIntHashMap<>();
		// prec = tp/(tp +fp)
		// The precision is the ratio between correctly detected anomalies and
		// all detected anomalies
		// rec = tp/(tp+fn)
		// The recall is the ratio between detected anomalies and all anomalies
		for (int i = 0; i < testResult.size(); i++) {
			final double[] pair = testResult.get(i);
			final DoublePair dp = new DoublePair(eventThreshold, timeThreshold);
			double normalizedEventThreshold = eventThreshold;
			double normalizedTimeThreshold = timeThreshold;
			if (aggType == ProbabilityAggregationMethod.NORMALIZED_MULTIPLY) {
				normalizedEventThreshold = Math.log(eventThreshold)
						* testTimedSequences.get(i).getEvents().size();
				normalizedTimeThreshold = Math.log(timeThreshold)
						* testTimedSequences.get(i).getEvents().size();
			}
			boolean classifierSaysAnomaly = false;
			if (Double.isNaN(timeThreshold)) {
				if (pair[0] <= normalizedEventThreshold) {
					classifierSaysAnomaly = true;
				}
			} else {
				if (pair[0] <= normalizedEventThreshold
						|| pair[1] <= normalizedTimeThreshold) {
					classifierSaysAnomaly = true;
				}
			}
			final TimedSequence testSequence = testTimedSequences.get(i);
			if (testSequence.getLabel() == ClassLabel.NORMAL) {
				if (!classifierSaysAnomaly) {
					tns.adjustOrPutValue(dp, 1, 1);
				} else {
					fps.adjustOrPutValue(dp, 1, 1);
				}
			}
			if (testSequence.getLabel() != ClassLabel.NORMAL) {
				if (classifierSaysAnomaly) {
					tps.adjustOrPutValue(dp, 1, 1);
				} else {
					fns.adjustOrPutValue(dp, 1, 1);
				}
			}
			// System.out.println("Event likelihood=" + pair[0] +
			// "; time likelihood=" + pair[1]);
		}
		final DoublePair dp = new DoublePair(eventThreshold, timeThreshold);
		final PdttaExperimentResult expResult = new PdttaExperimentResult(
				tps.get(dp), tns.get(dp), fps.get(dp), fns.get(dp));
		expResult.setEventThreshold(eventThreshold);
		expResult.setTimeThreshold(timeThreshold);

		return expResult;

	}

	class DoublePair {
		double d1;
		double d2;

		public DoublePair(double d1, double d2) {
			super();
			this.d1 = d1;
			this.d2 = d2;
		}

		public double getD1() {
			return d1;
		}

		public double getD2() {
			return d2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(d1);
			result = prime * result + (int) (temp ^ temp >>> 32);
			temp = Double.doubleToLongBits(d2);
			result = prime * result + (int) (temp ^ temp >>> 32);
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
			final DoublePair other = (DoublePair) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (Double.doubleToLongBits(d1) != Double
					.doubleToLongBits(other.d1)) {
				return false;
			}
			if (Double.doubleToLongBits(d2) != Double
					.doubleToLongBits(other.d2)) {
				return false;
			}
			return true;
		}

		private SmacPipeline getOuterType() {
			return SmacPipeline.this;
		}

	}

	// private TDoubleList parseLikelihoods(String trebaTestResultPathFile)
	// throws IOException {
	// TDoubleList result = new TDoubleArrayList();
	// BufferedReader br = Files.newBufferedReader(
	// Paths.get(trebaTestResultPathFile), StandardCharsets.UTF_8);
	// String line = null;
	// while ((line = br.readLine()) != null) {
	// result.add(Double.parseDouble(line.split("\\s+")[0]));
	// }
	// br.close();
	// return result;
	// }

	private Map<ZeroProbTransition, Distribution> fit(
			Map<ZeroProbTransition, TDoubleList> timeValueBuckets) {
		final Map<ZeroProbTransition, Distribution> result = new HashMap<>();
		for (final ZeroProbTransition t : timeValueBuckets.keySet()) {
			result.put(t, fitDistribution(timeValueBuckets.get(t)));
		}
		return result;
	}

	@SuppressWarnings("boxing")
	private Distribution fitDistribution(TDoubleList transitionTimes) {
		final Vec v = new DenseVector(transitionTimes.toArray());
		final jsat.utils.Pair<Boolean, Double> sameValues = DistributionSearch
				.checkForDifferentValues(v);
		if (sameValues.getFirstItem()) {
			final Distribution d = new SingleValueDistribution(
					sameValues.getSecondItem());
			return d;
		} else {
			final KernelDensityEstimator kde = new KernelDensityEstimator(v);
			return kde;
		}
	}

	private Map<ZeroProbTransition, TDoubleList> parseAutomatonPaths(
			String trebaResultPathFile, List<TimedSequence> timedSequences)
					throws IOException {
		final Map<ZeroProbTransition, TDoubleList> result = new HashMap<>();
		final BufferedReader br = Files.newBufferedReader(
				Paths.get(trebaResultPathFile), StandardCharsets.UTF_8);
		String line = null;
		int rowIndex = 0;
		int currentState = -1;
		int followingState = -1;
		while ((line = br.readLine()) != null) {
			final String[] split = line.split("\\s+");
			final TDoubleList timeValues = timedSequences.get(rowIndex)
					.getTimeValues();
			final TIntList eventValues = timedSequences.get(rowIndex).getEvents();
			if (split.length - 2 != timeValues.size()) {
				System.err
				.println("There should be one more state than there are time values (time values fill the gaps between the states\n"
						+ Arrays.toString(split) + "\n" + timeValues);
				System.err.println("Error occured in line=" + rowIndex);
				break;
			}
			// first element is likelihood; not interested in that right now
			for (int i = 1; i < split.length - 1; i++) {
				currentState = Integer.parseInt(split[i]);
				followingState = Integer.parseInt(split[i + 1]);
				if (currentState == 394 && followingState == 394
						&& eventValues.get(i - 1) == 8) {
					// Debug stuff
					System.out.println("Found it");
				}
				addTimeValue(result, currentState, followingState,
						eventValues.get(i - 1), timeValues.get(i - 1));
			}

			rowIndex++;
		}
		if (rowIndex != timedSequences.size()) {
			System.err.println("rowCount and sequences length do not match ("
					+ rowIndex + " / " + timedSequences.size() + ")");
		}
		br.close();
		return result;
	}

	private void addTimeValue(Map<ZeroProbTransition, TDoubleList> result,
			int currentState, int followingState, int event, double timeValue) {
		final ZeroProbTransition t = new ZeroProbTransition(currentState,
				followingState, event);
		final TDoubleList list = result.get(t);
		if (list == null) {
			final TDoubleList tempList = new TDoubleArrayList();
			tempList.add(timeValue);
			result.put(t, tempList);
		} else {
			list.add(timeValue);
		}
	}

	int fsmStateCount = -1;

	@SuppressWarnings("null")
	private void computeAutomatonPaths(String trebaAutomatonFile,
			String trebaTrainFileString, String trebaResultPathFile) {
		// treba.log1plus_taylor_init_wrapper();
		final observations o = treba.observations_read(trebaTrainFileString);
		final wfsa fsm = treba.wfsa_read_file(trebaAutomatonFile);
		final int obs_alphabet_size = treba.observations_alphabet_size(o);
		if (o != null && fsm != null
				&& fsm.getAlphabet_size() < obs_alphabet_size) {
			System.err
			.printf("Error: the observations file has symbols outside the FSA alphabet.\n");
			System.exit(1);
		}
		if (0 != trebaConstants.FORMAT_LOG2) {
			treba.wfsa_to_log2(fsm);
		}
		if (o == null) {
			logger.error("Error: the observations file could not be read:{}", trebaTrainFileString);
			System.exit(1);
		}
		if (fsm == null) {
			logger.error("Error: the fsm file could not be read:{}", trebaAutomatonFile);
			System.exit(1);
		}
		fsmStateCount = fsm.getNum_states();
		treba.forward_fsm_to_file(fsm, o, trebaConstants.DECODE_FORWARD_PROB,
				trebaResultPathFile);
		if (o != null) {
			treba.observations_destroy(o);
		}
		if (fsm != null) {
			treba.wfsa_destroy(fsm);
			// treba.log1plus_free_wrapper();
		}
	}

	private void createTrebaFile(List<TimedSequence> timedSequences,
			String trebaTrainFileString) throws IOException {
		final BufferedWriter bw = Files.newBufferedWriter(
				Paths.get(trebaTrainFileString), StandardCharsets.UTF_8);
		for (final TimedSequence ts : timedSequences) {
			bw.write(ts.getEventString());
			bw.append('\n');
		}
		bw.close();

	}

	private List<TimedSequence> parseTimedSequences(String timedInputTrainFile,
			boolean isRti, boolean containsClassLabels) throws IOException {
		final List<TimedSequence> result = Lists.newArrayList();
		final BufferedReader br = Files.newBufferedReader(
				Paths.get(timedInputTrainFile), StandardCharsets.UTF_8);

		String line = null;
		if (isRti) {
			// skip info with alphabet size
			br.readLine();
		}
		while ((line = br.readLine()) != null) {
			result.add(new TimedSequence(line, isRti, containsClassLabels));
		}
		return result;
	}

	public double trainFsm(String eventTrainFile, double g_merge_alpha,
			MergeTest mergeTest, boolean recursiveMerge, String fsmOutputFile) {
		int recursive_merge_test = 0;
		if (recursiveMerge) {
			recursive_merge_test = 1;
		}

		double ll;
		observations o = treba.observations_read(eventTrainFile);
		if (o == null) {
			System.err.println("Error reading observations file");
			System.exit(1);
		}
		o = treba.observations_sort(o);
		o = treba.observations_uniq(o);
		final wfsa fsm = treba.dffa_to_wfsa(treba.dffa_state_merge(o, g_merge_alpha,
				mergeTest.getAlgorithm(), recursive_merge_test));
		ll = treba.loglikelihood_all_observations_fsm(fsm, o);
		treba.wfsa_to_file(fsm, fsmOutputFile);

		if (fsm != null) {
			treba.wfsa_destroy(fsm);
		}
		if (o != null) {
			treba.observations_destroy(o);
		}
		return ll;
	}

}
