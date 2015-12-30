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
import sadl.utils.Settings;

public class AlergiaTest {

	@Test
	public void testBig() throws URISyntaxException, IOException {
		Settings.setDebug(false);
		final Alergia alergia = new Alergia(0.05);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 4519, 14, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
	}

	@Test
	public void testTrebaJava() throws URISyntaxException, IOException {
		Settings.setDebug(false);
		final Alergia alergia = new Alergia(0.05);
		final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, true);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final Pair<TimedInput, TimedInput> trainTest2 = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final PDFA pdfa2 = treba.train(trainTest.getKey());
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final AnomalyDetection detection2 = new AnomalyDetection(detector, pdfa2);
		final ExperimentResult expected = new ExperimentResult(467, 4519, 14, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		final ExperimentResult actual2 = detection2.test(trainTest2.getValue());
		assertEquals(expected, actual);
		assertEquals(actual2, actual);
	}

	@Test
	public void testPaper0() throws URISyntaxException, IOException {
		Settings.setDebug(false);
		final Alergia alergia = new Alergia(0.8);
		final TimedInput train = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
		final PDFA pdfa = alergia.train(train);
		assertEquals(2, pdfa.getStateCount());
	}
}
