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
package sadl.detectors;

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

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.detectors.featureCreators.UberFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class AggSublistsTest {

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
	public void test() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));

		final MinimalFeatureCreator featureCreator = new MinimalFeatureCreator();
		final ThresholdClassifier classifier = new ThresholdClassifier(Math.exp(-5), Math.exp(-8));
		final VectorDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, false);
		final VectorDetector detector2 = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator, classifier, true);

		final SmallFeatureCreator featureCreator3 = new SmallFeatureCreator();
		final ThresholdClassifier classifier3 = new ThresholdClassifier(Math.exp(-5), Math.exp(-8), Math.exp(-5), Math.exp(-8));
		final VectorDetector detector3 = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator3, classifier3, true);

		final UberFeatureCreator featureCreator4 = new UberFeatureCreator();
		final LibSvmClassifier classifier4 = new LibSvmClassifier(1, 0.2, 0.1, 1, 0.001, 3, ScalingMethod.NONE);
		final VectorDetector detector4 = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, featureCreator4, classifier4, true);

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final AnomalyDetection detection2 = new AnomalyDetection(detector2, learner);
		final AnomalyDetection detection3 = new AnomalyDetection(detector3, learner);
		final AnomalyDetection detection4 = new AnomalyDetection(detector4, learner);

		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> inputSets = IoUtils.readTrainTestFile(p);
		inputSets.getKey().decreaseSamples(0.2);
		final ExperimentResult actual = detection.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected = new ExperimentResult(467, 4174, 359, 0);
		assertEquals(expected, actual);

		inputSets = IoUtils.readTrainTestFile(p);
		inputSets.getKey().decreaseSamples(0.2);
		final ExperimentResult actual2 = detection2.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected2 = new ExperimentResult(4, 4164, 369, 463);
		assertEquals(expected2, actual2);

		inputSets = IoUtils.readTrainTestFile(p);
		inputSets.getKey().decreaseSamples(0.2);
		final ExperimentResult actual3 = detection3.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected3 = new ExperimentResult(48, 780, 3753, 419);
		assertEquals(expected3, actual3);

		inputSets = IoUtils.readTrainTestFile(p);
		inputSets.getKey().decreaseSamples(0.2);
		final ExperimentResult actual4 = detection4.trainTest(inputSets.getKey(), inputSets.getValue());
		final ExperimentResult expected4 = new ExperimentResult(134, 0, 4533, 333);
		assertEquals(expected4, actual4);
	}
}
