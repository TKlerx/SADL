/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.oneclassclassifier;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
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
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.clustering.DbScanClassifier;
import sadl.oneclassclassifier.clustering.GMeansClassifier;
import sadl.oneclassclassifier.clustering.KMeansClassifier;
import sadl.oneclassclassifier.clustering.XMeansClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class NumericClassifierTest {

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
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final LibSvmClassifier classifier = new LibSvmClassifier(1, 0.2, 0.1, 1, 0.001, 3, ScalingMethod.NONE);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(4072, 420, 47, 461);
		assertEquals(expected, actual);
	}

	@Test
	public void testDBScanClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new DbScanClassifier(0.05, 5, DistanceMethod.EUCLIDIAN, ScalingMethod.NORMALIZE);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(4356, 467, 0, 177);
		assertEquals(expected, actual);
	}

	// @Test
	public void testSomClassifier() throws URISyntaxException, IOException {
		// The SOM method is crappy and may fail randomly because it uses an internal random object.
		// final String osName = System.getProperty("os.name");
		// if (osName.toLowerCase().contains("linux")) {
		// final PdttaLearner learner = new PdttaLearner(0.05, false);
		// final FeatureCreator featureCreator = new UberFeatureCreator();
		// final NumericClassifier classifier = new SomClassifier(ScalingMethod.NORMALIZE, 10, 10, 0.1);
		// final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		// final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		// final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		// final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		// final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		// final ExperimentResult expected = new ExperimentResult(0, 4533, 0, 467);
		// assertEquals(expected, actual);
		// } else {
		// System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		// }
	}

	@Test
	public void testKMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new KMeansClassifier(ScalingMethod.NORMALIZE, 10, 0.05, 0, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(445, 467, 0, 4088);
		assertEquals(expected, actual);
	}

	@Test
	public void testGMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new GMeansClassifier(ScalingMethod.NORMALIZE, 0.05, 0, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(4239, 467, 0, 294);
		assertEquals(expected, actual);
	}

	@Test
	public void testXMeansClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new XMeansClassifier(ScalingMethod.NORMALIZE, 0.05, 0, DistanceMethod.EUCLIDIAN);
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(4311, 467, 0, 222);
		assertEquals(expected, actual);
	}

	@Test
	public void testClusteredClassifier() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		final FeatureCreator featureCreator = new UberFeatureCreator();
		final NumericClassifier classifier = new ClusteredClassifier(ScalingMethod.NORMALIZE,
				new GMeans(new HamerlyKMeans(new EuclideanDistance(), SeedSelection.KPP, MasterSeed.nextRandom())));
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(0, 467, 0, 4533);
		assertEquals(expected, actual);
	}
}
