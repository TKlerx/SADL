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
package sadl.run.data_analyzers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import jsat.math.OnLineStatistics;
import jsat.utils.Pair;

public class SmacResultAnalyzer {
	static char csvSeparator = ';';
	public static void main(String[] args) throws IOException {
		final Path inputDir = Paths.get(args[0]);
		final Path outputFile = Paths.get(args[1]);
		final List<Path> inputFiles = new ArrayList<>();
		Files.walk(inputDir).filter(p -> !Files.isDirectory(p)).forEach(p -> {
			try {
				inputFiles.add(p);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
		processFiles(inputFiles, outputFile);
	}

	static boolean wroteHeadline = false;
	static String[] firstHeadline = null;
	static Map<String, Pair<String, List<OnLineStatistics>>> results = new HashMap<>();
	private static void processFiles(List<Path> inputFiles, Path outputFile) throws IOException {
		String[] headline = null;
		try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(outputFile), csvSeparator)) {
			for (final Path p : inputFiles) {
				try (CSVReader reader = new CSVReader(Files.newBufferedReader(p), csvSeparator)) {
					if (!wroteHeadline) {
						headline = reader.readNext();
						firstHeadline = headline;
						final List<String> newHeadline = new ArrayList<>(Arrays.asList(headline));
						newHeadline.set(0, "algorithm");
						newHeadline.add(1, "numberOfRuns");
						writer.writeNext(newHeadline.toArray(new String[0]));
						wroteHeadline = true;
					} else {
						// skip headLine
						final String[] newHeadLine = reader.readNext();
						if (!Arrays.equals(newHeadLine, firstHeadline)) {
							throw new IllegalStateException("CSV Headlines do not match. Found a new one in File " + p);
						}
					}
					String[] line = null;
					while ((line = reader.readNext()) != null) {
						// final String time = line[0];
						final String configs = line[1];
						final String[] config = configs.split("\\s*,\\s*");
						final String algoName = config[1];
						final int paramStartIndex = 7;
						final SortedMap<String, String> map = new TreeMap<>();
						for (int i = paramStartIndex; i < config.length; i += 2) {
							map.put(config[i], config[i + 1]);
						}

						final StringBuilder sb = new StringBuilder();
						sb.append(algoName);
						sb.append(',');
						for (final String s : map.keySet()) {
							sb.append(s);
							sb.append('=');
							sb.append(map.get(s));
							sb.append(',');
						}
						sb.deleteCharAt(sb.length() - 1);
						final String qualifier = sb.toString();


						final int qualityStartIndex = 4;
						final int metricStartIndex = qualityStartIndex - 2;
						if (results.get(qualifier) == null) {
							final List<OnLineStatistics> list = new ArrayList<>();
							for (int i = metricStartIndex; i < line.length; i++) {
								list.add(new OnLineStatistics());
							}
							results.put(qualifier, new Pair<>(algoName, list));
						}
						final List<OnLineStatistics> list = results.get(qualifier).getSecondItem();
						final String trainTimeString = line[2].substring(line[2].indexOf('(') + 1, line[2].indexOf(')'));
						final String testTimeString = line[3].substring(line[3].indexOf('(') + 1, line[3].indexOf(')'));
						final double trainTimeMinutes = Integer.parseInt(trainTimeString) / 60000.0;
						final double testTimeMinutes = Integer.parseInt(testTimeString) / 60000.0;
						list.get(0).add(trainTimeMinutes);
						list.get(1).add(testTimeMinutes);

						for (int i = qualityStartIndex; i < line.length; i++) {
							final double qualityValue = Double.parseDouble(line[i]);
							if (!Double.isInfinite(qualityValue) && !Double.isNaN(qualityValue)) {
								list.get(i - metricStartIndex).add(qualityValue);
							}
						}
					}
				}
			}
			for (final String qualifier : results.keySet()) {
				final List<String> newLine = new ArrayList<>();
				final Pair<String, List<OnLineStatistics>> pair = results.get(qualifier);
				newLine.add(pair.getFirstItem());
				final List<OnLineStatistics> statistics = pair.getSecondItem();
				newLine.add(Double.toString(statistics.get(0).getSumOfWeights()));
				newLine.add(qualifier);
				for (final OnLineStatistics stat : statistics) {
					if (stat.getSumOfWeights() != 0) {
						newLine.add(Double.toString(stat.getMean()));
					} else {
						Double.toString(Double.NaN);
					}
				}
				writer.writeNext(newLine.toArray(new String[0]));
			}
		}

	}
}
