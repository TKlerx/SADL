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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import gnu.trove.list.array.TIntArrayList;
import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.TauPtaLearner;
import sadl.models.TauPTA;
import sadl.structure.Transition;
import sadl.utils.IoUtils;

public class ModelAnomalyExportTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void restoresBaseSymbolsInBothSplitsWithoutChangingDelaysOrLabels() throws Exception {
		final Random random = new Random(29);
		final StringBuilder examples = new StringBuilder();
		for (int i = 0; i < 600; i++) {
			examples.append("(a,").append((i % 2 == 0 ? 40000 : 200000) + random.nextInt(10000))
					.append(") (v.7,50000):0\n");
		}
		final TimedInput input = TimedInput.parse(new StringReader(examples.toString()));
		final ButlaPdtaLearner butla = new ButlaPdtaLearner(10000, EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
		final var split = butla.splitEventsInTimedSequences(input);
		final TimedWord shortDelay = split.getKey().get(0);
		final TimedWord longDelay = split.getKey().get(1);
		assertEquals("a.1", shortDelay.getSymbol(0));
		assertEquals("a.2", longDelay.getSymbol(0));
		assertEquals("v.7.1", shortDelay.getSymbol(1));

		final TimedWord anomaly = new TimedWord(List.of("a.1", "v.7.1", "unknown.9"),
				new TIntArrayList(new int[] { longDelay.getTimeValue(0), 50000, 12 }), ClassLabel.ANOMALY);
		final TimedInput training = new TimedInput(List.of(shortDelay));
		final TimedInput evaluation = new TimedInput(List.of(longDelay, anomaly));
		final Path file = temporary.newFile().toPath();
		ModelAnomalyExport.write(file, training, evaluation, ModelAnomalyExport.baseSymbols(split.getValue()));

		final var result = IoUtils.readTrainTestFile(file);
		assertEquals(1, result.getKey().size());
		assertEquals(2, result.getValue().size());
		assertEquals("(a," + shortDelay.getTimeValue(0) + ") (v.7,50000):0", result.getKey().get(0).toString(true));
		assertEquals("(a," + longDelay.getTimeValue(0) + ") (v.7,50000):0", result.getValue().get(0).toString(true));
		assertEquals("(a," + longDelay.getTimeValue(0) + ") (v.7,50000) (unknown.9,12):1", result.getValue().get(1).toString(true));
		assertTrue(result.getValue().get(1).isAnomaly());
		assertEquals("a.1", shortDelay.getSymbol(0));

		ModelAnomalyExport.write(file, training, evaluation, Map.of());
		assertEquals(training, IoUtils.readTrainTestFile(file).getKey());
	}
	@Test
	public void typeOneSubstitutionsRemainDifferentAfterRestoringSymbols() throws Exception {
		final TimedInput input = TimedInput.parse(new StringReader("(a.1,10) (a.1,20) (b.1,30):0\n(a.2,10) (a.2,20) (b.1,30):0"));
		final TauPTA normal = new TauPtaLearner().train(input);
		final TauPTA model = SerializationUtils.clone(normal);
		model.setRandom(new Random(29) {
			@Override
			public int nextInt(int bound) {
				return 0;
			}
		});
		final Map<String, String> baseSymbols = Map.of("a.1", "a", "a.2", "a", "b.1", "b");
		model.makeAbnormal(AnomalyInsertionType.TYPE_ONE, baseSymbols);

		int substitutions = 0;
		for (final Transition transition : model.getAllTransitions()) {
			if (transition.isAbnormal()) {
				final Transition original = normal.getOutTransitions(transition.getFromState(), false).stream()
						.filter(candidate -> candidate.getToState() == transition.getToState()).findFirst().get();
				assertNotEquals("Type 1 must change the exported symbol", baseSymbols.get(original.getSymbol()),
						baseSymbols.get(transition.getSymbol()));
				substitutions++;
			}
		}
		assertTrue(substitutions > 0);
	}


