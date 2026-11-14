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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.beust.jcommander.JCommander;

import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.run.factories.learn.PdfaFactory;
import sadl.run.factories.learn.PdttaFactory;

public class MdiPaperTest {
    @Test
    public void rejectsEqualityAndAcceptsTheNextThreshold() throws Exception {
        // Folding two states makes both the event and stop probabilities 1/2.
        final double cutoff = Math.log(4);
        assertEquals(2, train("(a,1)", Math.nextDown(cutoff)).getStateCount());
        assertEquals(2, train("(a,1)", cutoff).getStateCount());
        assertEquals(1, train("(a,1)", Math.nextUp(cutoff)).getStateCount());
    }

    @Test
    public void keepsBlueOnlyBranchWhenFoldingItsParent() throws Exception {
        // The first fold assigns probability 1/4 to "ab"; a second fold would reduce it to 1/27.
        final PDFA model = train("(a,1) (b,2)", 1.5);
        assertEquals(2, model.getStateCount());
        final int start = model.getStartState();
        assertEquals(start, model.getTransition(start, "a").getToState());
        final var blueOnly = model.getTransition(start, "b");
        assertEquals(0.5, blueOnly.getProbability(), 0.0);
        assertEquals(1.0, model.getFinalStateProbability(blueOnly.getToState()), 0.0);
        assertNull(model.getTransition(blueOnly.getToState(), "b"));
    }

    private static PDFA train(String word, double alpha) throws Exception {
        final var factory = new PdfaFactory();
        new JCommander(factory).parse("-mergeMethod", "MDI_PAPER", "-mergeT0", "1", "-mergeAlpha", Double.toString(alpha));
        return factory.createPdfaLearner().train(TimedInput.parse(new StringReader(word + "\n" + word + "\n")));
    }

    @Test
    public void comparesPdttaScoresWithFrozenNativeRun() throws Exception {
        final Path inputs = Path.of(getClass().getResource("/saved-model-compatibility/train.inp").toURI()).getParent();
        final var training = TimedInput.parse(inputs.resolve("train.inp"));
        final var evaluation = TimedInput.parse(inputs.resolve("evaluate.inp"));
        final var factory = new PdttaFactory();
        new JCommander(factory).parse("-mergeMethod", "MDI_PAPER", "-mergeAlpha", "0.05", "-mergeT0", "3",
                "-tauEstimation", "DENSITY");
        final var model = factory.create().train(training);
        final var nativeRun = Files.readAllLines(Path.of("..", "specs", "treba-mdi", "native-pdtta.tsv"));
        assertEquals(1, Integer.parseInt(nativeRun.getFirst().split("\\t")[1]));
        assertEquals(4, model.getStateCount());
        for (String line : nativeRun) {
            final String[] row = line.split("\\t");
            if (!"evaluation".equals(row[0])) {
                continue;
            }
            final int index = Integer.parseInt(row[1]);
            final var word = evaluation.get(index);
            final var scores = model.calculateProbabilities(word);
            assertEquals(Boolean.parseBoolean(row[3]), model.isInAutomaton(word));
            final double[] expected = index == 0 ? new double[] {1, 1, 1, 1} : new double[] {0};
            assertArrayEquals(expected, scores.getFirst().toArray(), 0.0);
            if (index == 0) {
                // Java fits timing per position. Native pools both a occurrences on one arc.
                final double middle = 0.3357622144428068;
                assertArrayEquals(new double[] {middle, middle, middle}, scores.getSecond().toArray(), 1e-12);
                assertNotEquals(row[4], java.util.Arrays.toString(scores.getFirst().toArray()));
                assertNotEquals(row[5], java.util.Arrays.toString(scores.getSecond().toArray()));
            } else {
                assertEquals(row[4], java.util.Arrays.toString(scores.getFirst().toArray()));
                assertEquals(row[5], java.util.Arrays.toString(scores.getSecond().toArray()));
            }
        }
    }
}
