/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

import sadl.input.TimedInput;
import sadl.run.datagenerators.SmacDataGenerator;
import sadl.run.datagenerators.Temp;

/**
 * 
 * @author Timo Klerx
 *
 */
public class IoUtils {
	private static Logger logger = LoggerFactory.getLogger(IoUtils.class);

	public static void deleteFiles(String[] strings) throws IOException {
		final Path[] paths = new Path[strings.length];
		for (int i = 0; i < strings.length; i++) {
			final Path p = Paths.get(strings[i]);
			paths[i] = p;
		}
		deleteFiles(paths);
	}

	public static List<Path> listFiles(Path directory, String fileEnding, boolean recursively) {
		final List<Path> result = new ArrayList<>();

		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (!attrs.isDirectory() && file.toString().endsWith(fileEnding)) {
						result.add(file);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
					if(recursively) {
						return super.preVisitDirectory(dir, attrs);
					} else {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
			});
		} catch (final IOException e) {
			logger.error("Unexpected exception occured.", e);
		}
		return result;
	}

	public static void runGraphviz(final Path gvFile, final Path pngFile) {
		final String[] args = { "dot", "-Tpng", gvFile.toAbsolutePath().toString(), "-o", pngFile.toAbsolutePath().toString() };
		Process pr = null;
		try {
			pr = Runtime.getRuntime().exec(args);
		} catch (final Exception e) {
			logger.warn("Could not create plot of Model: {}", e.getMessage());
		} finally {
			if (pr != null) {
				try {
					pr.waitFor();
				} catch (final InterruptedException e) {
				}
			}
		}
	}

	public static void deleteFiles(Path[] paths) throws IOException {
		for (final Path p : paths) {
			if (!Files.deleteIfExists(p)) {
				logger.warn("{} should have been explicitly deleted, but did not exist.", p);
			}
		}
	}

	public static void cleanDir(Path outputDir) throws IOException {
		if (Files.notExists(outputDir)) {
			Files.createDirectories(outputDir);
			return;
		}
		Files.walk(outputDir).filter(p -> !Files.isDirectory(p)).forEach(p -> {
			try {
				logger.info("Deleting file {}", p);
				Files.deleteIfExists(p);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
		Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				logger.info("Deleting file {}", file);
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			};

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (!dir.equals(outputDir)) {
					logger.info("Deleting directory {}", dir);
					Files.deleteIfExists(dir);
				}
				return FileVisitResult.CONTINUE;
			};
		});
	}

	public static Object deserialize(Path path) throws FileNotFoundException, IOException, ClassNotFoundException {
		try (FileInputStream fileIn = new FileInputStream(path.toFile()); ObjectInputStream in = new ObjectInputStream(fileIn)) {
			final Object o = in.readObject();
			return o;
		}
	}

	public static Pair<TimedInput, TimedInput> readTrainTestFile(Path trainTestFile, boolean skipFirstElement) {
		return readTrainTestFile(trainTestFile, (reader) -> {
			try {
				return TimedInput.parse(reader, skipFirstElement);
			} catch (final Exception e) {
				logger.error("Error while parsing train-test file {}", trainTestFile, e);
				throw new RuntimeException(e);
			}
		});
	}

	public static void writeTrainTestFile(Path trainTestFile, TimedInput trainSet, TimedInput testSet) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(trainTestFile, StandardCharsets.UTF_8)) {
			trainSet.toFile(bw, true);
			bw.write('\n');
			bw.write(Temp.TRAIN_TEST_SEP);
			bw.write('\n');
			testSet.toFile(bw, true);
		}
	}

	public static Pair<TimedInput, TimedInput> readTrainTestFile(Path trainTestFile) {
		return readTrainTestFile(trainTestFile, false);
	}

	public static Pair<TimedInput, TimedInput> readTrainTestFile(Path trainTestFile, Function<Reader, TimedInput> f) {
		try (BufferedReader br = Files.newBufferedReader(trainTestFile);
				PipedWriter trainWriter = new PipedWriter();
				PipedReader trainReader = new PipedReader(trainWriter);
				PipedWriter testWriter = new PipedWriter();
				PipedReader testReader = new PipedReader(testWriter)) {
			String line = "";
			final ExecutorService ex = Executors.newFixedThreadPool(2);
			final Future<TimedInput> trainWorker = ex.submit(() -> f.apply(trainReader));
			final Future<TimedInput> testWorker = ex.submit(() -> f.apply(testReader));
			ex.shutdown();
			boolean writeTrain = true;
			while ((line = br.readLine()) != null) {
				if (line.startsWith(SmacDataGenerator.TRAIN_TEST_SEP)) {
					writeTrain = false;
					trainWriter.close();
					continue;
				}
				if (writeTrain) {
					trainWriter.write(line);
					trainWriter.write('\n');
				} else {
					testWriter.write(line);
					testWriter.write('\n');
				}
			}
			testWriter.close();
			ex.shutdown();
			if (writeTrain) {
				trainWriter.close();
				ex.shutdownNow();
				throw new IOException("The provided file " + trainTestFile + " does not contain the separator " + SmacDataGenerator.TRAIN_TEST_SEP);
			}
			final Pair<TimedInput, TimedInput> result = Pair.of(trainWorker.get(), testWorker.get());
			return result;
		} catch (final IOException | InterruptedException | ExecutionException e) {
			logger.error("Unexpected exception!", e);
		}
		return null;
	}

	public static void serialize(Object o, Path path) throws IOException {
		try (OutputStream fileOut = Files.newOutputStream(path); ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
			out.writeObject(o);
			out.close();
		}
	}

	public static void writeToFile(double[] testSample, Path classificationTestFile) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(classificationTestFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
			bw.append(Arrays.toString(testSample).replace('[', ' ').replace(']', ' '));
			bw.append('\n');
		}

	}

	public static void writeToFile(List<double[]> testSamples, Path classificationTestFile) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(classificationTestFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
			for (final double[] testSample : testSamples) {
				bw.append(Arrays.toString(testSample).replace('[', ' ').replace(']', ' '));
				bw.append('\n');
			}
		}
	}

	public static Object xmlDeserialize(Path path) {
		try {
			final String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
			final XStream x = new XStream();
			return x.fromXML(xml);
		} catch (final Exception e) {
			logger.error("Unexpected exception", e);
		}
		return null;
	}

	public static void xmlSerialize(Object o, Path path) {
		String xml;
		try {
			final XStream x = new XStream();
			xml = x.toXML(o);
			Files.write(path, xml.getBytes());
		} catch (final Exception e) {
			logger.error("Unexpected exception", e);
		}
	}

}
