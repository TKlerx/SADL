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

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.utils.IoUtils;
import utils.LibraryChecker;

/** Explicit Linux JNI capture. Run with -Dtest=SharedTrebaReferenceCapture. */
public class SharedTrebaReferenceCapture {
    @Test
    public void captureBothLearnersOnSharedInputs() throws Exception {
        assertTrue("Live Treba JNI is required", LibraryChecker.trebaDepsInstalled());
        final Path output = Path.of(System.getProperty("treba.reference.output"));
        Files.createDirectories(output);
        final var split = IoUtils.readTrainTestFile(resource("smac_mix_type1.txt"));
        for (MergeTest mode : List.of(MergeTest.ALERGIA, MergeTest.MDI)) {
            final String prefix = mode.name().toLowerCase(java.util.Locale.ROOT);
            final PdfaLearner nativeSmall = new TrebaPdfaLearner(0.8, true, mode, 0, 1);
            final PdfaLearner javaSmall = javaLearner(mode, 0.8, 1);
            final TimedInput small = TimedInput.parse(resource("treba-shared-small.inp"));
            writeSmall(output.resolve(prefix + "-small-native.tsv"), nativeSmall.train(small));
            writeSmall(output.resolve(prefix + "-small-java.tsv"), javaSmall.train(small));

            for (boolean useNative : new boolean[] {true, false}) {
                final PdfaLearner learner = useNative ? new TrebaPdfaLearner(0.05, true, mode, 0, 3)
                        : javaLearner(mode, 0.05, 3);
                final long start = System.nanoTime();
                final var model = new PdttaLearner(learner).train(split.getLeft());
                final long elapsed = System.nanoTime() - start;
                final Path scores = output.resolve(prefix + (useNative ? "-native.tsv" : "-java.tsv"));
                try (var writer = Files.newBufferedWriter(scores)) {
                    writer.write("# states\t" + model.getStateCount() + "\n");
                    for (int i = 0; i < split.getRight().size(); i++) {
                        final var word = split.getRight().get(i);
                        final var probabilities = model.calculateProbabilities(word);
                        writer.write(i + "\t" + word.length() + "\t" + model.isInAutomaton(word) + "\t"
                                + Arrays.toString(probabilities.getFirst().toArray()) + "\t"
                                + Arrays.toString(probabilities.getSecond().toArray()) + "\n");
                    }
                }
                System.out.printf("%s training %.3f seconds%n", scores.getFileName(), elapsed / 1e9);
            }
        }
    }

    static PdfaLearner javaLearner(MergeTest mode, double alpha, int minimumVisits) {
        return mode == MergeTest.MDI ? new MdiTrebaLearner(alpha, minimumVisits)
                : new TrebaNativeParityLearner(alpha, true, minimumVisits);
    }

    static Path resource(String name) throws Exception {
        return Path.of(SharedTrebaReferenceCapture.class.getResource("/pdtta/" + name).toURI());
    }

    static void writeSmall(Path output, PDFA model) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("# states\t" + model.getStateCount() + "\n");
            for (int state = 0; state < model.getStateCount(); state++) {
                writer.write("stop\t" + state + "\t" + model.getFinalStateProbability(state) + "\n");
            }
            for (var arc : model.getTransitions().stream().sorted().toList()) {
                writer.write("arc\t" + arc.getFromState() + "\t" + arc.getSymbol() + "\t" + arc.getToState()
                        + "\t" + arc.getProbability() + "\n");
            }
        }
    }
}
