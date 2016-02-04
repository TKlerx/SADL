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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import sadl.constants.DetectorMethod;
import sadl.constants.DistanceMethod;
import sadl.constants.FeatureCreatorMethod;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.AnomalyDetector;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.FullFeatureCreator;
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.evaluation.Evaluation;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.interfaces.TrainableDetector;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.OneClassClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.utils.IoUtils;

public class TestRun {

	private static final Logger logger = LoggerFactory.getLogger(TestRun.class);

	private final boolean smacMode;

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

	@Parameter(names = "-svmEps")
	double svmEps;

	@Parameter(names = "-svmKernel")
	int svmKernelType;

	@Parameter(names = "-svmDegree")
	int svmDegree;

	@Parameter(names = "-svmProbabilityEstimate")
	int svmProbabilityEstimate;

	@Parameter(names = "-detectorMethod", description = "the anomaly detector method")
	DetectorMethod detectorMethod = DetectorMethod.SVM;

	@Parameter(names = "-featureCreator")
	FeatureCreatorMethod featureCreatorMethod = FeatureCreatorMethod.FULL;

	@Parameter(names = "-scalingMethod")
	ScalingMethod scalingMethod = ScalingMethod.NONE;

	@Parameter(names = "-distanceMetric", description = "Which distance metric to use for DBSCAN")
	DistanceMethod dbScanDistanceMethod = DistanceMethod.EUCLIDIAN;

	@Parameter(names = "-dbScanEps")
	private double dbscan_eps;

	@Parameter(names = "-dbScanN")
	private int dbscan_n;

	@Parameter(names = "-model", required = true)
	private Path modelFile;

	ProbabilisticModel testModel;

	@Parameter(names = "-trainSeqs")
	private Path trainIn;
	TimedInput trainSeqs;

	@Parameter(names = "-testSeqs")
	private Path testIn;
	TimedInput testSeqs;

	@Parameter(names = "-resOut")
	private final Path resultOut = Paths.get("sadl_test_res.csv");

	private OneClassClassifier classifier;;

	public TestRun(boolean smacMode) {
		this.smacMode = smacMode;
	}

	public ExperimentResult run() {

		FeatureCreator featureCreator;
		AnomalyDetector anomalyDetector;

		if (featureCreatorMethod == FeatureCreatorMethod.FULL) {
			featureCreator = new FullFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.SMALL) {
			featureCreator = new SmallFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.MINIMAL) {
			featureCreator = new MinimalFeatureCreator();
		} else {
			featureCreator = null;
		}

		if (detectorMethod == DetectorMethod.SVM) {
			classifier = new LibSvmClassifier(svmProbabilityEstimate, svmGamma, svmNu, svmKernelType, svmEps, svmDegree, scalingMethod);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_AGG_ONLY) {
			classifier = new ThresholdClassifier(aggregatedEventThreshold, aggregatedTimeThreshold);
		} else if (detectorMethod == DetectorMethod.THRESHOLD_ALL) {
			classifier = new ThresholdClassifier(aggregatedEventThreshold, aggregatedTimeThreshold, singleEventThreshold, singleTimeThreshold);
		} else if (detectorMethod == DetectorMethod.DBSCAN) {
			classifier = new DbScanClassifier(dbscan_eps, dbscan_n, dbScanDistanceMethod, scalingMethod);
		} else {
			classifier = null;
		}
		anomalyDetector = new VectorDetector(aggType, featureCreator, classifier);

		if(!smacMode){
			try {
				testModel = (ProbabilisticModel) IoUtils.deserialize(modelFile);
			} catch (final Exception e) {
				logger.error("Error when loading model from file!", e);
			}
			try {
				testSeqs = TimedInput.parse(testIn);
			} catch (final IOException e) {
				logger.error("Error when parsing the test sequences from file!", e);
			}
		}

		if (anomalyDetector instanceof TrainableDetector) {
			anomalyDetector.setModel(testModel);
			if (!smacMode) {
				try {
					trainSeqs = TimedInput.parse(trainIn);
				} catch (final IOException e) {
					logger.error("Error when parsing the train sequences from file!", e);
				}
			}
			((TrainableDetector) anomalyDetector).train(trainSeqs);
		}

		final Evaluation eval = new Evaluation(anomalyDetector, testModel);
		final ExperimentResult result = eval.evaluate(testSeqs);

		if (!smacMode) {
			try (BufferedWriter bw= Files.newBufferedWriter(resultOut)){
				bw.write(result.toCsvString());
				bw.close();
				System.out.println("F-Measure=" + result.getFMeasure());
			}			catch (final IOException e) {
				logger.error("Error when storing the test result to file!", e);
			}
		}
		return result;
	}
}
