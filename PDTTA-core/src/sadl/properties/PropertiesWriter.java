/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package sadl.properties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.MergeTest;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.run.Pipeline;


@SuppressWarnings("deprecation")
public class PropertiesWriter {
	static boolean deleteOldFiles = true;

	public static void main(String[] args) {

		try {
			final double[] mergeAlphas = new double[] { 0.0001, 0.001, 0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
			final MergeTest[] mergeTests = new MergeTest[] { MergeTest.ALERGIA };
			final double[] timeThresholds = new double[] { 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001, 1E-7, 1E-8, 1E-9, 1E-10, 1E-11, 1E-12, 0, Double.NaN };
			final double[] eventThresholds = new double[] { 0, 1E-10, 1E-9, 1E-8, 1E-7, 1E-6, 1E-5, 1E-4, 0.001, 0.005, 0.01, 0.1, 0.2, 0.3, .4, .5, .6, .7 };
			final AnomalyInsertionType[] anomalyInsertionTypes = new AnomalyInsertionType[] { AnomalyInsertionType.ALL, AnomalyInsertionType.TYPE_FOUR,
					AnomalyInsertionType.TYPE_ONE, AnomalyInsertionType.TYPE_THREE, AnomalyInsertionType.TYPE_TWO };
			final boolean[] recursiveMergeTests = new boolean[] { true, false };
			final Path configDir = Paths.get("properties");
			if (!Files.exists(configDir)) {
				Files.createDirectory(configDir);
			}
			if (deleteOldFiles) {
				final DirectoryStream<Path> dirStream = Files.newDirectoryStream(configDir, "*");
				for (final Path p : dirStream) {
					Files.delete(p);
				}
			}
			int tempIndex = 0;
			int jobNameCount = 0;
			for (final double mergeAlpha : mergeAlphas) {
				for (final MergeTest mergeTest : mergeTests) {
					for (final AnomalyInsertionType anomalyInsertionType : anomalyInsertionTypes) {
						for (final boolean recursiveMergeTest : recursiveMergeTests) {
							Path p = configDir.resolve(jobNameCount + tempIndex + ".properties");
							while (Files.exists(p)) {
								tempIndex++;
								p = configDir.resolve(jobNameCount + tempIndex + ".properties");
							}
							try (OutputStream output = new FileOutputStream(p.toFile())) {
								final Properties prop = new Properties();
								prop.setProperty(Pipeline.AGGREGATION_TYPE, ProbabilityAggregationMethod.NORMALIZED_MULTIPLY.toString());
								prop.setProperty(Pipeline.ANOMALY_INSERTION_TYPE, anomalyInsertionType.toString());
								prop.setProperty(Pipeline.EVENT_THRESHOLD, Arrays.toString(eventThresholds));
								prop.setProperty(Pipeline.JOB_NAME, Integer.toString(jobNameCount) + "_");
								prop.setProperty(Pipeline.MERGE_ALPHA, Double.toString(mergeAlpha));
								prop.setProperty(Pipeline.MERGE_TEST, mergeTest.toString());
								prop.setProperty(Pipeline.RECURSIVE_MERGE_TEST, Boolean.toString(recursiveMergeTest));
								prop.setProperty(Pipeline.TEMP_FILE_PREFIX, "treba_temp_");
								prop.setProperty(Pipeline.TIME_THRESHOLD, Arrays.toString(timeThresholds));
								prop.setProperty(Pipeline.TIMED_INPUT_FILE, "rti_input.txt");
								prop.setProperty(Pipeline.RESULT_FOLDER, "results");
								prop.store(output, null);
								output.close();
								jobNameCount++;
							}
						}
					}
				}
			}
			System.out.println("Created " + jobNameCount + " properties files");
		} catch (final IOException io) {
			io.printStackTrace();
		}
	}
}
