/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

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

/**
 * 
 * @author Timo Klerx
 *
 */
public class PropertiesWriter {
	public static final String TIME_THRESHOLD = "timeThreshold";
	public static final String EVENT_THRESHOLD = "eventThreshold";
	public static final String ANOMALY_INSERTION_TYPE = "anomalyInsertionType";
	public static final String AGGREGATION_TYPE = "aggType";
	public static final String TEMP_FILE_PREFIX = "tempFilePrefix";
	public static final String TIMED_INPUT_FILE = "timedInputFile";
	public static final String JOB_NAME = "jobName";
	public static final String MERGE_ALPHA = "mergeAlpha";
	public static final String RECURSIVE_MERGE_TEST = "recursiveMergeTest";
	public static final String MERGE_TEST = "mergeTest";
	public static final String RESULT_FOLDER = "results";
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
				try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(configDir, "*")) {
					for (final Path p : dirStream) {
						Files.delete(p);
					}
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
								prop.setProperty(AGGREGATION_TYPE, ProbabilityAggregationMethod.NORMALIZED_MULTIPLY.toString());
								prop.setProperty(ANOMALY_INSERTION_TYPE, anomalyInsertionType.toString());
								prop.setProperty(EVENT_THRESHOLD, Arrays.toString(eventThresholds));
								prop.setProperty(JOB_NAME, Integer.toString(jobNameCount) + "_");
								prop.setProperty(MERGE_ALPHA, Double.toString(mergeAlpha));
								prop.setProperty(MERGE_TEST, mergeTest.toString());
								prop.setProperty(RECURSIVE_MERGE_TEST, Boolean.toString(recursiveMergeTest));
								prop.setProperty(TEMP_FILE_PREFIX, "treba_temp_");
								prop.setProperty(TIME_THRESHOLD, Arrays.toString(timeThresholds));
								prop.setProperty(TIMED_INPUT_FILE, "rti_input.txt");
								prop.setProperty(RESULT_FOLDER, "results");
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
