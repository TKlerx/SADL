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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import sadl.run.SADL;

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

    private static void run(Path cwd, String... arguments) throws Exception {
        final Path bin = Path.of(System.getProperty("java.home"), "bin");
        final Path java = Files.isRegularFile(bin.resolve("java")) ? bin.resolve("java") : bin.resolve("java.exe");
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final var entries = new ArrayList<String>();
        for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            entries.add(Path.of(entry).toAbsolutePath().toString());
        }
        final var command = new ArrayList<>(List.of(java.toString(), "-cp", String.join(File.pathSeparator, entries),
                SADL.class.getName(), "-parallel", "false"));
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
