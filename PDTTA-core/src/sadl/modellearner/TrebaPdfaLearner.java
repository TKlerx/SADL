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

package sadl.modellearner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.utils.IoUtils;
import sadl.utils.Settings;
import treba.observations;
import treba.treba;
import treba.wfsa;

public class TrebaPdfaLearner implements PdfaLearner {

	protected double mergeAlpha;
	protected MergeTest mergeTest = getDefaultMergeTest();
	protected boolean recursiveMergeTest;
	private static Logger logger = LoggerFactory.getLogger(TrebaPdfaLearner.class);
	protected double smoothingPrior = 0.00;
	protected int mergeT0 = 3;

	public TrebaPdfaLearner(double mergeAlpha, boolean recursiveMergeTest) {
		this.mergeAlpha = mergeAlpha;
		this.recursiveMergeTest = recursiveMergeTest;
	}

	private MergeTest getDefaultMergeTest() {
		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux")) {
			return MergeTest.ALERGIA;
		} else if (osName.toLowerCase().contains("windows")) {
			return null;
		}
		return null;
	}

	public TrebaPdfaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest) {
		this(mergeAlpha, recursiveMergeTest);
		this.mergeTest = mergeTest;
	}

	public TrebaPdfaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest, double smoothingPrior, int mergeT0) {
		this(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior);
		this.mergeT0 = mergeT0;

	}

	public TrebaPdfaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest, double smoothingPrior) {
		this(mergeAlpha, recursiveMergeTest, mergeTest);
		this.smoothingPrior = smoothingPrior;
	}

	@Override
	public PDFA train(TimedInput trainingSequences) {
		final PDFA pdfa;
		treba.log1plus_init_wrapper();
		final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
		final long jobNumber = Double.doubleToLongBits(Math.random());
		String jobName = Long.toString(jobNumber);
		jobName = jobName.substring(0, 5);
		// create treba input file
		final String tempFilePrefix = tempDir.toString() + File.separatorChar + jobName + getClass().getName();
		final Path trebaTrainSetFile = createUniqueFile(Paths.get(tempFilePrefix + "train_set"));

		try {
			createTrebaFile(trainingSequences, trebaTrainSetFile);
			final Path trebaAutomaton = createUniqueFile(Paths.get(tempFilePrefix + "fsm.fsm"));
			logger.info("Starting to learn PDFA with treba...");
			final double loglikelihood = trainFsm(trebaTrainSetFile, trebaAutomaton);
			logger.debug("learned event automaton has loglikelihood of {}", loglikelihood);
			// compute paths through the automata for the training set and write to
			// 'trebaResultPathFile'
			// parse the 'trebaResultPathFile'
			// compute likelihood on test set for automaton and for time PDFs
			pdfa = new PDFA(trebaAutomaton, trainingSequences);
			pdfa.makeImmutable();
			if (!Settings.isDebug()) {
				IoUtils.deleteFiles(new Path[] { trebaTrainSetFile, trebaAutomaton });
			} else {
				logger.debug("temp dir: {}", tempDir);
			}
			treba.log1plus_free_wrapper();
			logger.info("Learned PDFA with treba.");
			return pdfa;
		} catch (final IOException e) {
			logger.error("An unexpected error occured", e);
			e.printStackTrace();
		}
		return null;
	}



	private Path createUniqueFile(Path path) {
		Path result = path;
		while (Files.exists(result)) {
			logger.warn("File {} exists. Creating a new filename.", result);
			final long jobNumber = Double.doubleToLongBits(Math.random());
			String jobName = Long.toString(jobNumber);
			jobName = jobName.substring(0, 5);
			result = Paths.get(result.toString() + jobName);
			logger.warn("New filename is {}", result);
		}
		return result;
	}

	private void createTrebaFile(TimedInput timedSequences, Path trebaTrainFile) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(trebaTrainFile, StandardCharsets.UTF_8)) {
			for (final TimedWord ts : timedSequences) {
				bw.write(getIntString(ts, timedSequences));
				bw.append('\n');
			}
			bw.close();
		}
	}

	private String getIntString(TimedWord ts, TimedInput timedSequences) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ts.length(); i++) {
			sb.append(timedSequences.getAlphIndex(ts.getSymbol(i)));
			if (i != ts.length() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	protected double trainFsm(Path eventTrainFile, Path fsmOutputFile) {
		int recursive_merge_test = 0;
		if (recursiveMergeTest) {
			recursive_merge_test = 1;
		}
		treba.setT0(mergeT0);
		treba.setPrior(smoothingPrior);
		double ll;
		observations o = treba.observations_read(eventTrainFile.toString());
		if (o == null) {
			logger.error("Error reading observations file {}", eventTrainFile);
			throw new IllegalStateException("Error reading observations file " + eventTrainFile);
		}
		o = treba.observations_sort(o);
		o = treba.observations_uniq(o);
		wfsa fsm;
		if (mergeTest == MergeTest.MDI) {
			fsm = treba.dffa_to_wfsa(treba.dffa_mdi(o, mergeAlpha));
		} else {
			fsm = treba.dffa_to_wfsa(treba.dffa_state_merge(o, mergeAlpha, mergeTest.getAlgorithm(), recursive_merge_test));
		}
		ll = treba.loglikelihood_all_observations_fsm(fsm, o);
		treba.wfsa_to_file(fsm, fsmOutputFile.toString());

		if (fsm != null) {
			treba.wfsa_destroy(fsm);
		}
		if (o != null) {
			treba.observations_destroy(o);
		}
		return ll;
	}

}
