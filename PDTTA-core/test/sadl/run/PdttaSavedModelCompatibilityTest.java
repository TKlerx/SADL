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

import java.io.File;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PdttaSavedModelCompatibilityTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void evaluatesSupportedPdttaVariants() throws Exception {
		final Path inputs = Paths.get(getClass().getResource("/saved-model-compatibility/train.inp").toURI()).getParent();
		final Path train = inputs.resolve("train.inp");
		final Path evaluation = inputs.resolve("evaluate.inp");
		for (String estimator : Arrays.asList("DENSITY", "MONTE_CARLO")) {
			for (String kernel : Arrays.asList("BIWEIGHT", "EPANECHNIKOV", "GAUSS", "TRIWEIGHT", "UNIFORM", "ESTIMATE")) {
				verifyVariant(train, evaluation, estimator, kernel, estimator + "/" + kernel);
			}
		}
		if (Boolean.getBoolean("sadl.test.recordModelTypes")) {
			recordTypes(inputs.resolve("reference.model"), "RTI/reference");
		}
	}

	@Test
	public void evaluatesConstantTimingPdtta() throws Exception {
		final Path train = temporary.newFile("constant.inp").toPath();
		Files.write(train, java.util.Collections.nCopies(12, "(a,3) (b,5) (a,3)"), StandardCharsets.UTF_8);
		final Path evaluation = Paths.get(getClass().getResource("/saved-model-compatibility/evaluate.inp").toURI());
		for (String estimator : Arrays.asList("DENSITY", "MONTE_CARLO")) {
			verifyVariant(train, evaluation, estimator, "ESTIMATE", estimator + "/CONSTANT");
		}
	}

	private void verifyVariant(Path train, Path evaluation, String estimator, String kernel, String variant) throws Exception {
		final Path cwd = temporary.newFolder().toPath();
		final Path model = cwd.resolve("pdtta.model");
		run(cwd, "train", "PDTTA", "-in", train.toString(), "-out", model.toString(),
				"-tauEstimation", estimator, "-kdeKernelFunction", kernel, "-kdeBandwidthEstimate", "false",
				"-kdeBandwidth", "1", "-mcNumberOfSteps", "32", "-mcPointsToStore", "64");
		assertTrue(Files.size(model) > 0);
		if (Boolean.getBoolean("sadl.test.recordModelTypes")) {
			recordTypes(model, variant);
		}
		run(cwd, "test", "-model", model.toString(), "-trainSeqs", train.toString(),
				"-testSeqs", evaluation.toString(), "-detectorMethod", "THRESHOLD_AGG_ONLY",
				"-featureCreator", "MINIMAL", "-aggregatedEventThreshold", "0.01", "-aggregatedTimeThreshold", "0.01");
		final String[] fields = new String(Files.readAllBytes(cwd.resolve("sadl_test_res.csv")), StandardCharsets.UTF_8).split(";");
		assertEquals(variant, Arrays.asList("1", "0", "1", "0"), Arrays.asList(fields).subList(6, 10));
	}

	private static void recordTypes(Path model, String variant) throws Exception {
		final Set<String> descriptors = new TreeSet<>();
		final Set<String> runtime = new TreeSet<>();
		try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(model)) {
			@Override
			protected ObjectStreamClass readClassDescriptor() throws java.io.IOException, ClassNotFoundException {
				final ObjectStreamClass descriptor = super.readClassDescriptor();
				descriptors.add(descriptor.getName());
				return descriptor;
			}
		}) {
			input.setObjectInputFilter(info -> {
				if (info.serialClass() != null) {
					runtime.add(info.serialClass().getName());
				}
				return ObjectInputFilter.Status.UNDECIDED;
			});
			input.readObject();
		}
		System.out.println("MODEL DESCRIPTORS " + variant + " " + descriptors);
		System.out.println("MODEL FILTER " + variant + " " + runtime);
	}

	private static void run(Path cwd, String... arguments) throws Exception {
		final Path bin = Paths.get(System.getProperty("java.home"), "bin");
		final Path java = Files.isRegularFile(bin.resolve("java")) ? bin.resolve("java") : bin.resolve("java.exe");
		final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		final List<String> entries = new ArrayList<>();
		for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
			entries.add(Paths.get(entry).toAbsolutePath().toString());
		}
		final List<String> command = new ArrayList<>(Arrays.asList(java.toString(), "-cp", String.join(File.pathSeparator, entries),
				SADL.class.getName(), "-parallel", "false"));
		command.addAll(Arrays.asList(arguments));
		final Path output = cwd.resolve("cli.log");
		final Process child = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true)
				.redirectOutput(output.toFile()).start();
		try {
			assertTrue("CLI timed out", child.waitFor(45, TimeUnit.SECONDS));
		} finally {
			if (child.isAlive()) {
				child.destroyForcibly();
			}
		}
		assertEquals(new String(Files.readAllBytes(output), StandardCharsets.UTF_8), 0, child.exitValue());
	}
}
