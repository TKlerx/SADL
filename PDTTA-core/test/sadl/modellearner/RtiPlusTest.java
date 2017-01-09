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
package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.modellearner.rtiplus.analysis.FrequencyAnalysis;
import sadl.modellearner.rtiplus.analysis.QuantileAnalysis;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.utils.IoUtils;

public class RtiPlusTest {
	private static Logger logger = LoggerFactory.getLogger(RtiPlusTest.class);

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
	public void testDeterminism() throws URISyntaxException, IOException {
		final String travis = System.getenv("TRAVIS");
		if (travis != null && travis.equalsIgnoreCase("true")) {
			// This test fails in travis
			logger.info("Skipped testDeterminism because of travis.");
			return;
		}
		logger.info("Starting testDeterminism...");

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, 4, false, false, null);
			final ProbabilisticModel p1 = l1.train(ti1);
			for (int j = 0; j < 10; j++) {
				final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, 4, false, true, null);
				final ProbabilisticModel p2 = l2.train(ti2);
				assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
			}
		}
		logger.info("Finished testDeterminism.");
	}

	@Test
	public void testDeterminismBig() throws URISyntaxException, IOException {
		final String travis = System.getenv("TRAVIS");
		if (travis != null && travis.equalsIgnoreCase("true")) {
			// This test fails in travis
			logger.info("Skipped testDeterminismBig because of travis.");
			return;
		}
		logger.info("Starting testDeterminismBig...");

		for (int i = 1; i <= 5; i++) {
			final Pair<TimedInput, TimedInput> traintestSet = IoUtils
					.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type" + i + ".txt").toURI()));

			final TimedInput ti1 = traintestSet.getKey();
			ti1.decreaseSamples(0.01);
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new LikelihoodRatioTester(false), SplitPosition.MIDDLE, false,
					false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);

			final ProbabilisticModel p1 = l1.train(ti1);
			for (int j = 0; j < 10; j++) {
				final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new LikelihoodRatioTester(false), SplitPosition.MIDDLE,
						false, false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);
				final ProbabilisticModel p2 = l2.train(ti2);

				assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
			}
		}
		logger.info("Finished testDeterminismBig.");

	}

	@Test
	public void testDeterminismNaive() throws URISyntaxException, IOException {
		final String travis = System.getenv("TRAVIS");
		if (travis != null && travis.equalsIgnoreCase("true")) {
			// This test fails in travis
			logger.info("Skipped testDeterminismNaive because of travis.");
			return;
		}
		logger.info("Starting testDeterminismNaive...");

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new NaiveLikelihoodRatioTester(), SplitPosition.MIDDLE, false,
					false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);
			final ProbabilisticModel p1 = l1.train(ti1);

			final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new NaiveLikelihoodRatioTester(), SplitPosition.MIDDLE, false,
					false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);
			final ProbabilisticModel p2 = l2.train(ti2);

			assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
		}
		logger.info("Finished testDeterminismNaive.");

	}

	@Test
	public void testSerialization() throws URISyntaxException, IOException, ClassNotFoundException {
		final String travis = System.getenv("TRAVIS");
		if (travis != null && travis.equalsIgnoreCase("true")) {
			// This test fails in travis
			logger.info("Skipped testSerialization because of travis.");
			return;
		}
		logger.info("Starting testSerialization...");

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new LikelihoodRatioTester(false), SplitPosition.MIDDLE, false,
					false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);
			final ProbabilisticModel p = l.train(ti);

			final Path path = Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI());
			IoUtils.serialize(p, path);
			final ProbabilisticModel cP = (ProbabilisticModel) IoUtils.deserialize(path);

			assertEquals("PDRTAs for files " + i + " are not equal", p, cP);
		}
		logger.info("Finished testSerialization.");

	}

	@Test
	public void testCorrectness() throws URISyntaxException, IOException, ClassNotFoundException {
		final String travis = System.getenv("TRAVIS");
		if (travis != null && travis.equalsIgnoreCase("true")) {
			// This test fails in travis
			logger.info("Skipped testCorrectness because of travis.");
			return;
		}
		logger.info("Starting testCorrectness...");

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, new QuantileAnalysis(4), new LikelihoodRatioTester(false), SplitPosition.MIDDLE, false,
					false, new FrequencyAnalysis(10, 0.2), false, true, 0.01, "AAO", null);
			final ProbabilisticModel pdrta = l.train(ti);

			// Deserialize
			final ProbabilisticModel p = (ProbabilisticModel) IoUtils.deserialize(Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI()));

			assertEquals("PDRTAs for files " + i + " are not equal", pdrta, p);
		}
		logger.info("Finished testCorrectness.");

	}

}
