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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.modellearner.TauPtaLearner;

public class ModelBasedTypeTwoTest {
	@Test
	public void leavesNormalPathWhenModelHasOnlyTwoPaths() throws Exception {
		final TauPTA normal = train("(a,10):0", "(a,10):0", "(b,20):0");
		final TauPTA anomaly = typeTwoModel(normal);
		anomaly.removeAbnormalSequences(normal);

		assertSample(normal.sampleSequence(), false, "a");
		assertSample(anomaly.sampleSequence(), true, "b");
	}

	@Test
	public void anomalyCannotStopAtUnselectedSharedPrefix() throws Exception {
		final List<String> words = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			words.add("(a,10):0");
		}
		for (int i = 0; i < 20; i++) {
			words.add("(a,10) (b" + i + ",20):0");
		}
		final TauPTA normal = train(words.toArray(new String[0]));
		final TauPTA anomaly = typeTwoModel(normal);
		anomaly.removeAbnormalSequences(normal);

		for (int i = 0; i < 100; i++) {
			final TimedWord sample = anomaly.sampleSequence();
			assertTrue(sample.isAnomaly());
			assertEquals("The normal prefix must not be emitted as a complete anomaly", 2, sample.length());
			assertEquals("a", sample.getSymbol(0));
			assertTrue(sample.getSymbol(1).matches("b([0-9]|1[0-9])"));
			assertSample(normal.sampleSequence(), false, "a");
		}
	}

	@Test
	public void rejectsOnePathBeforeChangingModel() throws Exception {
		final TauPTA model = train("(a,10):0");
		try {
			model.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
			fail("Expected a diagnostic for a one-path Type 2 model");
		} catch (final IllegalArgumentException error) {
			assertTrue(error.getMessage(), error.getMessage().contains("Type 2 requires at least two complete paths"));
		}
		assertEquals(AnomalyInsertionType.NONE, model.getAnomalyType());
		assertSample(model.sampleSequence(), false, "a");
	}

	static void assertSmallFixturePartition(TauPTA normal, TauPTA anomaly, TauPTA legacyAnomaly) {
		assertEquals(normal.getAllSequences(), legacyAnomaly.getAllSequences());
		assertTrue(legacyAnomaly.sampleSequence().isAnomaly());
		anomaly.removeAbnormalSequences(normal);
		assertEquals(1, normal.getAllSequences().size());
		final List<String> retainedPath = normal.getAllSequences().iterator().next().getEvents();
		for (int i = 0; i < 20; i++) {
			final TimedWord sample = anomaly.sampleSequence();
			assertTrue(sample.isAnomaly());
			final List<String> symbols = new ArrayList<>();
			for (int j = 0; j < sample.length(); j++) {
				symbols.add(sample.getSymbol(j));
			}
			assertTrue(!retainedPath.equals(symbols));
		}
	}

	private static TauPTA train(String... words) throws Exception {
		return new TauPtaLearner().train(TimedInput.parse(new StringReader(String.join("\n", words))));
	}

	private static TauPTA typeTwoModel(TauPTA normal) {
		final TauPTA anomaly = SerializationUtils.clone(normal);
		anomaly.setRandom(new Random(37));
		anomaly.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
		return anomaly;
	}

	private static void assertSample(TimedWord sample, boolean abnormal, String symbol) {
		assertEquals(abnormal, sample.isAnomaly());
		assertEquals(1, sample.length());
		assertEquals(symbol, sample.getSymbol(0));
	}
}
