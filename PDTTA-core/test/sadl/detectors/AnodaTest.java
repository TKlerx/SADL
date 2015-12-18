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

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.TransitionsType;
import sadl.experiments.ExperimentResult;
import sadl.modellearner.ButlaPdtaLearner;

public class AnodaTest {

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
	}

}
