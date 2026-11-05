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

import static java.io.ObjectInputFilter.Status.ALLOWED;
import static java.io.ObjectInputFilter.Status.REJECTED;
import static java.io.ObjectInputFilter.Status.UNDECIDED;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import sadl.interfaces.ProbabilisticModel;
import sadl.models.PDTTA;
import sadl.models.pdrta.PDRTA;

public final class SavedModelLoader {
	static final long MAX_BYTES = 16L * 1024 * 1024;
	private static final Set<String> ROOTS = Set.of(PDRTA.class.getName(), PDTTA.class.getName());

	// Exact descriptors from trusted CLI captures and the frozen RTI reference.
	private static final Set<String> DESCRIPTORS = Set.of(
			"[D",
			"[I",
			"[Ljava.util.concurrent.ConcurrentHashMap$Segment;",
			"[Lsadl.integration.MonteCarloPoint;",
			"com.google.common.collect.AbstractMapBasedMultimap",
			"com.google.common.collect.AbstractSetMultimap",
			"com.google.common.collect.AbstractSortedKeySortedSetMultimap",
			"com.google.common.collect.AbstractSortedSetMultimap",
			"com.google.common.collect.NaturalOrdering",
			"com.google.common.collect.TreeMultimap",
			"gnu.trove.impl.hash.THash",
			"gnu.trove.impl.hash.TIntDoubleHash",
			"gnu.trove.impl.hash.TIntHash",
			"gnu.trove.impl.hash.TIntIntHash",
			"gnu.trove.impl.hash.TObjectHash",
			"gnu.trove.impl.hash.TPrimitiveHash",
			"gnu.trove.list.array.TIntArrayList",
			"gnu.trove.list.linked.TIntLinkedList",
			"gnu.trove.map.hash.TIntDoubleHashMap",
			"gnu.trove.map.hash.TIntIntHashMap",
			"gnu.trove.map.hash.TIntObjectHashMap",
			"gnu.trove.map.hash.TObjectDoubleHashMap",
			"gnu.trove.map.hash.TObjectIntHashMap",
			"gnu.trove.set.hash.TIntHashSet",
			"java.lang.Enum",
			"java.lang.Integer",
			"java.lang.Number",
			"java.util.ArrayList",
			"java.util.Collections$SynchronizedMap",
			"java.util.HashMap",
			"java.util.HashSet",
			"java.util.Random",
			"java.util.TreeMap",
			"java.util.concurrent.ConcurrentHashMap",
			"java.util.concurrent.ConcurrentHashMap$Segment",
			"java.util.concurrent.locks.AbstractOwnableSynchronizer",
			"java.util.concurrent.locks.AbstractQueuedSynchronizer",
			"java.util.concurrent.locks.ReentrantLock",
			"java.util.concurrent.locks.ReentrantLock$NonfairSync",
			"java.util.concurrent.locks.ReentrantLock$Sync",
			"jsat.distributions.ContinuousDistribution",
			"jsat.distributions.Distribution",
			"jsat.distributions.SingleValueDistribution",
			"jsat.distributions.empirical.MyKernelDensityEstimator",
			"jsat.distributions.empirical.MyKernelDensityEstimator$1",
			"jsat.distributions.empirical.kernelfunc.BiweightKF",
			"jsat.distributions.empirical.kernelfunc.EpanechnikovKF",
			"jsat.distributions.empirical.kernelfunc.GaussKF",
			"jsat.distributions.empirical.kernelfunc.TriweightKF",
			"jsat.distributions.empirical.kernelfunc.UniformKF",
			"sadl.constants.ClassLabel",
			"sadl.input.TimedInput",
			"sadl.input.TimedWord",
			"sadl.integration.MonteCarloIntegration",
			"sadl.integration.MonteCarloPoint",
			"sadl.models.PDFA",
			"sadl.models.PDTTA",
			"sadl.models.pdrta.Interval",
			"sadl.models.pdrta.PDRTA",
			"sadl.models.pdrta.PDRTAInput",
			"sadl.models.pdrta.PDRTAState",
			"sadl.models.pdrta.StateStatistic",
			"sadl.structure.Transition",
			"sadl.structure.ZeroProbTransition",
			"sadl.tau_estimation.IdentityEstimator",
			"sadl.tau_estimation.MonteCarloEstimator"
	);
	private static final Set<String> SCRATCH_ARRAYS = Set.of("[Ljava.lang.Object;", "[Ljava.util.Map$Entry;");

	private SavedModelLoader() {
	}

	public enum Failure {
		FILE("expected a readable regular file"),
		LIMIT("model exceeds a loading limit"),
		STREAM_LIMIT("opened model stream exceeds the read limit"),
		CONTENT("unsupported model type or contents"),
		MALFORMED("invalid or truncated model"),
		FILTER("required serialization filter is unavailable"),
		TIMEOUT("loading deadline exceeded"),
		WORKER("loading worker failed"),
		MISSING_PDRTA_ROOT("missing PDRTA root state");

