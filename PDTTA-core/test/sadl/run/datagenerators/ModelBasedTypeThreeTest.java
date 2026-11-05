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
package sadl.run.datagenerators;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.TauPtaLearner;
import sadl.models.PDFA;
import sadl.models.TauPTA;

public class ModelBasedTypeThreeTest {
	@Test
	public void exportsMatchingUntimedPathsWithDifferentDelays() throws Exception {
		final String shortPath = "(9.1,40) (8,70) (11,50):0\n";
		final String longPath = "(9.2,200) (8,70) (10,50) (11,50):0\n";
		final String training = String.join("", Collections.nCopies(100, shortPath))
				+ String.join("", Collections.nCopies(300, longPath));
		final TimedInput input = TimedInput.parse(new StringReader(training));
		final TauPTA normal = new TauPtaLearner().train(input);
		normal.setRandom(new Random(37));
		final TauPTA abnormal = SerializationUtils.clone(normal);
		abnormal.makeAbnormal(AnomalyInsertionType.TYPE_THREE);

		final List<TimedWord> samples = SmacDataGenerator.sampleModelTestSet(normal, abnormal, 400, 0.5, new Random(37));
		final StringBuilder export = new StringBuilder();
		new TimedInput(samples).toFile(export, true);
		final TimedInput reloaded = TimedInput.parse(new StringReader(export.toString()));
		assertEquals(400, reloaded.size());
		final Map<List<String>, List<TimedWord>> controls = new HashMap<>();
		final Map<List<String>, List<TimedWord>> anomalies = new HashMap<>();
		for (final TimedWord word : reloaded) {
			final List<String> symbols = new ArrayList<>();
			for (int i = 0; i < word.length(); i++) {
				symbols.add(word.getSymbol(i));
			}
			(word.isAnomaly() ? anomalies : controls).computeIfAbsent(symbols, key -> new ArrayList<>()).add(word);
		}
		assertEquals(2, controls.size());
		assertEquals(controls.keySet(), anomalies.keySet());
		final PDFA alergia = new AlergiaRedBlue(0.05).train(input);
		for (final Map.Entry<List<String>, List<TimedWord>> entry : controls.entrySet()) {
			final List<TimedWord> normalWords = entry.getValue();
			final List<TimedWord> abnormalWords = anomalies.get(entry.getKey());
			assertEquals("Class counts for " + entry.getKey(), normalWords.size(), abnormalWords.size());
			for (final TimedWord anomaly : abnormalWords) {
				final TimedWord control = normalWords.get(0);
				int changedDelays = 0;
				for (int i = 0; i < control.length(); i++) {
					if (control.getTimeValue(i) != anomaly.getTimeValue(i)) {
						changedDelays++;
					}
				}
				assertEquals("Expected one changed delay", 1, changedDelays);
				assertEquals(alergia.calculateProbabilities(control), alergia.calculateProbabilities(anomaly));
			}
		}
	}
}
