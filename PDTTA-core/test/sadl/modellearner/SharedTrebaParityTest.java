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
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.models.PDFA;

public class SharedTrebaParityTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void alergiaMatchesNativeAfterMergingSharedTraces() throws Exception {
        verify(MergeTest.ALERGIA, "alergia");
    }

    @Test
    public void mdiMatchesNativeAfterMergingSharedTraces() throws Exception {
        verify(MergeTest.MDI, "mdi");
    }

    private void verify(MergeTest mode, String name) throws Exception {
        final var input = TimedInput.parse(Path.of(getClass().getResource("/pdtta/treba-shared-small.inp").toURI()));
        final var unmerged = learner(mode, input.size() + 1).train(input);
        final var model = learner(mode, 1).train(input);
        assertTrue(model.getStateCount() < unmerged.getStateCount());
        final Path actual = temporary.newFile().toPath();
        writeSmall(actual, model);
        final var expected = Files.readAllLines(Path.of("..", "specs", "treba-mdi", name + "-small-native.tsv"));
        final var observed = Files.readAllLines(actual);
        assertEquals(expected.getFirst(), observed.getFirst());
        assertEquals(expected.size(), observed.size());
        for (int i = 1; i < expected.size(); i++) {
            final List<String> left = List.of(expected.get(i).split("\\t"));
            final List<String> right = List.of(observed.get(i).split("\\t"));
            assertEquals(left.subList(0, left.size() - 1), right.subList(0, right.size() - 1));
            assertEquals(Double.parseDouble(left.getLast()), Double.parseDouble(right.getLast()), 1e-12);
        }
    }
    private PdfaLearner learner(MergeTest mode, int minimumVisits) {
        return mode == MergeTest.MDI ? new MdiTrebaLearner(0.8, minimumVisits)
                : new TrebaNativeParityLearner(0.8, true, minimumVisits);
    }

    private static void writeSmall(Path output, PDFA model) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("# states\t" + model.getStateCount() + "\n");
            for (int state : Arrays.stream(model.getStates()).sorted().toArray()) {
                writer.write("stop\t" + state + "\t" + model.getFinalStateProbability(state) + "\n");
            }
            for (var arc : model.getTransitions().stream().sorted().toList()) {
                writer.write("arc\t" + arc.getFromState() + "\t" + arc.getSymbol() + "\t" + arc.getToState()
                        + "\t" + arc.getProbability() + "\n");
            }
        }
    }
}
