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
	public void testAnoda() throws IOException, URISyntaxException {
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(1000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalButlaVariableBandwidth);
		final AnodaDetector anoda = new AnodaDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);

		final AnomalyDetection detection = new AnomalyDetection(anoda, learner);
		ExperimentResult expected = new ExperimentResult(467, 4428, 105, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(51, 4386, 119, 444);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(466, 4391, 123, 20);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 4335, 142, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(293, 4420, 118, 169);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
	}

}
