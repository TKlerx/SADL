package sadl.detectors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.TransitionsType;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.threshold.AggregatedThresholdDetector;
import sadl.experiments.ExperimentResult;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.ThresholdClassifier;

@SuppressWarnings("deprecation")
public class ThresholdDetectorTest {

	@Test
	public void testAggregatedThresholdDetector() throws IOException, URISyntaxException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			final PdttaLearner learner = new PdttaLearner(0.05, false);
			// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
			// false);
			final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, Math.exp(-5),
					Math.exp(-8), false);
			final AnomalyDetector detector2 = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new MinimalFeatureCreator(),
					new ThresholdClassifier(Math.exp(-5), Math.exp(-8)));

			final AnomalyDetection detection = new AnomalyDetection(detector, learner);
			final AnomalyDetection detection2 = new AnomalyDetection(detector2, learner);
			ExperimentResult expected = new ExperimentResult(467, 4340, 193, 0);
			Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
			ExperimentResult actual = detection.trainTest(p);
			ExperimentResult actual2 = detection2.trainTest(p);
			assertEquals(expected, actual);
			assertEquals(expected, actual2);

			expected = new ExperimentResult(79, 4288, 217, 416);
			p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
			actual = detection.trainTest(p);
			actual2 = detection2.trainTest(p);
			assertEquals(expected, actual);
			assertEquals(expected, actual2);

			expected = new ExperimentResult(469, 4311, 203, 17);
			p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
			actual = detection.trainTest(p);
			actual2 = detection2.trainTest(p);
			assertEquals(expected, actual);
			assertEquals(expected, actual2);

			expected = new ExperimentResult(523, 4259, 218, 0);
			p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
			actual = detection.trainTest(p);
			actual2 = detection2.trainTest(p);
			assertEquals(expected, actual);
			assertEquals(expected, actual2);

			expected = new ExperimentResult(377, 4335, 203, 85);
			p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
			actual = detection.trainTest(p);
			actual2 = detection2.trainTest(p);
			assertEquals(expected, actual);
			assertEquals(expected, actual2);
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testSingleThresholdDetectorPdtta() throws IOException, URISyntaxException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			final PdttaLearner learner = new PdttaLearner(0.05, false);
			// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
			// false);
			final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
					new ThresholdClassifier(Math.exp(-5)));

			final AnomalyDetection detection = new AnomalyDetection(detector, learner);
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
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testSingleThresholdDetectorButla() throws IOException, URISyntaxException {
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(1000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalButlaVariableBandwidth);
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
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
