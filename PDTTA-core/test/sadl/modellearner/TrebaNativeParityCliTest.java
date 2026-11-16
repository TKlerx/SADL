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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.run.SADL;
import sadl.utils.IoUtils;

public class TrebaNativeParityCliTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void trainsAndTestsPdttaThroughTheOptInCli() throws Exception {
        final Path cwd = temporary.getRoot().toPath();
        final Path nativeLibrary = cwd.resolve("native_libs/libtreba.so");
        Files.createDirectories(nativeLibrary.getParent());
        Files.writeString(nativeLibrary, "This is deliberately not a loadable shared library.");
        final Path input = Path.of(getClass().getResource("/saved-model-compatibility/train.inp").toURI());
        final Path evaluation = input.resolveSibling("evaluate.inp");
        final Path model = cwd.resolve("pdtta.model");
        run(cwd, "train", "PDTTA", "-in", input.toString(), "-out", model.toString(),
                "-mergeMethod", "TREBA_NATIVE_PARITY", "-tauEstimation", "DENSITY",
                "-kdeKernelFunction", "GAUSS", "-kdeBandwidthEstimate", "false", "-kdeBandwidth", "1");
        assertTrue(Files.size(model) > 0);
        run(cwd, "test", "-model", model.toString(), "-trainSeqs", input.toString(), "-testSeqs", evaluation.toString(),
                "-detectorMethod", "THRESHOLD_AGG_ONLY", "-featureCreator", "MINIMAL",
                "-aggregatedEventThreshold", "0.01", "-aggregatedTimeThreshold", "0.01");
        assertEquals(List.of("1", "0", "1", "0"),
                List.of(Files.readString(cwd.resolve("sadl_test_res.csv")).split(";")).subList(6, 10));
    }

    @Test
    public void legacyAlergiaConstructorMatchesNativeSmoothingWithoutJni() throws Exception {
        final Path cwd = temporary.getRoot().toPath();
        final Path nativeLibrary = cwd.resolve("native_libs/libtreba.so");
        Files.createDirectories(nativeLibrary.getParent());
        Files.writeString(nativeLibrary, "This is deliberately not a loadable shared library.");
        final Path reference = Path.of("..", "specs", "treba-alergia-parity").toAbsolutePath();
        runJava(cwd, LegacyAlergiaProbe.class, reference.toString());
    }

    @Test
    public void legacyMdiMatchesFrozenNativeModelWithoutJni() throws Exception {
        final Path cwd = temporary.getRoot().toPath();
        final Path library = cwd.resolve("native_libs/libtreba.so");
        Files.createDirectories(library.getParent());
        Files.writeString(library, "This is deliberately not a loadable shared library.");
        final Path input = Path.of(getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
        runJava(cwd, LegacyMdiProbe.class, input.toString());
    }

    @Test
    public void retiredMergeTestsPointToTheHistoricalRevision() {
        for (var mode : List.of(MergeTest.CHI_SQUARED, MergeTest.LR, MergeTest.BINOMIAL, MergeTest.EXACT_M, MergeTest.EXACT_B)) {
            final var failure = assertThrows(IllegalArgumentException.class, () -> new PdttaLearner(0.05, true, mode));
            assertTrue(failure.getMessage().contains(mode.name()));
            assertTrue(failure.getMessage().contains("c80ede9"));
        }
    }

    public static class LegacyMdiProbe {
        public static void main(String[] args) throws Exception {
            final var split = IoUtils.readTrainTestFile(Path.of(args[0]));
            final var learner = new PdttaLearner(0.05, true, null, -1, MergeTest.MDI, 0.0, 3, null);
            assertEquals(39, learner.train(split.getLeft()).getStateCount());
        }
    }

    public static class LegacyAlergiaProbe {
        public static void main(String[] args) throws Exception {
            final var input = TimedInput.parse(new StringReader("(a,1) (b,2)\n(a,1) (b,2)\n"));
            final Path reference = Path.of(args[0]);
            checkModel(new TrebaNativeParityLearner(0.05, true, 3, 0.5).train(input), reference.resolve("native-smoothing.tsv"));
            checkModel(new PdttaLearner(0.05, true, null, -1, MergeTest.ALERGIA, 0.5).train(input),
                    reference.resolve("native-pdtta-smoothing.tsv"));
        }

        private static void checkModel(PDFA model, Path reference) throws Exception {
            final var rows = Files.readAllLines(reference);
            assertEquals(rows.size() - 1, model.getStateCount());
            for (String line : rows.subList(1, rows.size())) {
                final var row = line.split("\\t");
                final int state = Integer.parseInt(row[0]);
                assertEquals(Double.parseDouble(row[1]), model.getFinalStateProbability(state), 1e-12);
                checkTransition(model, state, "a", row[2], row[3]);
                checkTransition(model, state, "b", row[4], row[5]);
            }
        }

        private static void checkTransition(PDFA model, int state, String symbol, String target, String probability) {
            final var transition = model.getTransition(state, symbol);
            if ("-".equals(target)) {
                assertNull(transition);
            } else {
                assertEquals(Integer.parseInt(target), transition.getToState());
                assertEquals(Double.parseDouble(probability), transition.getProbability(), 1e-12);
            }
        }
    }

    private static void run(Path cwd, String... arguments) throws Exception {
        final var args = new ArrayList<>(List.of("-parallel", "false"));
        args.addAll(List.of(arguments));
        runJava(cwd, SADL.class, args.toArray(String[]::new));
    }

    private static void runJava(Path cwd, Class<?> entryPoint, String... arguments) throws Exception {
        final Path bin = Path.of(System.getProperty("java.home"), "bin");
        final Path java = Files.isRegularFile(bin.resolve("java")) ? bin.resolve("java") : bin.resolve("java.exe");
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final var entries = new ArrayList<String>();
        for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            entries.add(Path.of(entry).toAbsolutePath().toString());
        }
        final var command = new ArrayList<>(List.of(java.toString(), "-cp", String.join(File.pathSeparator, entries),
                entryPoint.getName()));
        command.addAll(List.of(arguments));
        final Path output = cwd.resolve("cli.log");
        final Process child = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true)
                .redirectOutput(output.toFile()).start();
        try {
            assertTrue("CLI timed out", child.waitFor(45, TimeUnit.SECONDS));
            assertEquals(Files.readString(output), 0, child.exitValue());
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
            }
        }
    }

}
