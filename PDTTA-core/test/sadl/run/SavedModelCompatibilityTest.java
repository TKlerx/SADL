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
package sadl.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SavedModelCompatibilityTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void evaluatesReferenceModel() throws Exception {
		evaluate(fixture("reference.model"));
	}

	@Test
	public void trainsAndEvaluatesNewModel() throws Exception {
		final Path directory = temporary.newFolder().toPath();
		final Path model = directory.resolve("trained.model");
		runCli(directory, "train", "RTI", "-in", fixture("train.inp").toString(), "-out", model.toString(),
				"-sig", "0.05", "-hist", "QUANTILE");
		assertTrue("CLI did not write a nonempty model", Files.isRegularFile(model) && Files.size(model) > 0);
		evaluate(model);
	}

	private void evaluate(Path model) throws Exception {
		final Path directory = temporary.newFolder().toPath();
		final Path result = directory.resolve("sadl_test_res.csv");
		runCli(directory, "test", "-model", model.toString(), "-trainSeqs", fixture("train.inp").toString(),
				"-testSeqs", fixture("evaluate.inp").toString(), "-detectorMethod", "THRESHOLD_AGG_ONLY",
				"-featureCreator", "MINIMAL", "-aggregatedEventThreshold", "0.01", "-aggregatedTimeThreshold", "0.01");
		assertTrue("CLI did not write a result for " + model, Files.isRegularFile(result));
		final List<String> rows = Files.readAllLines(result, StandardCharsets.UTF_8);
		assertEquals("Expected one evaluation row", 1, rows.size());
		final String[] fields = rows.get(0).split(";", -1);
		assertTrue("Missing evaluation counts: " + rows.get(0), fields.length >= 10);
		assertEquals("TP, FP, TN, FN", Arrays.asList("1", "0", "1", "0"),
				Arrays.asList(fields).subList(6, 10));
	}

	private static Path fixture(String name) throws Exception {
		return Paths.get(SavedModelCompatibilityTest.class.getResource("/saved-model-compatibility/" + name).toURI());
	}

	private static void runCli(Path directory, String... arguments) throws Exception {
		final Path bin = Paths.get(System.getProperty("java.home"), "bin");
		final Path java = Files.isRegularFile(bin.resolve("java")) ? bin.resolve("java") : bin.resolve("java.exe");
		final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		final List<String> absoluteEntries = new ArrayList<>();
		for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
			absoluteEntries.add(Paths.get(entry).toAbsolutePath().toString());
		}
		final List<String> command = new ArrayList<>(Arrays.asList(java.toString(), "-cp",
				String.join(File.pathSeparator, absoluteEntries), SADL.class.getName(), "-parallel", "false"));
		command.addAll(Arrays.asList(arguments));
		final Path output = directory.resolve("cli.log");
		final Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
				.redirectOutput(output.toFile()).start();
		if (!process.waitFor(30, TimeUnit.SECONDS)) {
			process.destroyForcibly().waitFor();
			fail("SADL CLI timed out: " + new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
		}
		assertEquals("SADL CLI failed: " + new String(Files.readAllBytes(output), StandardCharsets.UTF_8),
				0, process.exitValue());
	}
}
