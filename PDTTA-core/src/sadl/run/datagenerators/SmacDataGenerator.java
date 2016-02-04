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
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.modellearner.TauPtaLearner;
import sadl.models.TauPTA;
import sadl.models.pta.Event;
import sadl.utils.MasterSeed;

/**
 * 
 * @author Timo Klerx
 *
 */
public class SmacDataGenerator implements Serializable {
	public static final String TRAIN_TEST_SEP = "?????????????????????????";

	private static Logger logger = LoggerFactory.getLogger(SmacDataGenerator.class);

	private static final long serialVersionUID = -6230657726489919272L;

	String dataString;

	Path outputDir = Paths.get("output");
	private static final double ANOMALY_PERCENTAGE = 0.1;
	private static final int TRAIN_SIZE = 10000;
	private static final int TEST_SIZE = 5000;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final SmacDataGenerator sp = new SmacDataGenerator();
		sp.dataString = args[0];
		logger.info("Running {} with args={}", sp.getClass().getSimpleName(), Arrays.toString(args));
		sp.run();
	}

	private void run() throws IOException, InterruptedException {
		if (Files.notExists(outputDir)) {
			Files.createDirectories(outputDir);
		}
		Files.walk(outputDir).filter(p -> !Files.isDirectory(p)).forEach(p -> {
			try {
				logger.info("Deleting file {}", p);
				Files.delete(p);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
		int k = 0;
		final boolean splitTimedEvents = true;
		// parse timed sequences
		TimedInput trainingTimedSequences = TimedInput.parseAlt(Paths.get(dataString), 1);
		if (splitTimedEvents) {
			final ButlaPdtaLearner butla = new ButlaPdtaLearner(10000, EventsCreationStrategy.SplitEvents, KDEFormelVariant.OriginalKDE);
			final Pair<TimedInput, Map<String, Event>> p = butla.splitEventsInTimedSequences(trainingTimedSequences);
			trainingTimedSequences = p.getKey();
		}
		final Random r = MasterSeed.nextRandom();
		final List<TimedWord> trainSequences = new ArrayList<>();
		final List<TimedWord> testSequences = new ArrayList<>();
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		final TauPTA typeTwoNormalPta = SerializationUtils.clone(pta);
		final DecimalFormat df = new DecimalFormat("00");
		// final Path p = Paths.get("pta_normal.dot");
		// pta.toGraphvizFile(outputDir.resolve(p), false);
		// final Process ps = Runtime.getRuntime().exec("dot -Tpdf -O " + outputDir.resolve(p));
		// System.out.println(outputDir.resolve(p));
		// ps.waitFor();
		logger.info("Finished TauPTA ({} states) creation.", pta.getStateCount());
		TauPTA currentPta;
		while (k < 54) {
			for (final AnomalyInsertionType type : AnomalyInsertionType.values()) {
				if (type != AnomalyInsertionType.NONE && type != AnomalyInsertionType.ALL) {
					// if (type != AnomalyInsertionType.TYPE_TWO) {
					// continue;
					// }
					if (type == AnomalyInsertionType.TYPE_TWO) {
						currentPta = SerializationUtils.clone(typeTwoNormalPta);
						currentPta.setRandom(MasterSeed.nextRandom());
					} else {
						currentPta = pta;
					}
					trainSequences.clear();
					testSequences.clear();
					final TauPTA anomaly = SerializationUtils.clone(currentPta);
					logger.info("inserting Anomaly Type {}", type);
					anomaly.makeAbnormal(type);
					if (type == AnomalyInsertionType.TYPE_TWO) {
						anomaly.removeAbnormalSequences(currentPta);
					}
					for (int i = 0; i < TRAIN_SIZE; i++) {
						trainSequences.add(currentPta.sampleSequence());
					}
					// PTAs of Type 2 and 4 always produce abnormal sequences
					// it is possible to sample abnormal and normal sequences with abnormal ptas of the other types (1,3,5).
					// but I don't know how the distribution is, so to be fair, i sample all anomalies the same
					for (int i = 0; i < TEST_SIZE; i++) {
						if (r.nextDouble() < ANOMALY_PERCENTAGE) {
							boolean wasAnormal = false;
							TimedWord seq = null;
							while (!wasAnormal) {
								seq = anomaly.sampleSequence();
								wasAnormal = seq.isAnomaly();
							}
							testSequences.add(seq);
						} else {
							testSequences.add(currentPta.sampleSequence());
						}
					}
					final TimedInput trainset = new TimedInput(trainSequences);
					final TimedInput testset = new TimedInput(testSequences);
					final Path outputFile = outputDir.resolve(Paths.get(df.format(k) + "_smac_type" + type.getTypeIndex() + ".txt"));
					try (BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
						trainset.toFile(bw, true);
						bw.write('\n');
						bw.write(TRAIN_TEST_SEP);
						bw.write('\n');
						testset.toFile(bw, true);
					}
					logger.info("Wrote file #{} ({})", k, outputFile);
					k++;
				}
			}
		}
	}

}
