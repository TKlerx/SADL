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

import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.GMeans;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.linear.distancemetrics.EuclideanDistance;
import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.DistanceMethod;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.clustering.GMeansClassifier;
import sadl.oneclassclassifier.clustering.KMeansClassifier;
import sadl.oneclassclassifier.clustering.XMeansClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class LibSvmClassifierTest {

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
	public void testLibSvmClassifier() throws URISyntaxException, IOException {
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

	@Test
	public void testSomClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new SomClassifier(ScalingMethod.NORMALIZE, 10, 10);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// TODO correct experimentresult
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

	@Test
	public void testKMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new KMeansClassifier(ScalingMethod.NORMALIZE, 10, 0.05, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// TODO correct experimentresult
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

	@Test
	public void testGMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new GMeansClassifier(ScalingMethod.NORMALIZE, 0.05, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// TODO correct experimentresult
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

	@Test
	public void testXMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new XMeansClassifier(ScalingMethod.NORMALIZE, 0.05, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// TODO correct experimentresult
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

	@Test
	public void testClusteredClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(0.05, false);
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new ClusteredClassifier(ScalingMethod.NORMALIZE,
				new GMeans(new HamerlyKMeans(new EuclideanDistance(), SeedSelection.KPP, MasterSeed.nextRandom())));
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// TODO correct experimentresult
		final ExperimentResult expected = new ExperimentResult(420, 4055, 478, 47);
		assertEquals(expected, actual);
	}

}
