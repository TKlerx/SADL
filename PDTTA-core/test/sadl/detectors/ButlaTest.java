/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
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
		ExperimentResult expected = new ExperimentResult(1831, 467, 0, 2702);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1778, 170, 325, 2727);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1861,375,111 , 2653);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1748, 523, 0,2729);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1818, 384, 78,2720);
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
		ExperimentResult expected = new ExperimentResult(4527, 464, 3, 6);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(4494, 40, 455, 11);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(4495, 283, 203, 19);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(4467, 523, 0, 10);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(4534, 362, 100, 4);
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
		ExperimentResult expected = new ExperimentResult(4243, 88, 379, 290);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4174, 307, 188, 331);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4258, 334, 152, 256);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4146, 72, 451, 331);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4195, 455, 7, 343);
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
		ExperimentResult expected = new ExperimentResult(1382, 467, 0, 3151);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(1440, 381, 114, 3065);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(1640, 486, 0, 2874);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(1388, 523, 0, 3089);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		expected = new ExperimentResult(1379, 462, 0, 3159);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		assertEquals(expected, actual);

		logger.info("Finished Threshold Original KDE test.");
	}

}
