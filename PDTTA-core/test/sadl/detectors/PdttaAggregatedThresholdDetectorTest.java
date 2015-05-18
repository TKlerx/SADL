package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

import sadl.anomalydetecion.PdttaAnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.threshold.PdttaAggregatedThresholdDetector;
import sadl.experiments.PdttaExperimentResult;
import sadl.modellearner.PdttaLearner;

public class PdttaAggregatedThresholdDetectorTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final PdttaAggregatedThresholdDetector pdttaDetector = new PdttaAggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY,-5,-8);
		final PdttaAnomalyDetection detection = new PdttaAnomalyDetection(pdttaDetector, learner);
		PdttaExperimentResult expected = new PdttaExperimentResult(467, 4340, 193, 0);
		PdttaExperimentResult actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		assertEquals(expected, actual);

		expected = new PdttaExperimentResult(79, 4288, 217, 416);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI()));
		assertEquals(expected, actual);

		expected = new PdttaExperimentResult(469, 4311, 203, 17);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI()));
		assertEquals(expected, actual);

		expected = new PdttaExperimentResult(523, 4259, 218, 0);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI()));
		assertEquals(expected, actual);

		expected = new PdttaExperimentResult(377, 4335, 203, 85);
		actual = detection.trainTest(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI()));
		assertEquals(expected, actual);
	}
}
