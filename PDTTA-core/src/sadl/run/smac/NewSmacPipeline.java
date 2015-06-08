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

package sadl.run.smac;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsat.distributions.empirical.kernelfunc.BiweightKF;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.TriweightKF;
import jsat.distributions.empirical.kernelfunc.UniformKF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.anomalydetecion.PdttaAnomalyDetection;
import sadl.constants.AnomalyInsertionType;
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
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.detectors.threshold.PdttaAggregatedThresholdDetector;
import sadl.detectors.threshold.PdttaFullThresholdDetector;
import sadl.experiments.PdttaExperimentResult;
import sadl.interfaces.ModelLearner;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * 
 * @author Timo Klerx
 *
 */
public class NewSmacPipeline implements Serializable {
	private static final long serialVersionUID = 4962328747559099050L;

	private static Logger logger = LoggerFactory.getLogger(NewSmacPipeline.class);
	// TODO move this to experiment project

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
	private final boolean debug = false;

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
		final NewSmacPipeline sp = new NewSmacPipeline();
		final JCommander jc = new JCommander(sp);
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
		if (debug) {
			Settings.setDebug(debug);
		}
		if (featureCreatorMethod == FeatureCreatorMethod.FULL_FEATURE_CREATOR) {
			featureCreator = new FullFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.SMALL_FEATURE_CREATOR) {
			featureCreator = new SmallFeatureCreator();
		} else if (featureCreatorMethod == FeatureCreatorMethod.MINIMAL_FEATURE_CREATOR) {
			featureCreator = new MinimalFeatureCreator();
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
		final ModelLearner learner = new PdttaLearner(mergeAlpha, recursiveMergeTest, kdeKernelFunction, kdeBandwidth, mergeTest, smoothingPrior, mergeT0);
		final PdttaAnomalyDetection detection = new PdttaAnomalyDetection(pdttaDetector, learner);
		final PdttaExperimentResult result = detection.trainTest(dataString);
		System.out.println("Result for SMAC: SUCCESS, 0, 0, " + (1 - result.getFMeasure()) + ", 0");
		// IoUtils.xmlSerialize(automaton, Paths.get("pdtta.xml"));
		// automaton = (PDTTA) IoUtils.xmlDeserialize(Paths.get("pdtta.xml"));
		return result;
	}

}
