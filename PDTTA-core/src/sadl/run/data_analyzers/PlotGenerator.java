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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.opencsv.CSVReader;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class PlotGenerator {
	private static final boolean generatePlots = false;

	static String[] keys = { "pdfa", "tpta", "pdtta", "rti", "butla" };
	static String[] values = { "ALERGIA", "TPTA", "ProDTAL", "RTI+", "BUTLA" };

	public static void main(String[] args) throws IOException, InterruptedException {
		run(Paths.get(args[0]), Paths.get(args[1]), Paths.get(args[2]));
	}

	private static void run(Path input, Path template, Path outputDir) throws IOException, InterruptedException {
		final Set<String> generationTypes = new HashSet<>();
		final Set<String> anomalyTypes = new HashSet<>();
		final List<String> templateContent = Files.readAllLines(template);
		final TObjectDoubleMap<String> map = new TObjectDoubleHashMap<>();
		final NumberFormat formatter = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		try (CSVReader reader = new CSVReader(Files.newBufferedReader(input))) {
			reader.readNext();
			String[] line = null;
			String generationType = null; // direct, random, tpta(-prep).
			String anomalyType = null;
			while ((line = reader.readNext()) != null) {
				final String algo = line[0];
				final String file = line[1];
				final double perf = Double.parseDouble(line[2]);
				final int lastSlashPos = file.lastIndexOf('/');
				final int secondLastSlashPos = file.lastIndexOf('/', lastSlashPos - 1);
				anomalyType = file.substring(lastSlashPos + 1);
				if (anomalyType.equals("A-mixed")) {
					anomalyType = "mixed";
				}
				generationType = file.substring(secondLastSlashPos + 1, lastSlashPos);
				generationTypes.add(generationType);
				anomalyTypes.add(anomalyType);
				map.put(algo + generationType + anomalyType, perf);
			}
		}
		final String[] genTypes = generationTypes.toArray(new String[0]);
		Arrays.sort(genTypes);
		for (final String generationType : genTypes) {
			try (BufferedWriter bw = Files.newBufferedWriter(outputDir.resolve(generationType + ".perf"))) {
				for (final String line : templateContent) {
					bw.append(line);
					bw.append('\n');
				}
				final String[] anoTypes = anomalyTypes.toArray(new String[0]);
				Arrays.sort(anoTypes);
				for (final String anomalyType : anoTypes) {
					final String testQualifier = keys[0] + generationType + anomalyType;
					if (!map.containsKey(testQualifier)) {
						continue;
					}
					bw.append(anomalyType);
					for (int i = 0; i < keys.length; i++) {
						final String qualifier = keys[i] + generationType + anomalyType;
						bw.append('\t');
						bw.append(formatter.format(map.get(qualifier)));
					}
					bw.append('\n');
				}
			}
		}
		if (generatePlots) {
			RecursivePlot.plot(outputDir);
		}
	}
}
