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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;

import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.MergeTest;
import sadl.constants.PTAOrdering;
import sadl.constants.TransitionsType;
import sadl.input.TimedInput;
import sadl.interfaces.AutomatonModel;
import sadl.models.pdta.PDTA;
import sadl.utils.IoUtils;

public class TrebaButlaLearnerTest {

	// @Test
	public void test() throws URISyntaxException {
		// TODO why do they give different results?
		final ButlaPdtaLearner butla = new ButlaPdtaLearner(100000000, 0.05, TransitionsType.Outgoing, 0.05, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.DontSplitEvents, KDEFormelVariant.OriginalKDE);
		final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, true, MergeTest.ALERGIA, 0.0, 0);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> input = IoUtils.readTrainTestFile(p);

		final PDTA pdta = butla.train(input.getKey());

		input = IoUtils.readTrainTestFile(p);
		final AutomatonModel pdfa = treba.train(input.getKey());

		assertEquals(pdta.getStateCount(), pdfa.getStateCount());
		assertEquals(pdta.getTransitionCount(), pdfa.getTransitionCount());
		assertEquals(pdfa, pdta.toPDFA());

	}

}
