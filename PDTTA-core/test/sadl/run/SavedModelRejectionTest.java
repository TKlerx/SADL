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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import gnu.trove.map.TIntObjectMap;
import sadl.models.pdrta.PDRTA;

public class SavedModelRejectionTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void rejectsMissingModel() throws Exception {
		assertRejected(temporary.getRoot().toPath().resolve("missing.model"), false);
	}

	@Test
	public void rejectsDirectoryAsModel() throws Exception {
		assertRejected(temporary.newFolder().toPath(), false);
	}

	@Test
	public void rejectsMalformedModelWithoutReplacingResult() throws Exception {
		assertRejected(Files.write(temporary.newFile().toPath(), new byte[] { 0, 1, 2, 3 }), true);
	}

	@Test
	public void rejectsTruncatedModel() throws Exception {
		assertRejected(Files.write(temporary.newFile().toPath(), Arrays.copyOf(Files.readAllBytes(reference()), 128)), false);
	}

	@Test
	public void rejectsUnsupportedRoot() throws Exception {
		assertRejected(save(new Date(0)), false);
	}

	@Test
	public void rejectsUnsupportedNestedObject() throws Exception {
		assertRejected(modelWithNested(new Date(0)), false);
	}

	@Test
	public void rejectsPdrtaWithoutRootBeforeEvaluation() throws Exception {
		final PDRTA model = readReference();
		final Field root = PDRTA.class.getDeclaredField("root");
		root.setAccessible(true);
		root.set(model, null);
		final Path data = reference().resolveSibling("evaluate.inp");
		final String output = assertRejectedFrom(SADL.class, save(model), data, true);
		assertTrue(output, output.contains("missing PDRTA root state"));
	}

	@SuppressWarnings("unchecked")
	private Path modelWithNested(Object value) throws Exception {
		final PDRTA model = readReference();
		final Field states = PDRTA.class.getDeclaredField("states");
		states.setAccessible(true);
		((TIntObjectMap<Object>) states.get(model)).put(-1, value);
		return save(model);
	}

	private static PDRTA readReference() throws Exception {
		try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(reference()))) {
			return (PDRTA) input.readObject();
		}
	}

	private Path save(Object model) throws Exception {
		final Path file = temporary.newFile().toPath();
		try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(file))) {
			output.writeObject(model);
		}
		return file;
	}

	private static Path reference() throws Exception {
		return Paths.get(SavedModelRejectionTest.class.getResource("/saved-model-compatibility/reference.model").toURI());
	}

	@Test
	public void rejectsOversizeModel() throws Exception {
		final Path file = temporary.newFile().toPath();
		try (RandomAccessFile sparse = new RandomAccessFile(file.toFile(), "rw")) {
			sparse.setLength(16_777_217);
		}
		assertRejected(file, false);
	}

	@Test
	public void rejectsWhenTheFilterCannotBeInstalled() throws Exception {
		assertRejected(reference(), false, "-Djdk.serialFilterFactory=" + DiscardingFilterFactory.class.getName());
	}

	@Test
	public void preservesConfiguredProcessFilter() throws Exception {
		final String output = assertRejected(reference(), true, "-Djdk.serialFilter=!sadl.models.pdrta.PDRTA;*");
		assertTrue(output, output.contains("unsupported model type or contents"));
	}

	@Test
	public void rejectsGraphDepthThroughCli() throws Exception {
		Object value = "leaf";
		for (int depth = 0; depth < 65; depth++) {
			value = new ArrayList<>(List.of(value));
		}
		assertCliLimit(value);
	}

	@Test
	public void rejectsReferenceCountThroughCli() throws Exception {
		final List<Object> groups = new ArrayList<>();
		for (int group = 0; group < 251; group++) {
			groups.add(new ArrayList<>(Collections.nCopies(1_000, Integer.valueOf(1))));
		}
		assertCliLimit(groups);
	}

	@Test
	public void rejectsArrayLengthThroughCli() throws Exception {
		assertCliLimit(new int[250_001]);
	}

	@Test
	public void rejectsLoadingTimeoutThroughCli() throws Exception {
		final String output = assertRejectedFrom(TimeoutCli.class, reference(), true);
		assertTrue(output, output.contains("loading deadline exceeded"));
	}

	@Test
	public void rejectsWorkerHeapExhaustionThroughCli() throws Exception {
		final List<double[]> data = new ArrayList<>();
		for (int array = 0; array < 6; array++) {
			data.add(new double[200_000]);
		}
		final String output = assertRejectedFrom(SmallHeapCli.class, modelWithNested(data), true);
		assertTrue(output, output.contains("loading worker failed"));
		assertTrue(output, output.contains("OutOfMemoryError"));
	}

	private void assertCliLimit(Object value) throws Exception {
		final String output = assertRejected(modelWithNested(value), true);
		assertTrue(output, output.contains("model exceeds a loading limit"));
	}

	@Test
	public void rejectsOpenedStreamReadLimitThroughCli() throws Exception {
		final Path model = reference();
		assertTrue(Files.size(model) > StreamCapCli.READ_LIMIT && Files.size(model) <= SavedModelLoader.MAX_BYTES);
		final String output = assertRejectedFrom(StreamCapCli.class, model, true);
		assertTrue(output, output.contains("opened model stream exceeds the read limit"));
	}

	public static class StreamCapCli {
		private static final long READ_LIMIT = 4096;

		public static void main(String[] args) throws Exception {
			SADL.main(args, new SavedModelWorker.Budget(512, Duration.ofSeconds(30), READ_LIMIT));
		}
	}

	public static class TimeoutCli {
		public static void main(String[] args) throws Exception {
			SADL.main(args, new SavedModelWorker.Budget(512, Duration.ZERO));
		}
	}

	public static class SmallHeapCli {
		public static void main(String[] args) throws Exception {
			SADL.main(args, new SavedModelWorker.Budget(8, Duration.ofSeconds(30)));
		}
	}

	public static class DiscardingFilterFactory implements BinaryOperator<ObjectInputFilter> {
		@Override
		public ObjectInputFilter apply(ObjectInputFilter current, ObjectInputFilter requested) {
			return null;
		}
	}

	private String assertRejected(Path model, boolean existingResult, String... jvmOptions) throws Exception {
		return assertRejectedFrom(SADL.class, model, existingResult, jvmOptions);
	}

	private String assertRejectedFrom(Class<?> cli, Path model, boolean existingResult, String... jvmOptions) throws Exception {
		return assertRejectedFrom(cli, model, temporary.newFile().toPath(), existingResult, jvmOptions);
	}

	private String assertRejectedFrom(Class<?> cli, Path model, Path data, boolean existingResult, String... jvmOptions) throws Exception {
		final Path cwd = temporary.newFolder().toPath();
		final Path result = cwd.resolve("sadl_test_res.csv");
		final byte[] original = { 42 };
		if (existingResult) {
			Files.write(result, original);
		}
		final Path bin = Paths.get(System.getProperty("java.home"), "bin");
		final Path java = Files.isRegularFile(bin.resolve("java")) ? bin.resolve("java") : bin.resolve("java.exe");
		final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		final List<String> entries = new ArrayList<>();
		for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
			entries.add(Paths.get(entry).toAbsolutePath().toString());
		}
		final Path output = cwd.resolve("cli.log");
		final List<String> command = new ArrayList<>(List.of(java.toString()));
		command.addAll(List.of(jvmOptions));
		command.addAll(List.of("-cp", String.join(File.pathSeparator, entries), cli.getName(), "test",
				"-model", model.toString(), "-trainSeqs", reference().resolveSibling("train.inp").toString(),
				"-testSeqs", data.toString(), "-detectorMethod", "THRESHOLD_AGG_ONLY",
				"-featureCreator", "MINIMAL", "-aggregatedEventThreshold", "0.01", "-aggregatedTimeThreshold", "0.01"));
		final Process child = new ProcessBuilder(command)
				.directory(cwd.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue("CLI timed out", child.waitFor(45, TimeUnit.SECONDS));
		} finally {
			if (child.isAlive()) {
				child.destroyForcibly();
			}
		}
		final String log = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
		assertNotEquals(log, 0, child.exitValue());
		assertTrue(log, log.contains("Model load failed:"));
		assertFalse(log, log.contains("F-Measure="));
		if (existingResult) {
			assertArrayEquals(original, Files.readAllBytes(result));
		} else {
			assertFalse(Files.exists(result));
		}
		return log;
	}
}
