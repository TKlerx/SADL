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
package sadl.modellearner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

import com.beust.jcommander.JCommander;

import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.run.factories.learn.PdfaFactory;
import sadl.utils.IoUtils;

public class TrebaNativeParityTest {
    @Test
    public void keepsStatesBelowMinimumVisitsSeparate() throws Exception {
        assertEquals(2, trainTwoSingleEventTraces(3).getStateCount());
    }

    @Test
    public void mergesAtExactlyMinimumVisits() throws Exception {
        assertEquals(1, trainTwoSingleEventTraces(2).getStateCount());
    }

    @Test
    public void mergesAboveMinimumVisits() throws Exception {
        assertEquals(1, trainTwoSingleEventTraces(1).getStateCount());
    }

    private static PDFA trainTwoSingleEventTraces(int minimumVisits) throws Exception {
        final var input = TimedInput.parse(new StringReader("(a,1)\n(a,1)\n"));
        return new TrebaNativeParityLearner(0.05, false, minimumVisits).train(input);
    }

    @Test
    public void learnedModelIsImmutable() throws Exception {
        assertTrue(trainTwoSingleEventTraces(2).isImmutable());
    }

    @Test
    public void emptyTrainingInputHasZeroStoppingProbability() {
        final PDFA model = new TrebaNativeParityLearner(0.05, true, 0).train(new TimedInput(new String[0]));
        assertEquals(1, model.getStateCount());
        assertEquals(0.0, model.getFinalStateProbability(model.getStartState()), 0.0);
    }

    @Test
    public void acceptsStoppingFrequencyExactlyOnHoeffdingBound() {
        final var left = stateWithEightVisits(8, 0, 0, 0);
        final var right = stateWithEightVisits(4, 4, 0, 0);
        assertTrue(boundaryLearner().compatible(left, right));
    }

    @Test
    public void acceptsSharedEventFrequencyExactlyOnHoeffdingBound() {
        final var left = stateWithEightVisits(0, 6, 2, 0);
        final var right = stateWithEightVisits(0, 2, 6, 0);
        assertTrue(boundaryLearner().compatible(left, right));
    }

    @Test
    public void acceptsRightOnlyEventFrequencyExactlyOnHoeffdingBound() {
        final var left = stateWithEightVisits(4, 2, 2, 0);
        final var right = stateWithEightVisits(4, 0, 0, 4);
        assertTrue(boundaryLearner().compatible(left, right));
    }

    private static TrebaNativeParityLearner boundaryLearner() {
        // For eight visits per state, this alpha gives a Hoeffding bound of 0.5.
        return new TrebaNativeParityLearner(2.0 / Math.E, false, 0);
    }

    private static TrebaNativeParityLearner.State stateWithEightVisits(int stops, int... counts) {
        final var state = new TrebaNativeParityLearner.State();
        state.stops = stops;
        for (int symbol = 0; symbol < counts.length; symbol++) {
            if (counts[symbol] > 0) {
                state.arcs.put(symbol, new TrebaNativeParityLearner.Arc(new TrebaNativeParityLearner.State(), counts[symbol]));
            }
        }
        state.updateTotal();
        assertEquals(8, state.total);
        return state;
    }

    @Test
    public void matchesFrozenNativeBenchmark() throws Exception {
        final var split = IoUtils.readTrainTestFile(Path.of(getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
        final var factory = new PdfaFactory();
        new JCommander(factory).parse("-mergeMethod", "TREBA_NATIVE_PARITY", "-mergeT0", "0");
        final PDFA model = (PDFA) factory.create().train(split.getLeft());
        final var reference = Files.readAllLines(referencePath("native-benchmark.tsv"));
        assertEquals(Integer.parseInt(reference.getFirst().split("\\t")[1]), model.getStateCount());
        final var stateIds = new HashMap<Integer, Integer>();
        final var mappedStates = new HashSet<Integer>();
        stateIds.put(0, 0);
        mappedStates.add(0);
        final var pending = new ArrayList<String[]>();
        for (String line : reference) {
            final String[] row = line.split("\\t");
            if ("transition".equals(row[0])) {
                pending.add(row);
            }
        }
        assertEquals(pending.size(), model.getTransitions().size());
        while (!pending.isEmpty()) {
            final int previousSize = pending.size();
            pending.removeIf(row -> {
                final Integer from = stateIds.get(Integer.parseInt(row[1]));
                if (from == null) {
                    return false;
                }
                final var transition = model.getTransition(from, row[2]);
                assertNotNull(String.join(" ", row), transition);
                assertProbability(row[4], transition.getProbability());
                final int to = transition.getToState();
                final Integer existing = stateIds.putIfAbsent(Integer.parseInt(row[3]), to);
                if (existing == null) {
                    assertTrue(mappedStates.add(to));
                } else {
                    assertEquals(existing.intValue(), to);
                }
                return true;
            });
            assertTrue("Reference topology must be reachable", pending.size() < previousSize);
        }
        for (String line : reference) {
            final String[] row = line.split("\\t");
            if ("stop".equals(row[0])) {
                assertProbability(row[2], model.getFinalStateProbability(stateIds.get(Integer.parseInt(row[1]))));
            }
            if ("evaluation".equals(row[0])) {
                final var word = split.getRight().get(Integer.parseInt(row[1]));
                assertEquals(Boolean.parseBoolean(row[2]), model.isInAutomaton(word));
                final String[] expected = row[4].substring(1, row[4].length() - 1).split(", ");
                final var actual = model.calculateProbabilities(word).getFirst();
                assertEquals(expected.length, actual.size());
                for (int i = 0; i < expected.length; i++) {
                    assertProbability(expected[i], actual.get(i));
                }
            }
        }
    }

    private static Path referencePath(String name) {
        return Path.of("..", "specs", "treba-alergia-parity", name);
    }

    private static void assertProbability(String expectedText, double actual) {
        final double expected = Double.parseDouble(expectedText);
        assertEquals(expected, actual, Math.max(1e-12, Math.abs(expected) * 1e-9));
    }
}
