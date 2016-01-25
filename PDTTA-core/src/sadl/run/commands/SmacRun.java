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
package sadl.run.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.Algoname;
import sadl.constants.DetectorMethod;
import sadl.constants.DistanceMethod;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.FeatureCreatorMethod;
import sadl.constants.KDEFormelVariant;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.AnodaDetector;
import sadl.detectors.AnomalyDetector;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.FullFeatureCreator;
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.models.pta.Event;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.OneClassClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.oneclassclassifier.clustering.GMeansClassifier;
import sadl.oneclassclassifier.clustering.KMeansClassifier;
import sadl.oneclassclassifier.clustering.XMeansClassifier;
import sadl.run.factories.LearnerFactory;
import sadl.run.factories.learn.ButlaFactory;
import sadl.run.factories.learn.PdttaFactory;
import sadl.run.factories.learn.PetriNetFactory;
import sadl.run.factories.learn.RTIFactory;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import sadl.utils.RamGobbler;

public class SmacRun {

	private enum QualityCriterion {
		F_MEASURE, PRECISION, RECALL, ACCURACY, PHI_COEFFICIENT
	}

	private static final Logger logger = LoggerFactory.getLogger(SmacRun.class);


	/*
	 * ################### SMAC Params ###################
	 */
	// // should be empty. not used, but for parsing smac stuff
	@Parameter()
	private final List<String> mainParams = new ArrayList<>();

	// just for parsing the one silly smac parameter
	@Parameter(names = "-1", hidden = true)
	private Boolean bla;

	@Parameter(names = "-qualityCriterion")
	QualityCriterion qCrit = QualityCriterion.F_MEASURE;

	// @ParametersDelegate
	// private final TrainRun trainRun = new TrainRun(true);
	//
	// @ParametersDelegate
	// private final TestRun testRun = new TestRun(true);

	/*
	 * ################### Tester Params ###################
	 */
	// Detector parameters
	@Parameter(names = "-aggregateSublists", arity = 1)
	boolean aggregateSublists = false;

	@Parameter(names = "-aggregatedTimeThreshold")
	private double aggregatedTimeThreshold;

	@Parameter(names = "-aggregatedEventThreshold")
	private double aggregatedEventThreshold;

	@Parameter(names = "-singleEventThreshold")
	private double singleEventThreshold;

	@Parameter(names = "-singleTimeThreshold")
	private double singleTimeThreshold;

	@Parameter(names = "-probabilityAggregationMethod")
	ProbabilityAggregationMethod aggType = ProbabilityAggregationMethod.NORMALIZED_MULTIPLY;

	@Parameter(names = "-svmNu")
	double svmNu;

	@Parameter(names = "-svmGamma")
	double svmGamma;

	@Parameter(names = "-svmGammaEstimate", arity = 1)
	boolean svmGammaEstimate;

	@Parameter(names = "-svmEps")
	double svmEps;

	@Parameter(names = "-svmKernel")
	int svmKernelType;

	@Parameter(names = "-svmDegree")
	int svmDegree;

	@Parameter(names = "-svmProbabilityEstimate")
	int svmProbabilityEstimate;

	@Parameter(names = "-detectorMethod", description = "the anomaly detector method")
	DetectorMethod detectorMethod;

	@Parameter(names = "-featureCreator")
	FeatureCreatorMethod featureCreatorMethod;

	@Parameter(names = "-scalingMethod")
	ScalingMethod scalingMethod = ScalingMethod.NONE;

	@Parameter(names = "-distanceMetric", description = "Which distance metric to use for clustering")
	DistanceMethod clusteringDistanceMethod = DistanceMethod.EUCLIDIAN;

	@Parameter(names = "-dbScanEps")
	private double dbscan_eps;

	@Parameter(names = "-dbScanN")
	private int dbscan_n;

	@Parameter(names = "-dbScanThreshold")
	private double dbscan_threshold = -1;

	@Parameter(names = "-kmeansThreshold")
	private final double kmeans_threshold = -1;

	@Parameter(names = "-kmeansMinPoints")
	private final int kmeans_minPoints = 0;

	@Parameter(names = "-kmeansK")
	private final int kmeans_k = 2;

	@Parameter(names = "-skipFirstElement", arity = 1)
	boolean skipFirstElement = false;

	@Parameter(names = "-butlaPreprocessing", arity = 1)
	boolean applyButlaPreprocessing = false;

