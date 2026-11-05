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

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import sadl.run.SavedModelLoader.Failure;
import sadl.run.SavedModelLoader.LoadException;

public final class SavedModelWorker {
	private static final int HEAP_MIB = 512;
	private static final int LOAD_FAILURE_BASE = 20;
	private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(30);
	static final Budget DEFAULT = new Budget(HEAP_MIB, LOAD_TIMEOUT);

	record Budget(int heapMib, Duration timeout, long streamBytes) {
		Budget(int heapMib, Duration timeout) {
			this(heapMib, timeout, SavedModelLoader.MAX_BYTES);
		}

		Budget {
			if (heapMib < 1 || heapMib > HEAP_MIB || timeout.isNegative() || timeout.compareTo(LOAD_TIMEOUT) > 0
					|| streamBytes < 4 || streamBytes > SavedModelLoader.MAX_BYTES) {
				throw new IllegalArgumentException("Invalid worker budget");
			}
		}
	}

	private SavedModelWorker() {
	}

	static void evaluate(String[] cliArguments, Budget budget) throws IOException {
		final String filter = transferableFilter();
		final Path statusDirectory = Files.createTempDirectory("sadl-model-");
		final Path ready = statusDirectory.resolve("ready");
		Process child = null;
		try {
			final List<String> command = new ArrayList<>(List.of(javaExecutable(), "-Xmx" + budget.heapMib() + "m",
					"-XX:+ExitOnOutOfMemoryError"));
			if (filter != null) {
				command.add("-Djdk.serialFilter=" + filter);
			}
			final String securityProperties = System.getProperty("java.security.properties");
			if (securityProperties != null) {
				command.add("-Djava.security.properties=" + securityProperties);
			}
			command.addAll(List.of("-cp", System.getProperty("java.class.path"), SavedModelWorker.class.getName(),
					ready.toString(), Long.toString(budget.streamBytes())));
			command.addAll(List.of(cliArguments));
			final long deadline = System.nanoTime() + budget.timeout().toNanos();
			child = new ProcessBuilder(command).inheritIO().start();
			awaitReadyUntil(child, ready, deadline);
			Files.createFile(acknowledgment(ready));
			if (child.waitFor() != 0) {
				throw new IOException("Saved-model evaluation worker failed");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Saved-model evaluation interrupted", e);
		} finally {
			if (child != null && child.isAlive()) {
				child.destroyForcibly().onExit().join();
			}
			Files.deleteIfExists(acknowledgment(ready));
			Files.deleteIfExists(ready);
			Files.deleteIfExists(statusDirectory);
		}
	}

	private static String transferableFilter() throws LoadException {
		try {
			final Class<?> factory = ObjectInputFilter.Config.getSerialFilterFactory().getClass();
			// A custom factory can close over process-local state. The child cannot reproduce it.
			if (factory.getModule() != ObjectInputFilter.class.getModule()
					|| !factory.getName().equals("java.io.ObjectInputFilter$Config$BuiltinFilterFactory")) {
				throw new LoadException(Failure.FILTER);
			}
			final ObjectInputFilter current = ObjectInputFilter.Config.getSerialFilter();
			if (current == null) {
				return null;
			}
			if (current.getClass() != ObjectInputFilter.Config.createFilter("!*").getClass()) {
				throw new LoadException(Failure.FILTER);
			}
			return current.toString();
		} catch (IllegalStateException | SecurityException e) {
			throw new LoadException(Failure.FILTER);
		}
	}

	static void awaitReady(Process child, Path ready, Duration timeout) throws InterruptedException, LoadException {
		awaitReadyUntil(child, ready, System.nanoTime() + timeout.toNanos());
	}

	private static void awaitReadyUntil(Process child, Path ready, long deadline) throws InterruptedException, LoadException {
		while (true) {
			final boolean readyObserved = Files.exists(ready);
			final long remaining = deadline - System.nanoTime();
			if (remaining <= 0) {
				child.destroyForcibly().waitFor();
				throw new LoadException(Failure.TIMEOUT);
			}
			if (readyObserved) {
				return;
			}
			if (child.waitFor(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(20)), TimeUnit.NANOSECONDS)) {
				final int index = child.exitValue() - LOAD_FAILURE_BASE;
				final Failure[] failures = Failure.values();
				throw new LoadException(index >= 0 && index < failures.length ? failures[index] : Failure.WORKER);
			}
		}
	}

	private static Path acknowledgment(Path ready) {
		return ready.resolveSibling("accepted");
	}

	public static void confirmLoaded(Path ready) throws IOException {
		final long deadline = System.nanoTime() + LOAD_TIMEOUT.toNanos();
		Files.createFile(ready);
		while (!Files.exists(acknowledgment(ready))) {
			if (deadline - System.nanoTime() <= 0) {
				throw new LoadException(Failure.TIMEOUT);
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new LoadException(Failure.WORKER);
			}
		}
	}

	static String javaExecutable() {
		final Path bin = Path.of(System.getProperty("java.home"), "bin");
		final Path java = bin.resolve("java");
		return (Files.isRegularFile(java) ? java : bin.resolve("java.exe")).toString();
	}

	public static void main(String[] args) throws IOException {
		try {
			if (Runtime.getRuntime().maxMemory() > HEAP_MIB * 1024L * 1024) {
				throw new LoadException(Failure.WORKER);
			}
			SADL.run(java.util.Arrays.copyOfRange(args, 2, args.length), Path.of(args[0]),
					new Budget(HEAP_MIB, LOAD_TIMEOUT, Long.parseLong(args[1])));
		} catch (LoadException e) {
			System.exit(LOAD_FAILURE_BASE + e.reason.ordinal());
		}
	}
}
