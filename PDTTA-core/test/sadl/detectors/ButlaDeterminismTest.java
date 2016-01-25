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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.IntervalCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.TransitionsType;
import sadl.detectors.featureCreators.SmallFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

public class ButlaDeterminismTest {

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
	public void thresholdDeterminismTest() throws IOException, URISyntaxException {
		double fMeasure = -1;
		final boolean firstRun = true;
		for (int i = 1; i <= 10; i++) {
			final Pair<TimedInput, TimedInput> trainTest = IoUtils
					.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
			Settings.setDebug(false);
			final ButlaPdtaLearner learner = new ButlaPdtaLearner(10000, 0.05, TransitionsType.Incoming, 0.05, 0.05, PTAOrdering.BottomUp,
					EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE, IntervalCreationStrategy.OriginalButla);

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
	}
}
