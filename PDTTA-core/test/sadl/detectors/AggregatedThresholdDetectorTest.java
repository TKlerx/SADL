package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.threshold.AggregatedThresholdDetector;
import sadl.experiments.ExperimentResult;
import sadl.modellearner.PdttaLearner;

public class AggregatedThresholdDetectorTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
				false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(467, 4340, 193, 0);
		ExperimentResult actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		assertEquals(expected, actual);

		expected = new ExperimentResult(79, 4288, 217, 416);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI()));
		assertEquals(expected, actual);

		expected = new ExperimentResult(469, 4311, 203, 17);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI()));
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 4259, 218, 0);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI()));
		assertEquals(expected, actual);

		expected = new ExperimentResult(377, 4335, 203, 85);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI()));
		assertEquals(expected, actual);
	}
}