		final String message;

		Failure(String message) {
			this.message = message;
		}
	}

	public static final class LoadException extends IOException {
		private static final long serialVersionUID = 1L;
		final Failure reason;

		LoadException(Failure reason) {
			super(reason.message);
			this.reason = reason;
		}
	}

	public static ProbabilisticModel load(Path path) throws LoadException {
		return load(path, MAX_BYTES);
	}

	public static ProbabilisticModel load(Path path, long streamBytes) throws LoadException {
		if (streamBytes < 4 || streamBytes > MAX_BYTES) {
			throw new IllegalArgumentException("Invalid model stream budget");
		}
		if (path == null || !Files.isRegularFile(path)) {
			throw new LoadException(Failure.FILE);
		}
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			if (channel.size() > MAX_BYTES) {
				throw new LoadException(Failure.LIMIT);
			}
			final LimitedInput stream = new LimitedInput(Channels.newInputStream(channel), streamBytes);
			try (ModelInput input = new ModelInput(stream)) {
				final ObjectInputFilter existing = input.getObjectInputFilter();
				final ObjectInputFilter local = info -> {
					if (exceedsLimits(info)) {
						input.limitExceeded = true;
					}
					return filter(info);
				};
				final ObjectInputFilter policy = existing == null ? local : ObjectInputFilter.merge(local, existing);
				try {
					input.setObjectInputFilter(policy);
				} catch (IllegalStateException | SecurityException e) {
					throw new LoadException(Failure.FILTER);
				}
				if (input.getObjectInputFilter() != policy) {
					throw new LoadException(Failure.FILTER);
				}

				final Object model;
				try {
					model = input.readObject();
				} catch (InvalidClassException e) {
					throw new LoadException(input.limitExceeded ? Failure.LIMIT : Failure.CONTENT);
				} catch (IOException e) {
					if (stream.remaining == 0 && channel.size() > streamBytes) {
						throw new LoadException(Failure.STREAM_LIMIT);
					}
					throw e;
				}
				if (channel.size() > MAX_BYTES) {
					throw new LoadException(Failure.LIMIT);
				}
				if (model == null || !ROOTS.contains(model.getClass().getName())) {
					throw new LoadException(Failure.CONTENT);
				}
				if (model instanceof PDRTA pdrta && pdrta.getRoot() == null) {
					throw new LoadException(Failure.MISSING_PDRTA_ROOT);
				}
				return (ProbabilisticModel) model;
			} catch (LoadException e) {
				throw e;
			} catch (ClassNotFoundException | IOException | RuntimeException e) {
				throw new LoadException(Failure.MALFORMED);
			}
		} catch (LoadException e) {
			throw e;
		} catch (IOException | SecurityException e) {
			throw new LoadException(Failure.FILE);
		}
	}

	private static boolean exceedsLimits(ObjectInputFilter.FilterInfo info) {
		return info.depth() > 64 || info.references() > 250_000 || info.arrayLength() > 250_000
				|| info.streamBytes() > MAX_BYTES;
	}

	static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
		if (exceedsLimits(info)) {
			return REJECTED;
		}
		if (info.serialClass() == null) {
			return UNDECIDED;
		}
		final String type = info.serialClass().getName();
		return DESCRIPTORS.contains(type) || SCRATCH_ARRAYS.contains(type) ? ALLOWED : REJECTED;
	}

	private static final class ModelInput extends ObjectInputStream {
		private boolean firstDescriptor = true;
		private boolean limitExceeded;

		ModelInput(InputStream input) throws IOException {
			super(input);
		}

		@Override
		protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
			final ObjectStreamClass descriptor = super.readClassDescriptor();
			final String type = descriptor.getName();
			if (!DESCRIPTORS.contains(type) || (firstDescriptor && !ROOTS.contains(type))) {
				throw new InvalidClassException("unsupported model descriptor");
			}
			firstDescriptor = false;
			return descriptor;
		}

		@Override
		protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
			throw new InvalidClassException("proxy descriptors are unsupported");
		}
	}

	static final class LimitedInput extends FilterInputStream {
		private long remaining;

		LimitedInput(InputStream input, long limit) {
			super(input);
			remaining = limit;
		}

		@Override
		public int read() throws IOException {
			if (remaining == 0) {
				return -1;
			}
			final int value = in.read();
			if (value >= 0) {
				remaining--;
			}
			return value;
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (length == 0) {
				return 0;
			}
			if (remaining == 0) {
				return -1;
			}
			final int count = in.read(bytes, offset, (int) Math.min(remaining, length));
			if (count > 0) {
				remaining -= count;
			}
			return count;
		}

		@Override
		public long skip(long count) throws IOException {
			final long skipped = in.skip(Math.min(remaining, count));
			remaining -= skipped;
			return skipped;
		}
	}
}
