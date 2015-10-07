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

package sadl.run.pipelines;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import sadl.anomalydetecion.AnomalyDetection;
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
import sadl.experiments.ExperimentResult;
import sadl.interfaces.Model;
import sadl.interfaces.ModelLearner;
import sadl.modellearner.rtiplus.GreedyPDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner.DistributionCheckType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.OperationTesterType;
import sadl.models.pdrta.PDRTA;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.OneClassClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
@Deprecated
public class RTISmacPipeline implements Serializable {
	private static final long serialVersionUID = 4962328747559099050L;

	private static Logger logger = LoggerFactory.getLogger(RTISmacPipeline.class);
	// TODO move this to experiment project

	String dataString;

	// should be empty. not used, but for parsing smac stuff
	@Parameter()
	private final List<String> rest = new ArrayList<>();

	// just for parsing the one silly smac parameter
	@Parameter(names = "-1", hidden = true)
	private Boolean bla;

	@Parameter(names = "-debug")
	boolean debug = false;

	// RTI+ Parameters
	@Parameter(names = "-sig", required = true, arity = 1)
	private double sig;

	@Parameter(names = "-hist", required = true, arity = 1)
	private String hist;

	@Parameter(names = "-greedy", arity = 0)
	boolean greedy = false;

	@Parameter(names = "-em", arity = 1)
	OperationTesterType tester = OperationTesterType.LRT;

	@Parameter(names = "-ida", arity = 1)
	DistributionCheckType distrCheck = DistributionCheckType.DISABLED;

	@Parameter(names = "-bop", arity = 1)
	String boolOps = "AAA";

	@Parameter(names = "-steps", arity = 1)
	String stepsDir = null;

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
	FeatureCreatorMethod featureCreatorMethod = FeatureCreatorMethod.FULL_FEATURE_CREATOR;

	@Parameter(names = "-scalingMethod")
	ScalingMethod scalingMethod = ScalingMethod.NONE;

	@Parameter(names = "-distanceMetric", description = "Which distance metric to use for DBSCAN")
	DistanceMethod dbScanDistanceMethod = DistanceMethod.EUCLIDIAN;

	@Parameter(names = "-dbScanEps")
	private double dbscan_eps;

	@Parameter(names = "-dbScanN")
	private int dbscan_n;

	private static final boolean abort = true;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (abort) {
			throw new UnsupportedOperationException("This class is no longer supported! Use SADL main class with smac command");
		}
		final RTISmacPipeline sp = new RTISmacPipeline();
		final JCommander jc = new JCommander(sp);
		if (args.length < 4) {
			logger.error("Please provide the following inputs: [inputFile] 1 1 [Random Seed] [Parameter Arguments..]");
			jc.usage();
			System.exit(1);
		}
		jc.setAcceptUnknownOptions(true);
		jc.parse(args);
		sp.dataString = args[0];
		logger.info("Running Generic Pipeline with args" + Arrays.toString(args));
		MasterSeed.setSeed(Long.parseLong(args[3]));

		try {
			boolean fileExisted = true;
			final ExperimentResult result = sp.run();
			final Path resultPath = Paths.get("result.csv");
			if (!Files.exists(resultPath)) {
				Files.createFile(resultPath);
				fileExisted = false;
			}
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
				if (!fileExisted) {
					bw.append(ExperimentResult.CsvHeader());
					bw.append('\n');
				}
				bw.append(df.format(new Date()));
				bw.append(" ; ");
				bw.append(Arrays.toString(args));
				bw.append("; ");
				bw.append(result.toCsvString());
				bw.append('\n');
			}

			System.exit(0);
		} catch (final Exception e) {
			logger.error("Unexpected exception with parameters" + Arrays.toString(args), e);
			throw e;
		}
	}

	FeatureCreator featureCreator;
	AnomalyDetector anomalyDetector;
	OneClassClassifier classifier;

	public ExperimentResult run() throws IOException, InterruptedException {
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

		final ModelLearner learner;
		if (!greedy) {
			learner = new SimplePDRTALearner(sig, hist, tester, distrCheck, boolOps, stepsDir);
		} else {
			learner = new GreedyPDRTALearner(sig, hist, tester, distrCheck, boolOps, stepsDir);
		}
		final AnomalyDetection detection = new AnomalyDetection(anomalyDetector, learner);
		final Model m = detection.train(Paths.get(dataString));
		final PDRTA p = (PDRTA) m;
		System.out.println(p.toString());
		// final ExperimentResult result = detection.trainTest(dataString);
		// System.out.println("Result for SMAC: SUCCESS, 0, 0, " + (1 - result.getFMeasure()) + ", 0");
		// IoUtils.xmlSerialize(automaton, Paths.get("pdtta.xml"));
		// automaton = (PDTTA) IoUtils.xmlDeserialize(Paths.get("pdtta.xml"));
		return null;
	}

}