	@Parameter(names = "-butlaPreprocessingBandwidthEstimate", arity = 1)
	boolean butlaPreprocessingBandwidthEstimate = false;

	@Parameter(names = "-butlaPreprocessingBandwidth")
	double butlaPreprocessingBandwidth = 10000;

	@SuppressWarnings("null")
	public ExperimentResult run(JCommander jc) {
		final RamGobbler gobbler = new RamGobbler();
		gobbler.start();
		logger.info("Starting new SmacRun with commands={}", jc.getUnknownOptions());
		MasterSeed.setSeed(Long.parseLong(mainParams.get(4)));

		// TODO Try to use this again
		// final Pair<TimedInput, TimedInput> inputs = IoUtils.readTrainTestFile(inputSeqs);
		// trainRun.trainSeqs = inputs.getFirst();
		// testRun.trainSeqs = inputs.getFirst();
		// testRun.testSeqs = inputs.getSecond();
		//
		// final Model m = trainRun.run(jc);
		// testRun.testModel = m;
		// final ExperimentResult result = testRun.run();

		FeatureCreator featureCreator;
		AnomalyDetector anomalyDetector;
		OneClassClassifier classifier;
		if (featureCreatorMethod == FeatureCreatorMethod.FULL) {
			featureCreator = new FullFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.SMALL) {
			featureCreator = new SmallFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.MINIMAL) {
			featureCreator = new MinimalFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.UBER) {
			featureCreator = new UberFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.SINGLE) {
			featureCreator = new AggregatedSingleFeatureCreator();
		} else {
			featureCreator = null;
		}
		if (detectorMethod == DetectorMethod.SVM) {
			if (svmGammaEstimate) {
				svmGamma = 0;
			}
			classifier = new LibSvmClassifier(svmProbabilityEstimate, svmGamma, svmNu, svmKernelType, svmEps, svmDegree, scalingMethod);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_SINGLE) {
			// only works with minimal feature creator
			if (featureCreatorMethod != null && featureCreatorMethod != FeatureCreatorMethod.SINGLE) {
				throw new IllegalArgumentException(
						"Please do only specify " + FeatureCreatorMethod.SINGLE + " or no featureCreatorMethod for " + detectorMethod);
			}
			featureCreator = new AggregatedSingleFeatureCreator();
			classifier = new ThresholdClassifier(aggregatedEventThreshold);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_AGG_ONLY) {
			// only works with minimal feature creator
			if (featureCreatorMethod != null && featureCreatorMethod != FeatureCreatorMethod.MINIMAL) {
				throw new IllegalArgumentException(
						"Please do only specify " + FeatureCreatorMethod.MINIMAL + " or no featureCreatorMethod for " + detectorMethod);
			}
			featureCreator = new MinimalFeatureCreator();
			classifier = new ThresholdClassifier(aggregatedEventThreshold, aggregatedTimeThreshold);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_ALL) {
			// only works with small feature creator
			if (featureCreatorMethod != null && featureCreatorMethod != FeatureCreatorMethod.SMALL) {
				throw new IllegalArgumentException(
						"Please do only specify " + FeatureCreatorMethod.SMALL + " or no featureCreatorMethod for " + detectorMethod);
			}
			featureCreator = new SmallFeatureCreator();
			classifier = new ThresholdClassifier(aggregatedEventThreshold, aggregatedTimeThreshold, singleEventThreshold, singleTimeThreshold);
		} else if (detectorMethod == DetectorMethod.DBSCAN) {
			if (dbscan_threshold <= 0) {
				dbscan_threshold = dbscan_eps;
			}
			classifier = new DbScanClassifier(dbscan_eps, dbscan_n, dbscan_threshold, clusteringDistanceMethod, scalingMethod);
		} else if (detectorMethod == DetectorMethod.GMEANS) {
			classifier = new GMeansClassifier(scalingMethod, kmeans_threshold, kmeans_minPoints, clusteringDistanceMethod);
		} else if (detectorMethod == DetectorMethod.XMEANS) {
			classifier = new XMeansClassifier(scalingMethod, kmeans_threshold, kmeans_minPoints, clusteringDistanceMethod);
		} else if (detectorMethod == DetectorMethod.KMEANS) {
			classifier = new KMeansClassifier(scalingMethod, kmeans_k, kmeans_threshold, kmeans_minPoints, clusteringDistanceMethod);
		} else {
			classifier = null;
		}

		final ProbabilisticModelLearner learner = getLearner(Algoname.getAlgoname(mainParams.get(0)), jc);
		final AnomalyDetection detection;
		if (detectorMethod == DetectorMethod.ANODA) {
			detection = new AnomalyDetection(new AnodaDetector(), learner);
		} else {
			if (classifier == null || featureCreator == null) {
				throw new IllegalStateException("classifier or featureCreator is null");
			}
			anomalyDetector = new VectorDetector(aggType, featureCreator, classifier, aggregateSublists);
			detection = new AnomalyDetection(anomalyDetector, learner);
		}
		ExperimentResult result = null;
		try {
			final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(mainParams.get(1)), skipFirstElement);
			TimedInput trainSet = trainTest.getKey();
			TimedInput testSet = trainTest.getValue();
			if (applyButlaPreprocessing) {
				double bandwidth;
				if (butlaPreprocessingBandwidthEstimate) {
					bandwidth = 0;
				} else {
					bandwidth = butlaPreprocessingBandwidth;
				}
				final ButlaPdtaLearner butla = new ButlaPdtaLearner(bandwidth, EventsCreationStrategy.SplitEvents,
						KDEFormelVariant.OriginalKDE);
				final Pair<TimedInput, Map<String, Event>> pair = butla.splitEventsInTimedSequences(trainSet);
				trainSet = pair.getKey();
				testSet = butla.getSplitInputForMapping(testSet, pair.getValue());
			}
			result = detection.trainTest(trainSet, testSet);
		} catch (final IOException e) {
			logger.error("Error when loading input from file: " + e.getMessage());
			smacErrorAbort();
		}

