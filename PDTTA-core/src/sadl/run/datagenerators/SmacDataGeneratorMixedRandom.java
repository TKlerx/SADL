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
package sadl.run.datagenerators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public class SmacDataGeneratorMixedRandom implements Serializable {

	public static final String TRAIN_TEST_SEP = "?????????????????????????";
	private static final double ANOMALY_PERCENTAGE = 0.1;
	private static final int TRAIN_SIZE = 10000;
	private static final int TEST_SIZE = 5000;
	private static final int SAMPLE_FILES = 11;

	private static Logger logger = LoggerFactory.getLogger(SmacDataGeneratorMixedRandom.class);

	private static final long serialVersionUID = -6230657726489919272L;

	String dataString;

	Path outputDir = Paths.get("output");

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final SmacDataGeneratorMixedRandom sp = new SmacDataGeneratorMixedRandom();
		sp.dataString = args[0];
		logger.info("Running {} with args={}", sp.getClass().getSimpleName(), Arrays.toString(args));
		sp.run();
	}

	private void run() throws IOException, InterruptedException {
		IoUtils.cleanDir(outputDir);
		int k = 0;
		// parse timed sequences
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(Paths.get(dataString), 1);
		final Random r = MasterSeed.nextRandom();
		while (k < SAMPLE_FILES) {
			final ArrayList<TimedWord> trainSequences = new ArrayList<>();
			final ArrayList<TimedWord> testSequences = new ArrayList<>();
			final DecimalFormat df = new DecimalFormat("00");
			// final Path p = Paths.get("pta_normal.dot");
			// pta.toGraphvizFile(outputDir.resolve(p), false);
			// final Process ps = Runtime.getRuntime().exec("dot -Tpdf -O " + outputDir.resolve(p));
			// System.out.println(outputDir.resolve(p));
			// ps.waitFor();
			final TIntSet chosenSequences = new TIntHashSet();
			while (trainSequences.size() < TRAIN_SIZE) {
				int chosenIndex = r.nextInt(trainingTimedSequences.size());
				while (chosenSequences.contains(chosenIndex)) {
					chosenIndex = r.nextInt(trainingTimedSequences.size());
				}
				trainSequences.add(trainingTimedSequences.get(chosenIndex));
				chosenSequences.add(chosenIndex);
			}
			while (testSequences.size() < TEST_SIZE) {
				int chosenIndex = r.nextInt(trainingTimedSequences.size());
				while (chosenSequences.contains(chosenIndex)) {
					chosenIndex = r.nextInt(trainingTimedSequences.size());
				}
				testSequences.add(trainingTimedSequences.get(chosenIndex));
				chosenSequences.add(chosenIndex);
			}
			for (final AnomalyInsertionType type : AnomalyInsertionType.values()) {
				if (type != AnomalyInsertionType.NONE) {
					logger.info("inserting Anomaly Type {}", type);
					final List<TimedWord> trainSequenceClone = ((List<TimedWord>) trainSequences.clone());
					final List<TimedWord> testSequenceClone = ((List<TimedWord>) testSequences.clone());
					final TimedInput trainSet = new TimedInput(trainSequenceClone);
					TimedInput testSet = new TimedInput(testSequenceClone);
					testSet = testSet.insertRandomAnomalies(type, ANOMALY_PERCENTAGE);
					final int typeIndex = type.getTypeIndex();
					final String typeFolderString = typeIndex == AnomalyInsertionType.ALL.getTypeIndex() ? "mixed" : "type" + typeIndex;

					final Path outputFile = outputDir.resolve(Paths.get("random-" + df.format(k) + "_smac_" + typeFolderString + ".txt"));
					try (BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
						trainSet.toFile(bw, true);
						bw.write('\n');
						bw.write(TRAIN_TEST_SEP);
						bw.write('\n');
						testSet.toFile(bw, true);
						logger.info("Wrote file #{} ({})", k, outputFile);
					}
				}
				k++;
			}
		}
	}

}
