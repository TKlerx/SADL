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

import static java.io.ObjectInputFilter.Status.REJECTED;
import static java.io.ObjectInputFilter.Status.UNDECIDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.FilterInfo;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SavedModelLoadingLimitsTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void graphLimitsAreInclusive() {
		assertEquals(UNDECIDED, SavedModelLoader.filter(new Graph(64, 250_000, 250_000, 16_777_216)));
		assertEquals(REJECTED, SavedModelLoader.filter(new Graph(65, 1, -1, 1)));
		assertEquals(REJECTED, SavedModelLoader.filter(new Graph(1, 250_001, -1, 1)));
		assertEquals(REJECTED, SavedModelLoader.filter(new Graph(1, 1, 250_001, 1)));
		assertEquals(REJECTED, SavedModelLoader.filter(new Graph(1, 1, -1, 16_777_217)));
	}

	@Test
	public void rejectsOversizeFileBeforeReconstruction() throws Exception {
		final Path file = temporary.newFile().toPath();
		try (RandomAccessFile sparse = new RandomAccessFile(file.toFile(), "rw")) {
			sparse.setLength(16_777_217);
		}
		assertEquals(SavedModelLoader.Failure.LIMIT, assertThrows(SavedModelLoader.LoadException.class,
				() -> SavedModelLoader.load(file)).reason);
	}

	@Test
	public void actualReadsAndSkipsStayWithinBudget() throws Exception {
		try (var input = new SavedModelLoader.LimitedInput(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }), 3)) {
			assertEquals(1, input.read());
			assertEquals(1, input.skip(1));
			assertEquals(3, input.read());
			assertEquals(-1, input.read());
			assertEquals(0, input.skip(1));
		}
	}

	@Test
	public void killsWorkerThatDoesNotBecomeReady() throws Exception {
		final Process child = startProbe("sleep");
		assertEquals(SavedModelLoader.Failure.TIMEOUT, assertThrows(SavedModelLoader.LoadException.class,
				() -> SavedModelWorker.awaitReady(child, temporary.getRoot().toPath().resolve("ready"),
						Duration.ofMillis(100))).reason);
		assertFalse(child.isAlive());
	}

	@Test
	public void rejectsWorkerThatExitsWithoutReadiness() throws Exception {
		final Process child = startProbe("exit");
		assertEquals(SavedModelLoader.Failure.WORKER, assertThrows(SavedModelLoader.LoadException.class,
				() -> SavedModelWorker.awaitReady(child, temporary.getRoot().toPath().resolve("ready"),
						Duration.ofSeconds(10))).reason);
	}

	@Test
	public void containsAllocationFailureBeforeReadiness() throws Exception {
		final Process child = startProbe("heap");
		assertEquals(SavedModelLoader.Failure.WORKER, assertThrows(SavedModelLoader.LoadException.class,
				() -> SavedModelWorker.awaitReady(child, temporary.getRoot().toPath().resolve("ready"),
						Duration.ofSeconds(10))).reason);
		assertTrue(child.exitValue() != 0);
	}

	@Test
	public void rejectsReadinessObservedAfterTheDeadline() throws Exception {
		final Process child = startProbe("sleep");
		final Path ready = Files.createFile(temporary.newFile().toPath().resolveSibling("late-ready"));
		try {
			assertEquals(SavedModelLoader.Failure.TIMEOUT, assertThrows(SavedModelLoader.LoadException.class,
					() -> SavedModelWorker.awaitReady(child, ready, Duration.ZERO)).reason);
			assertFalse(child.isAlive());
		} finally {
			if (child.isAlive()) {
				child.destroyForcibly().waitFor();
			}
		}
	}

	@Test
	public void rejectsProgrammaticPolicyThatCannotCrossTheWorkerBoundary() throws Exception {
		final Process child = startProbe("policy");
		assertTrue(child.waitFor(15, java.util.concurrent.TimeUnit.SECONDS));
		assertEquals(0, child.exitValue());
	}

	private static Process startProbe(String mode) throws Exception {
		final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		return new ProcessBuilder(SavedModelWorker.javaExecutable(), "-Xmx32m", "-XX:+ExitOnOutOfMemoryError",
				"-cp", classpath, Probe.class.getName(), mode).redirectOutput(ProcessBuilder.Redirect.DISCARD)
						.redirectError(ProcessBuilder.Redirect.DISCARD).start();
	}

	public static class Probe {
		public static void main(String[] args) throws Exception {
			if ("sleep".equals(args[0])) {
				Thread.sleep(10_000);
			} else if ("policy".equals(args[0])) {
				ObjectInputFilter.Config.setSerialFilter(info -> REJECTED);
				assertEquals(SavedModelLoader.Failure.FILTER, assertThrows(SavedModelLoader.LoadException.class,
						() -> SADL.run(new String[] { "test", "-model", "missing.model" })).reason);
			} else if ("heap".equals(args[0])) {
				System.out.println(new byte[48 * 1024 * 1024].length);
			}
		}
	}

	private record Graph(long depth, long references, long arrayLength, long streamBytes) implements FilterInfo {
		@Override
		public Class<?> serialClass() {
			return null;
		}
	}
}
