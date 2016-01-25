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
package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.DistanceMethod;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.constants.TransitionsType;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.oneclassclassifier.NumericClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.oneclassclassifier.clustering.XMeansClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class ButlaTest {
	private static Logger logger = LoggerFactory.getLogger(ButlaTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		MasterSeed.reset();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAnodaOriginalKDE() throws IOException, URISyntaxException {
		logger.info("Starting Anoda Original KDE test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Outgoing, 0.05, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
		final AnodaDetector anoda = new AnodaDetector();

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(467, 3034, 1499, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(0, 3024, 1481, 495);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(374, 3036, 1478, 112);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 3005, 1472, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(384, 3039, 1499, 78);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
		logger.info("Finished Anoda Original KDE test.");

	}

	@Test
	public void testAnodaOriginalButla() throws IOException, URISyntaxException {

		logger.info("Starting Anoda Original BUTLA test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalButlaVariableBandwidth);
		final AnodaDetector anoda = new AnodaDetector();

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(281, 2118, 2415, 186);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=377, trueNegatives=1966, falsePositives=2567, falseNegatives=90, executionTimeTraining=00:03:27.512,
		// executionTimeTesting=00:00:00.143, numberOfStates=43, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(147, 2402, 2103, 348);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=228, trueNegatives=1667, falsePositives=2838, falseNegatives=267, executionTimeTraining=00:02:36.574,
		// executionTimeTesting=00:00:00.005, numberOfStates=445, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(322, 2248, 2266, 164);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=486, trueNegatives=1295, falsePositives=3219, falseNegatives=0, executionTimeTraining=00:19:40.180,
		// executionTimeTesting=00:00:00.012, numberOfStates=3010, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 2373, 2104, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=523, trueNegatives=1148, falsePositives=3329, falseNegatives=0, executionTimeTraining=00:25:46.229,
		// executionTimeTesting=00:00:00.018, numberOfStates=3022, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(225, 2207, 2331, 237);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=462, trueNegatives=1153, falsePositives=3385, falseNegatives=0, executionTimeTraining=00:25:25.535,
		// executionTimeTesting=00:00:00.004, numberOfStates=2985, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		logger.info("Finished Anoda Original BUTLA test.");
	}

	@Test
	public void testXMeansOriginalKDE() throws IOException, URISyntaxException {
		logger.info("Starting Anoda Original KDE test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Outgoing, 0.01, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new XMeansClassifier(ScalingMethod.NORMALIZE, 0.05, 0, DistanceMethod.EUCLIDIAN);
		// final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-1)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(467, 3034, 1499, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(0, 3024, 1481, 495);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(374, 3036, 1478, 112);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 3005, 1472, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(384, 3039, 1499, 78);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
		logger.info("Finished Anoda Original KDE test.");

	}

	@Test
	public void testThresholdOriginalButla() throws IOException, URISyntaxException {

		logger.info("Starting Anoda Original BUTLA test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalButlaVariableBandwidth);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new XMeansClassifier(ScalingMethod.NORMALIZE, 0.05, 0, DistanceMethod.EUCLIDIAN);
		// final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(281, 2118, 2415, 186);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=377, trueNegatives=1966, falsePositives=2567, falseNegatives=90, executionTimeTraining=00:03:27.512,
		// executionTimeTesting=00:00:00.143, numberOfStates=43, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(147, 2402, 2103, 348);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=228, trueNegatives=1667, falsePositives=2838, falseNegatives=267, executionTimeTraining=00:02:36.574,
		// executionTimeTesting=00:00:00.005, numberOfStates=445, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(322, 2248, 2266, 164);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=486, trueNegatives=1295, falsePositives=3219, falseNegatives=0, executionTimeTraining=00:19:40.180,
		// executionTimeTesting=00:00:00.012, numberOfStates=3010, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 2373, 2104, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=523, trueNegatives=1148, falsePositives=3329, falseNegatives=0, executionTimeTraining=00:25:46.229,
		// executionTimeTesting=00:00:00.018, numberOfStates=3022, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(225, 2207, 2331, 237);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=462, trueNegatives=1153, falsePositives=3385, falseNegatives=0, executionTimeTraining=00:25:25.535,
		// executionTimeTesting=00:00:00.004, numberOfStates=2985, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		logger.info("Finished Anoda Original BUTLA test.");
	}

}
