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
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.models.PDTTA;
import sadl.models.pta.Event;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;

public class PdttaWithPreprocessingTest {

	@Test
	public void test() throws URISyntaxException, IOException {
		final PdttaLearner learner = new PdttaLearner(new AlergiaRedBlue(0.05, true));
		// final AggregatedThresholdDetector detector = new AggregatedThresholdDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, -5, -8,
		// false);
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));

		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(p);

		final ButlaPdtaLearner butla = new ButlaPdtaLearner(20000, 0.001, EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
		// final ButlaPdtaLearner butla = new ButlaPdtaLearner(EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);

		final Pair<TimedInput, Map<String, Event>> pair = butla.splitEventsInTimedSequences(trainTest.getKey());
		final TimedInput splittedTrainSet = pair.getKey();
		final Map<String, Event> eventMapping = pair.getValue();
		final PDTTA model = learner.train(splittedTrainSet);
		detector.setModel(model);
		final AnomalyDetection detection = new AnomalyDetection(detector, model);
		final TimedInput testSet = butla.getSplitInputForMapping(trainTest.getValue(), eventMapping);
		final ExperimentResult result = detection.test(testSet);
		final ExperimentResult expected = new ExperimentResult(467, 4421, 112, 0);
		assertEquals(expected, result);
	}

}
