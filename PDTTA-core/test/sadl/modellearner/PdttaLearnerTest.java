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
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

public class PdttaLearnerTest {

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
	public void oldNewLeanerTest() throws IOException, URISyntaxException {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			for (int i = 1; i <= 5; i++) {
				final Pair<TimedInput, TimedInput> trainTest = IoUtils
						.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type" + i + ".txt").toURI()));
				Settings.setDebug(false);
				final TimedInput ti1 = trainTest.getKey();
				final TimedInput ti2 = SerializationUtils.clone(ti1);
				final PdttaLearner l1 = new PdttaLearner(new TrebaPdfaLearner(0.05, false));
				final ProbabilisticModel p1 = l1.train(ti1);
				MasterSeed.reset();
				final PdttaLeanerOld l2 = new PdttaLeanerOld(0.05, false);
				final ProbabilisticModel p2 = l2.train(ti2);
				assertEquals("PDTTAs for file " + i + " are not equal", p2, p1);
				// TODO Also compare with loaded model from file
			}
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}
}
