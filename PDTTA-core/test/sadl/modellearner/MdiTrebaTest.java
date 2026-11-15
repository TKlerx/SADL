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
import static org.junit.Assert.assertArrayEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

import com.beust.jcommander.JCommander;

import sadl.input.TimedInput;
import sadl.run.factories.learn.PdttaFactory;

public class MdiTrebaTest {
    @Test
    public void matchesNativeAutomatonOnWeightedBranchingInput() throws Exception {
        final var input = TimedInput.parse(new StringReader("(a,1) (b,2)\n(a,1) (b,2)\n(a,1) (c,3)\n"));
        final var model = new MdiTrebaLearner(0.0, 1).train(input);
        final var rows = Files.readAllLines(reference("native-branching-pdfa.tsv"));
        final var states = new HashMap<Integer, Integer>();
        states.put(0, model.getStartState());
        assertEquals(4, model.getStateCount());
        assertEquals(3, model.getTransitions().size());
        for (String line : rows) {
            final String[] row = line.split("\\t");
            if ("arc".equals(row[0])) {
                final var arc = model.getTransition(states.get(Integer.parseInt(row[1])), row[2]);
                states.put(Integer.parseInt(row[3]), arc.getToState());
                assertEquals(Double.parseDouble(row[4]), arc.getProbability(), 1e-12);
            }
        }
        assertEquals(4, new java.util.HashSet<>(states.values()).size());
        for (String line : rows) {
            final String[] row = line.split("\\t");
            if ("stop".equals(row[0])) {
                assertEquals(Double.parseDouble(row[2]), model.getFinalStateProbability(states.get(Integer.parseInt(row[1]))), 1e-12);
            }
        }
    }

    @Test
    public void blueOnlyBranchRetainsItsConditionalProbability() {
        final var red = node(2, 2);
        final var blue = node(1, 4);
        connect(blue, 0, 3);
        // Only the paired roots contribute a difference: blue stops with 1/4 and the merged root with 1/2.
        assertEquals(Math.log(2) / 6, MdiTrebaLearner.score(red, blue, 6), 1e-15);
    }

    @Test
    public void blueOnlyStopCarriesTheNonzeroMergedPrefix() {
        final var red = node(2, 4);
        final var blue = node(1, 4);
        connect(red, 0, 2);
        final var blueChild = node(0, 3);
        blue.arcs.put(0, new TrebaNativeParityLearner.Arc(blueChild, 3));
        connect(blueChild, 1, 2);
        blueChild.stops = 1;
        final var blueOnly = blueChild.arcs.get(1).target;
        blueOnly.stops = 1;
        connect(blueOnly, 1, 1);
        // Root and paired-child stops use pooled counts. The blue-only leaf
        // keeps the shared prefix (merged 5/8, blue 3/4) and its own stop probability.
        final double root = Math.log(1.5);
        final double child = Math.log((5.0 / 8 * 3.0 / 5) / (3.0 / 4 * 1.0 / 3));
        final double leaf = Math.log((3.0 / 4) / (5.0 / 8));
        assertEquals((root + child + 2 * leaf) / 16, MdiTrebaLearner.score(red, blue, 8), 1e-15);
    }

    @Test
    public void matchesNativeCutoffAndBranchingScores() throws Exception {
        for (String file : new String[] {"native-scores-o2.tsv", "native-branching.tsv"}) {
            for (String line : Files.readAllLines(reference(file)).stream().distinct().toList()) {
                final String[] row = line.split("\\t");
                if ("blue-only".equals(row[0])) {
                    continue;
                }
                final boolean branching = "branching".equals(row[0]);
                final var red = branching ? node(1, 7) : node(2, 4);
                final var blue = branching ? node(1, 6) : node(1, 4);
                connect(red, 0, 2);
                connect(blue, 0, 3);
                if (branching) {
                    connect(red, 1, 4);
                    connect(blue, 1, 2);
                }
                final int observations = red.total + blue.total;
                final double score = MdiTrebaLearner.score(red, blue, observations);
                final double frozen = Double.parseDouble(row[1]);
                assertEquals(frozen, score, 1e-15);
                assertEquals("1".equals(row[2]), MdiTrebaLearner.compatible(red, blue, observations, Math.nextDown(frozen)));
                assertEquals("1".equals(row[3]), MdiTrebaLearner.compatible(red, blue, observations, frozen));
                assertEquals("1".equals(row[4]), MdiTrebaLearner.compatible(red, blue, observations, Math.nextUp(frozen)));
            }
        }
    }

    @Test
    public void comparesPdttaWithFrozenNativeRun() throws Exception {
        final Path input = Path.of(getClass().getResource("/saved-model-compatibility/train.inp").toURI());
        final var factory = new PdttaFactory();
        new JCommander(factory).parse("-mergeMethod", "MDI_TREBA", "-tauEstimation", "DENSITY");
        final var model = factory.create().train(TimedInput.parse(input));
        final var evaluation = TimedInput.parse(input.resolveSibling("evaluate.inp"));
        for (String line : Files.readAllLines(reference("native-pdtta.tsv"))) {
            final String[] row = line.split("\\t");
            switch (row[0]) {
                case "# states" -> {
                    assertEquals(1, Integer.parseInt(row[1]));
                    assertEquals(3, model.getStateCount());
                }
                case "evaluation" -> {
                    final var word = evaluation.get(Integer.parseInt(row[1]));
                    final var scores = model.calculateProbabilities(word);
                    assertEquals(Boolean.parseBoolean(row[3]), model.isInAutomaton(word));
                    if ("0".equals(row[1])) {
                        assertArrayEquals(new double[] {0.5, 0.5, 1.0, 1.0}, scores.getFirst().toArray(), 1e-12);
                        assertArrayEquals(new double[] {0.3357622144428068, 0.3357622144428068, 0.3357622144428068},
                                scores.getSecond().toArray(), 1e-12);
                    } else {
                        assertEquals(row[4], Arrays.toString(scores.getFirst().toArray()));
                        assertEquals(row[5], Arrays.toString(scores.getSecond().toArray()));
                    }
                }
                default -> { }
            }
        }
    }

    private static TrebaNativeParityLearner.State node(int stops, int total) {
        final var state = new TrebaNativeParityLearner.State();
        state.stops = stops;
        state.total = total;
        return state;
    }

    private static void connect(TrebaNativeParityLearner.State state, int symbol, int count) {
        state.arcs.put(symbol, new TrebaNativeParityLearner.Arc(node(count, count), count));
    }

    private static Path reference(String name) {
        return Path.of("..", "specs", "treba-mdi", name);
    }
}
