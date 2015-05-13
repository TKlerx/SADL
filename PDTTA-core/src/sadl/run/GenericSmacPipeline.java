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

package sadl.run;

import gnu.trove.list.TDoubleList;

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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jsat.distributions.empirical.kernelfunc.BiweightKF;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.TriweightKF;
import jsat.distributions.empirical.kernelfunc.UniformKF;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;
import sadl.constants.DetectorMethod;
import sadl.constants.DistanceMethod;
import sadl.constants.FeatureCreatorMethod;
import sadl.constants.KdeKernelFunction;
import sadl.constants.MergeTest;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.PdttaDetector;
import sadl.detectors.PdttaVectorDetector;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.FullFeatureCreator;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.detectors.threshold.PdttaAggregatedThresholdDetector;
import sadl.detectors.threshold.PdttaFullThresholdDetector;
import sadl.experiments.PdttaExperimentResult;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.Model;
import sadl.interfaces.ModelLearner;
import sadl.interfaces.TrainableDetector;
import sadl.modellearner.PdttaLeaner;
import sadl.models.PDTTA;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.structure.TimedSequence;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public class GenericSmacPipeline implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(GenericSmacPipeline.class);
	// TODO move this to experiment project

	/**
	 * 
	 */
	private static final long serialVersionUID = -6230657726489919272L;
	private static final double TEST_PERCENTAGE = 0.3;
	private static final double TRAIN_PERCENTAGE = 0.7;
	private static final double ANOMALY_PERCENTAGE = 0.1;
	private static final double HUGE_TIME_CHANGE = 0.9;
	private static final double SMALL_TIME_CHANGE = 0.1;

	// just for parsing the one silly smac parameter
	@Parameter()
	private final List<String> rest = new ArrayList<>();

	// just for parsing the one silly smac parameter
	@Parameter(names = "-1", hidden = true)
	private Boolean bla;

	@Parameter(names = "-mergeTest")
	MergeTest mergeTest = MergeTest.ALERGIA;

	@Parameter(names = "-detectorMethod", description = "the anomaly detector method")
	DetectorMethod detectorMethod = DetectorMethod.SVM;

	@Parameter(names = "-featureCreator")
	FeatureCreatorMethod featureCreatorMethod = FeatureCreatorMethod.FULL_FEATURE_CREATOR;

	@Parameter(names = "-scalingMethod")
	ScalingMethod scalingMethod = ScalingMethod.NONE;

	@Parameter(names = "-distanceMetric", description = "Which distance metric to use for DBSCAN")
	DistanceMethod dbScanDistanceMethod = DistanceMethod.EUCLIDIAN;

	@Parameter(names = "-mergeAlpha")
	private double mergeAlpha;
	@Parameter(names = "-dbScanEps")
	private double dbscan_eps;
	@Parameter(names = "-dbScanN")
	private int dbscan_n;

	@Parameter(names = "-smoothingPrior")
	double smoothingPrior = 0;

	@Parameter(names = "-mergeT0")
	int mergeT0 = 3;

	@Parameter(names = "-kdeBandwidth")
	double kdeBandwidth;

	@Parameter(names = "-debug")
	static boolean debug = false;

	@Parameter(names = "-kdeKernelFunction")
	KdeKernelFunction kdeKernelFunctionQualifier;
	KernelFunction kdeKernelFunction;

	@Parameter(names = "-recursiveMergeTest")
	private boolean recursiveMergeTest;
	@Parameter(names = "-aggregatedTimeThreshold")
	private double aggregatedTimeThreshold;
	@Parameter(names = "-aggregatedEventThreshold")
	private double aggregatedEventThreshold;

	@Parameter(names = "-singleEventThreshold")
	private double singleEventThreshold;

	@Parameter(names = "-singleTimeThreshold")
	private double singleTimeThreshold;

	String dataString;

	@Parameter(names = "-probabilityAggregationMethod")
	ProbabilityAggregationMethod aggType = ProbabilityAggregationMethod.NORMALIZED_MULTIPLY;
	@Parameter(names = "-anomalyInsertionType")
	AnomalyInsertionType anomalyInsertionType = AnomalyInsertionType.ALL;

	@Parameter(names = "-svmCosts")
	double svmCosts;

	@Parameter(names = "-svmNu")
	double svmNu;

	@Parameter(names = "-svmGamma")
	double svmGamma;

	@Parameter(names = "-svmEps")
	double svmEps;

	@Parameter(names = "-svmKernel")
	int svmKernelType;

	@Parameter(names = "-svmDegree")
	int svmDegree;

	@Parameter(names = "-svmProbabilityEstimate")
	int svmProbabilityEstimate;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final GenericSmacPipeline sp = new GenericSmacPipeline();
		final JCommander jc = new JCommander(sp);
		System.out.println(Arrays.toString(args));
		if (args.length < 4) {
			logger.error("Please provide the following inputs: [inputFile] 1 1 [Random Seed] [Parameter Arguments..]");
			jc.usage();
			System.exit(1);
		}
		jc.parse(args);
		sp.dataString = args[0];
		logger.info("Running Generic Pipeline with args" + Arrays.toString(args));
		MasterSeed.setSeed(Long.parseLong(args[3]));

		// try {
		final PdttaExperimentResult result = sp.run();
		final Path resultPath = Paths.get("result.csv");
		if (!Files.exists(resultPath)) {
			Files.createFile(resultPath);
		}
		try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
			bw.append(Arrays.toString(args) + "; " + result.toCsvString());
			bw.append('\n');
		}

		System.exit(0);
		// } catch (Exception e) {
		// logger.error("Unexpected exception with parameters" + Arrays.toString(args), e);
		// throw e;
		// }
	}


	FeatureCreator featureCreator;
	PdttaDetector pdttaDetector;


	public PdttaExperimentResult run() throws IOException, InterruptedException {

		if (featureCreatorMethod == FeatureCreatorMethod.FULL_FEATURE_CREATOR) {
			featureCreator = new FullFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.SMALL_FEATURE_CREATOR) {
			featureCreator = new SmallFeatureCreator();
		} else {
			featureCreator = null;
		}
		if (detectorMethod == DetectorMethod.SVM) {
			pdttaDetector = new PdttaVectorDetector(aggType, featureCreator, new LibSvmClassifier(svmProbabilityEstimate, svmGamma, svmNu, svmCosts,
					svmKernelType, svmEps, svmDegree, scalingMethod));
			// pdttaDetector = new PdttaOneClassSvmDetector(aggType, featureCreator, svmProbabilityEstimate, svmGamma, svmNu, svmCosts, svmKernelType, svmEps,
			// svmDegree, scalingMethod);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_AGG_ONLY) {
			pdttaDetector = new PdttaAggregatedThresholdDetector(aggType, aggregatedEventThreshold, aggregatedTimeThreshold);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_ALL) {
			pdttaDetector = new PdttaFullThresholdDetector(aggType, aggregatedEventThreshold, aggregatedTimeThreshold, singleEventThreshold,
					singleTimeThreshold);
		} else if (detectorMethod == DetectorMethod.DBSCAN) {
			// pdttaDetector = new PdttaDbScanDetector(aggType, featureCreator, dbscan_eps, dbscan_n, distanceMethod, scalingMethod);
			pdttaDetector = new PdttaVectorDetector(aggType, featureCreator, new DbScanClassifier(dbscan_eps, dbscan_n, dbScanDistanceMethod, scalingMethod));
		} else {
			pdttaDetector = null;
		}

		if (kdeKernelFunctionQualifier == KdeKernelFunction.BIWEIGHT) {
			kdeKernelFunction = BiweightKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.EPANECHNIKOV) {
			kdeKernelFunction = EpanechnikovKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.GAUSS) {
			kdeKernelFunction = GaussKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.TRIWEIGHT) {
			kdeKernelFunction = TriweightKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.UNIFORM) {
			kdeKernelFunction = UniformKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.ESTIMATE) {
			kdeKernelFunction = null;
		}
		final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
		final long jobNumber = Double.doubleToLongBits(Math.random());

		String jobName = Long.toString(jobNumber);
		jobName = jobName.substring(0, 10);
		// create treba input file
		final String tempFilePrefix = tempDir.toString() + File.separator + jobName + getClass().getName();

		final String timedInputTrainFile = tempFilePrefix + "trainFile";
		final String timedInputTestFile = tempFilePrefix + "testFile";
		splitTrainTestFile(dataString, timedInputTrainFile, timedInputTestFile, TRAIN_PERCENTAGE, TEST_PERCENTAGE, ANOMALY_PERCENTAGE,
				false);
		// parse timed sequences
		// final List<TimedSequence> trainingTimedSequences = TimedSequence.parseTimedSequences(timedInputTrainFile, false, false);
		final TimedInput trainingInput = TimedInput.parseAlt(Paths.get(timedInputTrainFile), 0);
		final ModelLearner learner = new PdttaLeaner(mergeAlpha, recursiveMergeTest, kdeKernelFunction, kdeBandwidth, mergeTest, smoothingPrior, mergeT0);

		final Model model = learner.train(trainingInput);
		PDTTA automaton;
		if (model instanceof PDTTA) {
			automaton = (PDTTA) model;
		} else {
			throw new NotImplementedException("This approach only works for PDTTA models");
		}
		if (pdttaDetector instanceof TrainableDetector) {
			pdttaDetector.setModel(automaton);
			((TrainableDetector) pdttaDetector).train(trainingInput);
		}
		// TODO change this s.t. the train and test set are in one file!
		// then the test set is not generated on the fly anymore
		// TODO for smac change the input format s.t. it contains unlabeled train and labeled test set

		//
		// compute likelihood on test set for automaton and for time PDFs
		// final List<TimedSequence> testTimedSequences = TimedSequence.parseTimedSequences(timedInputTestFile, false, true);
		final TimedInput testInput = TimedInput.parseAlt(Paths.get(timedInputTestFile), 0);
		// XXX here was the anomaly creation before it was deleted
		// if (anomalyInsertionType == AnomalyInsertionType.TYPE_TWO || anomalyInsertionType == AnomalyInsertionType.ALL) {
		// for (int i = 0; i < testTimedSequences.size(); i++) {
		// final TimedSequence ts = testTimedSequences.get(i);
		// if (ts.getLabel() == ClassLabel.ANOMALY && ts.getAnomalyType() == AnomalyInsertionType.TYPE_TWO) {
		// TimedSequence abnormalSequence = automaton.createAbnormalEventSequence(new Random(MasterSeed.nextLong()));
		// int tries = 0;
		// final int maxTries = 1000;
		// while (abnormalSequence.getEvents().size() == 0 && tries < maxTries) {
		// abnormalSequence = automaton.createAbnormalEventSequence(new Random(MasterSeed.nextLong()));
		// tries++;
		// }
		// if (tries >= maxTries) {
		// // no non-empty sequence created after a lot of tries
		// testTimedSequences.remove(i);
		// } else {
		// testTimedSequences.set(i, abnormalSequence);
		// }
		// }
		// }
		// }

		final PdttaExperimentResult result = testPdtta(testInput, automaton);
		logger.info("F-Measure={}", result.getfMeasure());
		System.out.println("Result for SMAC: SUCCESS, 0, 0, " + (1 - result.getfMeasure()) + ", 0");
		// IoUtils.xmlSerialize(automaton, Paths.get("pdtta.xml"));
		// automaton = (PDTTA) IoUtils.xmlDeserialize(Paths.get("pdtta.xml"));
		IoUtils.deleteFiles(new String[] { timedInputTrainFile, timedInputTestFile });
		return result;
	}

	private void splitTrainTestFile(String timedInputFile, String timedInputTrainFile, String timedInputTestFile, double trainPercentage,
			double testPercentage, double anomalyPercentage, boolean isRti) throws IOException {
		logger.info("TimedInputFile=" + timedInputFile);
		final File f = new File(timedInputFile);
		System.out.println(f);
		final LineNumberReader lnr = new LineNumberReader(new FileReader(timedInputFile));
		lnr.skip(Long.MAX_VALUE);
		int samples = lnr.getLineNumber();
		lnr.close();
		final int trainingSamples = (int) (samples * trainPercentage);
		final int testSamples = (int) (samples * testPercentage);
		final int anomalies = (int) (anomalyPercentage * testSamples);
		final int writtenTrainingSamples = 0;
		final int writtenTestSamples = 0;
		int insertedAnomalies = 0;
		final BufferedReader br = Files.newBufferedReader(Paths.get(timedInputFile), StandardCharsets.UTF_8);
		String line = null;
		final BufferedWriter trainWriter = Files.newBufferedWriter(Paths.get(timedInputTrainFile), StandardCharsets.UTF_8);
		final BufferedWriter testWriter = Files.newBufferedWriter(Paths.get(timedInputTestFile), StandardCharsets.UTF_8);
		final Random r = new Random(MasterSeed.nextLong());
		final Random mutation = new Random(MasterSeed.nextLong());
		boolean force = false;
		int lineIndex = 0;
		int linesLeft;
		int anomaliesToInsert;
		if (isRti) {
			br.readLine();
			samples--;
		}
		while ((line = br.readLine()) != null) {
			if (writtenTrainingSamples < trainingSamples && writtenTestSamples < testSamples) {
				// choose randomly according to train/test percentage
				if (r.nextDouble() > testPercentage) {
					// write to train
					writeSample(new TimedSequence(line, true, false).toTrebaString(), trainWriter);
				} else {
					// write to test
					insertedAnomalies = testAndWriteAnomaly(anomalies, insertedAnomalies, anomalyPercentage, line, testWriter, mutation,
							force);
				}
			} else if (writtenTrainingSamples >= trainingSamples) {
				insertedAnomalies = testAndWriteAnomaly(anomalies, insertedAnomalies, anomalyPercentage, line, testWriter, mutation,
						force);
			} else if (writtenTestSamples >= testSamples) {
				// only write trainSamples from now on
				writeSample(new TimedSequence(line, true, false).toTrebaString(), trainWriter);
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

	private int testAndWriteAnomaly(int anomalies, int insertedAnomalies, double anomalyPercentage, String line,
			BufferedWriter testWriter, Random mutation, boolean force) throws IOException {
		final String newLine = line;
		TimedSequence ts = new TimedSequence(newLine, true, false);
		int insertionCount = 0;
		if (insertedAnomalies < anomalies && (force || mutation.nextDouble() < anomalyPercentage)) {
			ts = mutate(ts, anomalyInsertionType, mutation);
			insertionCount++;
		} else {
			ts.setLabel(ClassLabel.NORMAL);
		}
		writeSample(ts.toLabeledString(), testWriter);
		return insertedAnomalies + insertionCount;
	}

	int lastInsertionType = -1;

	private TimedSequence mutate(TimedSequence ts, @SuppressWarnings("hiding") AnomalyInsertionType anomalyInsertionType, Random mutation) throws IOException {
		if (anomalyInsertionType == AnomalyInsertionType.TYPE_ONE) {
			final int toDelete = mutation.nextInt(ts.getEvents().size());
			ts.remove(toDelete);
			ts.setLabel(ClassLabel.ANOMALY);
			// ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_TWO) {
			// for this type of anomaly we need the fsm with distributions and
			// thus, we have to read the testset after the fsm is trained and
			// create the anomalies at that point
			ts.setLabel(ClassLabel.ANOMALY);
			// ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_THREE) {
			final int changeIndex = mutation.nextInt(ts.getTimeValues().size());
			final TDoubleList timeValues = ts.getTimeValues();
			final double changePercent = HUGE_TIME_CHANGE;
			changeTimeValue(mutation, timeValues, changePercent, changeIndex);
			ts.setLabel(ClassLabel.ANOMALY);
			// ts.setAnomalyType(anomalyInsertionType);
			return ts;
		} else if (anomalyInsertionType == AnomalyInsertionType.TYPE_FOUR) {
			final TDoubleList timeValues = ts.getTimeValues();
			final double changePercent = SMALL_TIME_CHANGE;
			for (int i = 0; i < timeValues.size(); i++) {
				changeTimeValue(mutation, timeValues, changePercent, i);
			}
			// ts.setAnomalyType(anomalyInsertionType);
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

	private void changeTimeValue(Random mutation, TDoubleList timeValues, double changePercent, int i) {
		double newValue = timeValues.get(i);
		if (mutation.nextBoolean()) {
			newValue = newValue + newValue * changePercent;
		} else {
			newValue = newValue - newValue * changePercent;
		}
		timeValues.set(i, newValue);
	}

	private void writeSample(String line, BufferedWriter writer) throws IOException {
		writer.write(line);
		writer.append('\n');
	}

	private PdttaExperimentResult testPdtta(TimedInput testTimedSequences, PDTTA automaton) throws IOException {
		// TODO put this in an evaluation class
		logger.info("Testing with {} sequences", testTimedSequences.size());
		pdttaDetector.setModel(automaton);
		final boolean[] detectorResult = pdttaDetector.areAnomalies(testTimedSequences);
		int truePos = 0;
		int trueNeg = 0;
		int falsePos = 0;
		int falseNeg = 0;
		// prec = tp/(tp +fp)
		// The precision is the ratio between correctly detected anomalies and
		// all detected anomalies
		// rec = tp/(tp+fn)
		// The recall is the ratio between detected anomalies and all anomalies

		for (int i = 0; i < testTimedSequences.size(); i++) {
			final TimedWord s = testTimedSequences.get(i);
			if (s.getLabel() == ClassLabel.NORMAL) {
				if (detectorResult[i]) {
					// detector said anomaly
					falsePos++;
				} else {
					// detector said normal
					trueNeg++;
				}
			} else if (s.getLabel() == ClassLabel.ANOMALY) {
				if (detectorResult[i]) {
					// detector said anomaly
					truePos++;
				} else {
					// detector said normal
					falseNeg++;
				}
			}

		}
		final PdttaExperimentResult expResult = new PdttaExperimentResult(truePos, trueNeg, falsePos, falseNeg);

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
			if (Double.doubleToLongBits(d1) != Double.doubleToLongBits(other.d1)) {
				return false;
			}
			if (Double.doubleToLongBits(d2) != Double.doubleToLongBits(other.d2)) {
				return false;
			}
			return true;
		}

		private GenericSmacPipeline getOuterType() {
			return GenericSmacPipeline.this;
		}

	}



}
