package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.models.PDTTA;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;

public class PdttaWithPreprocessingTest {

	@Test
	public void test() throws URISyntaxException, IOException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			final PdttaLearner learner = new PdttaLearner(0.05, false);
			// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
			// false);
			final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
					new ThresholdClassifier(Math.exp(-5)));

			final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
			final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);

			final ButlaPdtaLearner butla = new ButlaPdtaLearner(10000, 0.05, null, 0.05, 0.05, null, EventsCreationStrategy.SplitEvents,
					KDEFormelVariant.OriginalButlaVariableBandwidth);
			final TimedInput splittedTrainSet = butla.splitEventsInTimedSequences(trainTest.getFirst());
			final PDTTA model = learner.train(splittedTrainSet);
			detector.setModel(model);
			final AnomalyDetection detection = new AnomalyDetection(detector, model);
			final TimedInput testSet = trainTest.getValue();
			// TODO method to apply the event preprocessing to a test set with the splits from the train set
			final ExperimentResult result = detection.test(testSet);
			final ExperimentResult expected = new ExperimentResult(0, 0, 0, 0);
			assertEquals(expected, result);
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

}
