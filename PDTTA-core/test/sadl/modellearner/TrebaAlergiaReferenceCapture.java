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

import java.io.StringReader;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import sadl.constants.MergeMethod;
import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.structure.Transition;
import sadl.utils.IoUtils;
import utils.LibraryChecker;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Explicitly run this capture with -Dtest=TrebaAlergiaReferenceCapture. */
public class TrebaAlergiaReferenceCapture {
    @Test
    public void captureNonzeroSmoothing() throws Exception {
        assertTrue("Live Treba JNI is required", LibraryChecker.trebaDepsInstalled());
        final Path output = Path.of(System.getProperty("treba.reference.output"));
        Files.createDirectories(output);
        final var input = TimedInput.parse(new StringReader("(a,1) (b,2)\n(a,1) (b,2)\n"));
        final var learner = new TrebaPdfaLearner(0.05, true, MergeTest.ALERGIA, 0.5, 3);
        writeSmoothing(output.resolve("native-smoothing.tsv"), learner.train(input));
        writeSmoothing(output.resolve("native-pdtta-smoothing.tsv"), new PdttaLearner(learner).train(input));
    }

    private static void writeSmoothing(Path output, PDFA model) throws Exception {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output))) {
            writer.println("state\tstop\ta-target\ta-probability\tb-target\tb-probability");
            for (int i = 0; i < model.getStateCount(); i++) {
                final var a = model.getTransition(i, "a");
                final var b = model.getTransition(i, "b");
                writer.printf("%d\t%s\t%s\t%s%n", i, model.getFinalStateProbability(i),
                        a == null ? "-\t-" : a.getToState() + "\t" + a.getProbability(),
                        b == null ? "-\t-" : b.getToState() + "\t" + b.getProbability());
            }
            if (writer.checkError()) {
                throw new java.io.IOException("Could not write smoothing capture " + output);
            }
        }
    }

    @Test
    public void captureBenchmark() throws Exception {
        assertTrue("Live Treba JNI is required", LibraryChecker.trebaDepsInstalled());
        final Path output = Path.of(System.getProperty("treba.reference.output"));
        final Path input = Path.of(getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
        final Pair<TimedInput, TimedInput> split = IoUtils.readTrainTestFile(input);
        final PDFA nativeModel = new TrebaPdfaLearner(0.05, true, MergeTest.ALERGIA, 0.0, 0).train(split.getLeft());
        final PDFA javaModel = new AlergiaRedBlue(0.05, true, MergeMethod.TREBA).train(split.getLeft());
        assertEquals(356, nativeModel.getStateCount());
        assertEquals(70, javaModel.getStateCount());

        Files.createDirectories(output);
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output.resolve("native-benchmark.tsv")))) {
            writer.println("# Native states\t" + nativeModel.getStateCount());
            writer.println("# Java TREBA states\t" + javaModel.getStateCount());
            writer.println("kind\tfrom\tsymbol\tto\tprobability");
            final List<Transition> transitions = new ArrayList<>(nativeModel.getTransitions());
            transitions.sort(Comparator.naturalOrder());
            for (Transition transition : transitions) {
                writer.printf("transition\t%d\t%s\t%d\t%s%n", transition.getFromState(), transition.getSymbol(),
                        transition.getToState(), transition.getProbability());
            }
            writer.println("kind\tstate\tprobability");
            for (int state = 0; state < nativeModel.getStateCount(); state++) {
                writer.printf("stop\t%d\t%s%n", state, nativeModel.getFinalStateProbability(state));
            }
            writer.println("kind\tevaluation-index\tnative-accepted\tjava-treba-accepted\tnative-event-probabilities\tjava-treba-event-probabilities");
            for (int index : new int[] {0, 1, 2, split.getRight().size() / 2, split.getRight().size() - 1}) {
                final var word = split.getRight().get(index);
                writer.printf("evaluation\t%d\t%b\t%b\t%s\t%s%n", index, nativeModel.isInAutomaton(word),
                        javaModel.isInAutomaton(word), nativeModel.calculateProbabilities(word).getFirst(),
                        javaModel.calculateProbabilities(word).getFirst());
            }
            if (writer.checkError()) {
                throw new java.io.IOException("Could not write benchmark capture " + output);
            }
        }
    }
}
