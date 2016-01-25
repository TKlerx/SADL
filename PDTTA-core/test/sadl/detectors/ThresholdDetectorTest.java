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
		ExperimentResult expected = new ExperimentResult(467, 4436, 97, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		ExperimentResult actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(72, 4345, 160, 423);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(454, 4400, 114, 32);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(523, 4346, 131, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		actual2 = detection2.trainTest(p);
		assertEquals(expected, actual);
		assertEquals(expected, actual2);

		expected = new ExperimentResult(383, 4385, 153, 79);
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
		ExperimentResult expected = new ExperimentResult(467, 4437, 96, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(72, 4349, 156, 423);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(453, 4401, 113, 33);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 4348, 129, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(383, 4386, 152, 79);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
	}

	@Test
	public void testSingleThresholdDetectorButla() throws IOException, URISyntaxException {
		final ButlaPdtaLearner learner = new ButlaPdtaLearner(20000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.OriginalButla);
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final AnomalyDetection detection = new AnomalyDetection(detector, learner);
		ExperimentResult expected = new ExperimentResult(467, 0, 4533, 0);
		Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		ExperimentResult actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(495, 0, 4505, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type2.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(486, 0, 4514, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type3.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(523, 0, 4477, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type4.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);

		expected = new ExperimentResult(462, 0, 4538, 0);
		p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type5.txt").toURI());
		actual = detection.trainTest(p);
		assertEquals(expected, actual);
	}

}