	@Test
	public void typeOneRejectsEventsAllowedUnderAnotherAliasOfTheExportedPrefix() throws Exception {
		final TimedInput training = TimedInput.parse(new StringReader(
				"(a.1,10) (x.1,20):0\n(a.2,30) (y.1,20):0\n(x.1,20):0\n(y.1,20):0"));
		final Map<String, String> baseSymbols = Map.of("a.1", "a", "a.2", "a", "x.1", "x", "y.1", "y");
		final TauPTA model = new TauPtaLearner().train(training);
		model.setRandom(new Random(29) {
			@Override
			public int nextInt(int bound) {
				return bound == 3 ? 2 : 0;
			}
		});
		model.makeAbnormal(AnomalyInsertionType.TYPE_ONE, baseSymbols);
		model.setRandom(new Random(2));

		final List<TimedWord> anomalous = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			final TimedWord word = model.sampleSequence();
			if (word.isAnomaly()) {
				anomalous.add(word);
			}
		}
		assertTrue("Expected at least one Type 1 sample", !anomalous.isEmpty());
		final Path file = temporary.newFile().toPath();
		ModelAnomalyExport.write(file, training, new TimedInput(anomalous), baseSymbols);
		final var export = IoUtils.readTrainTestFile(file);
		assertEquals("a", export.getKey().get(1).getSymbol(0));
		assertEquals("y", export.getKey().get(1).getSymbol(1));
		for (final TimedWord word : export.getValue()) {
			assertEquals(2, word.length());
			assertEquals("a", word.getSymbol(0));
			assertEquals("Type 1 must not replace x with y because the exported prefix a already allows y", "a", word.getSymbol(1));
			assertTrue(word.isAnomaly());
		}
	}

	@Test
	public void typeOneRequiresARealBaseSymbolSubstitution() throws Exception {
		final TimedInput input = TimedInput.parse(new StringReader("(a.1,10) (a.1,20):0\n(a.2,10) (a.2,20):0"));
		final TauPTA model = new TauPtaLearner().train(input);
		try {
			model.makeAbnormal(AnomalyInsertionType.TYPE_ONE, Map.of("a.1", "a", "a.2", "a"));
			fail("Expected a diagnostic when every alias is for the same event");
		} catch (final IllegalArgumentException error) {
			assertTrue(error.getMessage(), error.getMessage().contains("Type 1 requires at least two base event symbols"));
		}
		assertEquals(AnomalyInsertionType.NONE, model.getAnomalyType());
	}

	@Test
	public void typeTwoKeepsAliasesOfAnExportedPathInOneClass() throws Exception {
		final String input = "(a.1,10):0\n".repeat(10) + "(a.2,20):0\n" + "(b.1,30):0\n".repeat(5);
		final TauPTA normal = new TauPtaLearner().train(TimedInput.parse(new StringReader(input)));
		final TauPTA anomaly = SerializationUtils.clone(normal);
		normal.setRandom(new Random(29));
		anomaly.setRandom(new Random(29));
		final Map<String, String> baseSymbols = Map.of("a.1", "a", "a.2", "a", "b.1", "b");
		anomaly.makeAbnormal(AnomalyInsertionType.TYPE_TWO, baseSymbols);
		anomaly.removeAbnormalSequences(normal);

		final List<TimedWord> training = new ArrayList<>();
		final List<TimedWord> evaluation = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			training.add(normal.sampleSequence());
			evaluation.add(anomaly.sampleSequence());
		}
		final Path file = temporary.newFile().toPath();
		ModelAnomalyExport.write(file, new TimedInput(training), new TimedInput(evaluation), baseSymbols);
		final var result = IoUtils.readTrainTestFile(file);
		assertEquals(100, result.getKey().size());
		assertEquals(100, result.getValue().size());
		for (final TimedWord word : result.getKey()) {
			assertEquals("a", word.getSymbol(0));
			assertEquals(ClassLabel.NORMAL, word.getLabel());
		}
		for (final TimedWord word : result.getValue()) {
			assertEquals("b", word.getSymbol(0));
			assertEquals(ClassLabel.ANOMALY, word.getLabel());
		}
	}

	@Test
	public void typeTwoRejectsOneExportedPathBeforeChangingModel() throws Exception {
		final TimedInput input = TimedInput.parse(new StringReader("(a.1,10):0\n(a.2,20):0"));
		final TauPTA model = new TauPtaLearner().train(input);
		try {
			model.makeAbnormal(AnomalyInsertionType.TYPE_TWO, Map.of("a.1", "a", "a.2", "a"));
			fail("Expected a diagnostic when aliases collapse to one exported path");
		} catch (final IllegalArgumentException error) {
			assertTrue(error.getMessage(), error.getMessage().contains("Type 2 requires at least two distinct exported paths"));
		}
		assertEquals(AnomalyInsertionType.NONE, model.getAnomalyType());
	}

}
