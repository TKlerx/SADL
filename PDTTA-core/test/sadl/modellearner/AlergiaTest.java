package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.AnomalyDetector;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;

public class AlergiaTest {

	@Test
	public void test() throws URISyntaxException, IOException {
		final Alergia alergia = new Alergia(0.05);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 0, 4533, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
	}

}
