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

import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner.DistributionCheckType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.OperationTesterType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.utils.IoUtils;

public class RtiPlusTest {

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

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE, "AAO",
					null);
			final ProbabilisticModel p1 = l1.train(ti1);
			for (int j = 0; j < 10; j++) {
				final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE,
						"AAO",
						null);
				final ProbabilisticModel p2 = l2.train(ti2);
				assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
			}
		}
	}

	// @Test
	public void testDeterminismBig() throws URISyntaxException, IOException {
		// TODO fix this test
		for (int i = 1; i <= 5; i++) {
			final Pair<TimedInput, TimedInput> traintestSet = IoUtils
					.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type" + i + ".txt").toURI()));

			final TimedInput ti1 = traintestSet.getKey();
			ti1.decreaseSamples(0.01);
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE, "AAO",
					null);

			final ProbabilisticModel p1 = l1.train(ti1);
			for (int j = 0; j < 10; j++) {
				final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE, "AAO",
						null);
				final ProbabilisticModel p2 = l2.train(ti2);

				assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
			}
		}
	}

	// @Test
	public void testDeterminismNaive() throws URISyntaxException, IOException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, "4", OperationTesterType.NAIVE_LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE,
					"AOO",
					"/home/fabian/sadl_rti_test/" + i + "/");
			final ProbabilisticModel p1 = l1.train(ti1);

			final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, "4", OperationTesterType.NAIVE_LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE,
					"AOO", null);
			final ProbabilisticModel p2 = l2.train(ti2);

			assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
		}
	}

	@Test
	public void testSerialization() throws URISyntaxException, IOException, ClassNotFoundException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE, "AOO",
					null);
			final ProbabilisticModel p = l.train(ti);

			final Path path = Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI());
			IoUtils.serialize(p, path);
			final ProbabilisticModel cP = (ProbabilisticModel) IoUtils.deserialize(path);

			assertEquals("PDRTAs for files " + i + " are not equal", p, cP);
		}
	}

	@Test
	public void testCorrectness() throws URISyntaxException, IOException, ClassNotFoundException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.STRICT, SplitPosition.MIDDLE, "AOO",
					null);
			final ProbabilisticModel pdrta = l.train(ti);

			// Deserialize
			final ProbabilisticModel p = (ProbabilisticModel) IoUtils.deserialize(Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI()));

			assertEquals("PDRTAs for files " + i + " are not equal", pdrta, p);
		}
	}

}
