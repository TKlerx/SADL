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
package sadl.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Random;

import org.junit.Test;

import sadl.constants.AnomalyInsertionType;

public class RandomTypeTwoTest {
	@Test
	public void replacesCommonPathsWithTheRarestCompletePathInEachSplit() throws Exception {
		assertReplacement("(a,10) (b,20):0", "(a,30) (c,40):0");
		assertReplacement("(a,30) (c,40):0", "(a,10) (b,20):0");
	}

	@Test
	public void skipsWhenNoStrictlyRarerPathExists() throws Exception {
		for (final String data : new String[] { "(a,10):0\n(a,20):0", "(a,10):0\n(b,20):0" }) {
			final TimedInput input = TimedInput.parse(new StringReader(data));
			input.r = new Random(35);
			final TimedInput result = input.insertRandomAnomalies(AnomalyInsertionType.TYPE_TWO, 1.0);
			assertEquals(input.toString(), result.toString());
			for (final TimedWord word : result) {
				assertFalse(word.isAnomaly());
			}
		}
	}

	private static void assertReplacement(String common, String rare) throws Exception {
		final TimedInput input = TimedInput.parse(new StringReader((common + "\n").repeat(10) + rare));
		input.r = new Random(35);
		final TimedInput result = input.insertRandomAnomalies(AnomalyInsertionType.TYPE_TWO, 1.0);
		for (int i = 0; i < 10; i++) {
			assertEquals(rare.replace(":0", ":1"), result.get(i).toString(true));
			assertTrue(result.get(i).isAnomaly());
			assertEquals(common, input.get(i).toString(true));
		}
		assertEquals(rare, result.get(10).toString(true));
	}
}
