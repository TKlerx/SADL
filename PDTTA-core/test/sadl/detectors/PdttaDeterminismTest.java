package sadl.detectors;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

public class PdttaDeterminismTest {

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
	public void svmDeterminismTest() throws IOException, URISyntaxException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			double fMeasure = -1;
			final boolean firstRun = true;
			for (int i = 1; i <= 10; i++) {
				final Pair<TimedInput, TimedInput> trainTest = IoUtils
						.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt")
								.toURI()));
				Settings.setDebug(false);
				final PdttaLearner learner = new PdttaLearner(0.05, false);
				final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new UberFeatureCreator(),
						new LibSvmClassifier(1, 0.2, 0.1, 1, 0.001, 3, ScalingMethod.NORMALIZE));

				final AnomalyDetection detection = new AnomalyDetection(detector, learner);

				final ExperimentResult result = detection.trainTest(trainTest.getKey(), trainTest.getValue());

				if (firstRun) {
					fMeasure = result.getFMeasure();
				} else {
					if (!Precision.equals(fMeasure, result.getFMeasure())) {
						fail("Failed for run " + i + " because in previous runs fMeasure=" + fMeasure + "; now fMeasure=" + result.getFMeasure());
					}
				}

			}
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void thresholdDeterminismTest() throws IOException, URISyntaxException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			double fMeasure = -1;
			final boolean firstRun = true;
			for (int i = 1; i <= 10; i++) {
				final Pair<TimedInput, TimedInput> trainTest = IoUtils
						.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
				Settings.setDebug(false);
				final PdttaLearner learner = new PdttaLearner(0.05, false);

				final SmallFeatureCreator featureCreator = new SmallFeatureCreator();
				final ThresholdClassifier classifier = new ThresholdClassifier(Math.exp(-5), Math.exp(-8), 0.01, 0.001);

				final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier);


				final AnomalyDetection detection = new AnomalyDetection(detector, learner);

				final ExperimentResult result = detection.trainTest(trainTest.getKey(), trainTest.getValue());

				if (firstRun) {
					fMeasure = result.getFMeasure();
				} else {
					if (!Precision.equals(fMeasure, result.getFMeasure())) {
						fail("Failed for run " + i + " because in previous runs fMeasure=" + fMeasure + "; now fMeasure=" + result.getFMeasure());
					}
				}

			}
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}
}
