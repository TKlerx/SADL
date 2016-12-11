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

import org.junit.Before;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.IntervalCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.TransitionsType;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.detectors.featureCreators.MinimalFeatureCreator;
import sadl.detectors.threshold.AggregatedThresholdDetector;
import sadl.experiments.ExperimentResult;
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.MasterSeed;

@SuppressWarnings("deprecation")
public class ThresholdDetectorTest {
	@Before
	public void setUp() throws Exception {
		MasterSeed.reset();
	}

	@Test
	public void testAggregatedThresholdDetector() throws IOException, URISyntaxException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, Math.exp(-5),
				Math.exp(-8), false);
		final AnomalyDetector detector2 = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new MinimalFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5), Math.exp(-8)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		final AnomalyDetection detection2 = new AnomalyDetection(detector2, learner);
		ExperimentResult expected = new ExperimentResult(4436, 467, 0, 97);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		ExperimentResult actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(4345, 72, 423, 160);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(4400, 454, 32, 114);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(4346, 523, 0, 131);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(4385, 383, 79, 153);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);
	}

	@Test
	public void testSingleThresholdDetectorPdtta() throws IOException, URISyntaxException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(4437, 467, 0, 96);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4349, 72, 423, 156);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4401, 453, 33, 113);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4348, 523, 0, 129);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(4386, 383, 79, 152);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
	}

	@Test
	public void testSingleThresholdDetectorButla() throws IOException, URISyntaxException {
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(20000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.extendInterval);
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(1388, 467, 0, 3145);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1341, 201, 294, 3164);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1423, 322, 164, 3091);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1331, 523, 0, 3146);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(1419, 251, 211, 3119);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
	}

}
