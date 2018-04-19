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

import org.apache.commons.lang3.SerializationUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.modellearner.TauPtaLearner;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTestSmallV2 {
	@BeforeClass
	public static void resetSeed() {
		MasterSeed.reset();
	}
	@Test
	public void testTauPTATimedInputAbnormal() throws IOException, URISyntaxException, ClassNotFoundException {
		Path p = Paths.get(TauPtaTestSmallV2.class.getResource("/taupta/small/rti_small.txt").toURI());
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		for (final AnomalyInsertionType type : AnomalyInsertionType.values()) {
			if (type != AnomalyInsertionType.NONE && type != AnomalyInsertionType.ALL) {
				final TauPTA anomaly1 = SerializationUtils.clone(pta);
				anomaly1.makeAbnormal(type);
				p = Paths.get(this.getClass().getResource("/taupta/small/pta_abnormal_" + type.getTypeIndex() + ".ser").toURI());
				final TauPTA des = (TauPTA) IoUtils.deserialize(p);
				assertEquals("Test failed for anomaly type " + type.getTypeIndex(), anomaly1, des);
				System.out.println("Test " + type + " passed.");
			}
		}
	}

}
