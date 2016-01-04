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
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.TransitionsType;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class AnodaTest {
	private static Logger logger = LoggerFactory.getLogger(AnodaTest.class);

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
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
		final AnodaDetector anoda = new AnodaDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(463, 2906, 1627, 4);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		detection.getLearnedModel();
		assertEquals(expected, actual);

		expected = new ExperimentResult(0, 2882, 1623, 495);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(374, 2923, 1591, 112);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 2831, 1646, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(373, 2867, 1671, 89);
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
		final AnodaDetector anoda = new AnodaDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(311, 1705, 2828, 156);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);
		TimedInput trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		ExperimentResult actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=377, trueNegatives=1966, falsePositives=2567, falseNegatives=90, executionTimeTraining=00:03:27.512,
		// executionTimeTesting=00:00:00.143, numberOfStates=43, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(463, 1030, 3475, 32);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=228, trueNegatives=1667, falsePositives=2838, falseNegatives=267, executionTimeTraining=00:02:36.574,
		// executionTimeTesting=00:00:00.005, numberOfStates=445, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(486, 1116, 3398, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=486, trueNegatives=1295, falsePositives=3219, falseNegatives=0, executionTimeTraining=00:19:40.180,
		// executionTimeTesting=00:00:00.012, numberOfStates=3010, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 1038, 3439, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		trainTest = IoUtils.readTrainTestFile(p);
		trainSet = trainTest.getKey();
		trainSet.decreaseSamples(0.1);
		actual = detection.trainTest(trainSet, trainTest.getValue());
		// ExperimentResult [truePositives=523, trueNegatives=1148, falsePositives=3329, falseNegatives=0, executionTimeTraining=00:25:46.229,
		// executionTimeTesting=00:00:00.018, numberOfStates=3022, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(462, 1099, 3439, 0);
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
