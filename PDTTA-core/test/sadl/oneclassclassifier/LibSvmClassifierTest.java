package sadl.oneclassclassifier;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.math3.util.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.PdttaLearner;
import sadl.utils.IoUtils;

public class LibSvmClassifierTest {

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
	public void test() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final LibSvmClassifier classifier = new LibSvmClassifier(1, 0.2, 0.1, 1, 0.001, 3, ScalingMethod.NONE);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

}
