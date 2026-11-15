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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import utils.LibraryChecker;

import static org.junit.Assert.assertTrue;

/** Run explicitly with -Dtest=TrebaMdiReferenceCapture on Linux with JNI. */
public class TrebaMdiReferenceCapture {
    @Test
    public void captureDefinedBranchingAutomaton() throws Exception {
        assertTrue("Live Treba JNI is required", LibraryChecker.trebaDepsInstalled());
        final var input = TimedInput.parse(new java.io.StringReader(
                "(a,1) (b,2)\n(a,1) (b,2)\n(a,1) (c,3)\n"));
        final var model = new TrebaPdfaLearner(0.0, false, MergeTest.MDI, 0.0, 1).train(input);
        final var lines = new ArrayList<String>();
        for (int state = 0; state < model.getStateCount(); state++) {
            lines.add("stop\t" + state + "\t" + model.getFinalStateProbability(state));
        }
        model.getTransitions().stream().sorted().forEach(arc -> lines.add("arc\t" + arc.getFromState() + "\t"
                + arc.getSymbol() + "\t" + arc.getToState() + "\t" + arc.getProbability()));
        Files.write(Path.of(System.getProperty("treba.reference.output") + ".branching.tsv"), lines);
    }

    @Test
    public void captureNativeTrainToTest() throws Exception {
        assertTrue("Live Treba JNI is required", LibraryChecker.trebaDepsInstalled());
        final Path inputs = Path.of(getClass().getResource("/saved-model-compatibility/train.inp").toURI()).getParent();
        final var training = TimedInput.parse(inputs.resolve("train.inp"));
        final var evaluation = TimedInput.parse(inputs.resolve("evaluate.inp"));
        final var pdtta = new PdttaLearner(new TrebaPdfaLearner(0.05, false, MergeTest.MDI, 0.0, 3)).train(training);
        final var lines = new ArrayList<String>();
        lines.add("# states\t" + pdtta.getStateCount());
        lines.add("kind\tfrom\tsymbol\tto\tprobability");
        pdtta.getTransitions().stream().sorted().forEach(arc -> lines.add(String.format(Locale.ROOT,
                "transition\t%d\t%s\t%d\t%s", arc.getFromState(), arc.getSymbol(), arc.getToState(), arc.getProbability())));
        lines.add("kind\tstate\tprobability");
        for (int state = 0; state < pdtta.getStateCount(); state++) {
            lines.add("stop\t" + state + "\t" + pdtta.getFinalStateProbability(state));
        }
        lines.add("kind\tindex\tlabel\taccepted\tevent-probabilities\ttime-scores");
        for (int i = 0; i < evaluation.size(); i++) {
            final var word = evaluation.get(i);
            final var scores = pdtta.calculateProbabilities(word);
            lines.add("evaluation\t" + i + "\t" + word.getLabel() + "\t" + pdtta.isInAutomaton(word)
                    + "\t" + Arrays.toString(scores.getFirst().toArray()) + "\t" + Arrays.toString(scores.getSecond().toArray()));
        }
        Files.write(Path.of(System.getProperty("treba.reference.output")), lines);
    }
}
