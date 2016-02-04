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
import sadl.constants.IntervalCreationStrategy;
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
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.extendInterval);
		final AnodaDetector anoda = new AnodaDetector();

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(467, 1831, 2702, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(170, 1778, 2727, 325);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(375, 1861, 2653, 111);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 1748, 2729, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(384, 1818, 2720, 78);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
		logger.info("Finished Anoda Original KDE test.");

	}

	@Test
	public void testAnodaOriginalButla() throws IOException, URISyntaxException {

		logger.info("Starting Anoda Original BUTLA test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.9, TransitionsType.Incoming, 0.000001, 0.3, PTAOrdering.BottomUp,
				EventsCreationStrategy.NotTimedEvents, KDEFormelVariant.OriginalButlaVariableBandwidth, IntervalCreationStrategy.extendInterval);
		final AnodaDetector anoda = new AnodaDetector();

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(464, 4527, 6, 3);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(40, 4494, 11, 455);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(283, 4495, 19, 203);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 4467, 10, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(362, 4534, 4, 100);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		logger.info("Finished Anoda Original BUTLA test.");
	}

	@Test
	public void testXMeansOriginalKDE() throws IOException, URISyntaxException {
		logger.info("Starting XMeans Original KDE test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Outgoing, 0.01, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.extendInterval);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new XMeansClassifier(ScalingMethod.NORMALIZE, 0.2, 0, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(88, 4243, 290, 379);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(307, 4174, 331, 188);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(334, 4258, 256, 152);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(72, 4146, 331, 451);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(455, 4195, 343, 7);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
		logger.info("Finished XMeans Original KDE test.");

	}

	@Test
	public void testThresholdOriginalKDE() throws IOException, URISyntaxException {

		logger.info("Starting Threshold Original KDE test...");
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Incoming, 0.01, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.extendInterval);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-1)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(467, 1382, 3151, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(381, 1440, 3065, 114);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(486, 1640, 2874, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 1388, 3089, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(462, 1379, 3159, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		logger.info("Finished Threshold Original KDE test.");
	}

}
