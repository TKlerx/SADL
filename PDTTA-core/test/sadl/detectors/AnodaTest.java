package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import sadl.modellearner.ButlaPdtaLearner;

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
		ExperimentResult expected = new ExperimentResult(377, 1966, 2567, 90);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		detection.getLearnedModel();
		// ExperimentResult [truePositives=377, trueNegatives=1966, falsePositives=2567, falseNegatives=90, executionTimeTraining=00:03:27.512,
		// executionTimeTesting=00:00:00.143, numberOfStates=43, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(0, 2882, 1623, 495);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		// ExperimentResult [truePositives=228, trueNegatives=1667, falsePositives=2838, falseNegatives=267, executionTimeTraining=00:02:36.574,
		// executionTimeTesting=00:00:00.005, numberOfStates=445, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(374, 2923, 1591, 112);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		// ExperimentResult [truePositives=486, trueNegatives=1295, falsePositives=3219, falseNegatives=0, executionTimeTraining=00:19:40.180,
		// executionTimeTesting=00:00:00.012, numberOfStates=3010, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 2831, 1646, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		// ExperimentResult [truePositives=523, trueNegatives=1148, falsePositives=3329, falseNegatives=0, executionTimeTraining=00:25:46.229,
		// executionTimeTesting=00:00:00.018, numberOfStates=3022, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		expected = new ExperimentResult(373, 2867, 1671, 89);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		// ExperimentResult [truePositives=462, trueNegatives=1153, falsePositives=3385, falseNegatives=0, executionTimeTraining=00:25:25.535,
		// executionTimeTesting=00:00:00.004, numberOfStates=2985, maxMemoryUsage=0(MB), minMemoryUsage=0(MB), avgMemoryUsage=0.0(MB)]
		assertEquals(expected, actual);

		logger.info("Finished Anoda Original BUTLA test.");
	}

}