		// Can stay the same
		double qVal = 0.0;
		switch (qCrit) {
			case F_MEASURE:
				qVal = result.getFMeasure();
				break;
			case PRECISION:
				qVal = result.getPrecision();
				break;
			case RECALL:
				qVal = result.getRecall();
				break;
			case PHI_COEFFICIENT:
				qVal = result.getPhiCoefficient();
				break;
			case ACCURACY:
				qVal = result.getAccuracy();
				break;
			default:
				logger.error("Quality criterion not found!");
				break;
		}

		logger.info("{}={}", qCrit.name(), qVal);
		result.setAvgMemoryUsage(gobbler.getAvgRam());
		result.setMaxMemoryUsage(gobbler.getMaxRam());
		result.setMinMemoryUsage(gobbler.getMinRam());
		logger.info("{}", result);
		gobbler.shutdown();
		System.out.println("Result for SMAC: SUCCESS, 0, 0, " + (1 - qVal) + ", 0");
		return result;
	}

	@SuppressWarnings("unused")
	@Deprecated
	private Pair<Algoname, Path> extractAlgoAndInput() {

		final Set<String> algoNames = Arrays.stream(Algoname.values()).map(a -> a.name().toLowerCase()).collect(Collectors.toSet());

		Algoname algo = null;
		Path input = null;
		for (final String arg : mainParams) {
			if (algoNames.contains(arg.toLowerCase()) && algo == null) {
				algo = Algoname.getAlgoname(arg);
			} else if (arg.contains("/") && input == null) {
				input = Paths.get(arg);
			}
		}
		if (algo == null) {
			logger.error("Algo not found for mainParams={}!", mainParams);
			smacErrorAbort();
		}
		return Pair.of(algo, input);
	}

	private ProbabilisticModelLearner getLearner(Algoname algoName, JCommander jc) {

		LearnerFactory lf = null;

		switch (algoName) {
			case RTI:
				lf = new RTIFactory();
				break;
			case PDTTA:
				lf = new PdttaFactory();
				break;
			case PETRI_NET:
				lf = new PetriNetFactory();
				break;
			case BUTLA:
				lf = new ButlaFactory();
				break;
				// TODO Add other learning algorithms
			default:
				logger.error("Unknown algo param {}!", algoName);
				smacErrorAbort();
				break;
		}

		final JCommander subjc = new JCommander(lf);
		final String[] subOptions = jc.getUnknownOptions().toArray(new String[0]);
		logger.debug("Unknown options array for jcommander={}", Arrays.toString(subOptions));
		subjc.parse(subOptions);

		@SuppressWarnings("null")
		final ProbabilisticModelLearner ml = lf.create();
		return ml;
	}

	protected static void smacErrorAbort() {
		System.out.println("Result for SMAC: CRASHED, 0, 0, 0, 0");
		System.exit(1);
	}

}
