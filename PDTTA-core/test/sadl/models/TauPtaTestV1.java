/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.models;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.modellearner.TauPtaLearner;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTestV1 {
	// XXX if this test fails and V2 passes then this may be an issue with the reset method in this class. As long as V2 passes, everything should be OK

	static TimedInput trainingTimedSequences;

	@BeforeClass
	public static void setup() throws URISyntaxException, IOException {
		final Path p = Paths.get(TauPtaTestV1.class.getResource("/taupta/medium/rti_medium.txt").toURI());
		trainingTimedSequences = TimedInput.parseAlt(p, 1);
	}

	@Before
	public void reset() {
		MasterSeed.reset();
	}
	@Test
	public void testTauPTATimedInputNormal() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_normal.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal1() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_1.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_ONE);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal2() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_2.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal3() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_3.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_THREE);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal4() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_4.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FOUR);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal5() throws IOException, URISyntaxException, ClassNotFoundException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_5.ser").toURI());
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FIVE);
		final TauPTA saved = (TauPTA) IoUtils.deserialize(p);
		assertEquals("TauPTAs of type 5 are not equal", pta, saved);
	}

}
