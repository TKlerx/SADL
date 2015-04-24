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
package de.upb.timok.modellearner;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsat.distributions.Distribution;
import jsat.distributions.MyDistributionSearch;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import treba.observations;
import treba.treba;
import treba.trebaConstants;
import treba.wfsa;
import de.upb.timok.constants.MergeTest;
import de.upb.timok.interfaces.Model;
import de.upb.timok.interfaces.ModelLearner;
import de.upb.timok.models.PDTTA;
import de.upb.timok.structure.TimedSequence;
import de.upb.timok.structure.ZeroProbTransition;
import de.upb.timok.utils.IoUtils;

public class PdttaLeaner implements ModelLearner {
	double mergeAlpha;
	MergeTest mergeTest = MergeTest.ALERGIA;
	boolean recursiveMergeTest;
	private static Logger logger = LoggerFactory.getLogger(PdttaLeaner.class);
	int fsmStateCount = -1;
	KernelFunction kdeKernelFunction;
	double kdeBandwidth;
	double smoothingPrior = 0.00;
	int mergeT0 = 3;

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest) {
		this.mergeAlpha = mergeAlpha;
		this.recursiveMergeTest = recursiveMergeTest;
	}

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth) {
		this(mergeAlpha, recursiveMergeTest);
		this.kdeKernelFunction = kdeKernelFunction;
		this.kdeBandwidth = kdeBandwidth;
	}

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest) {
		this(mergeAlpha, recursiveMergeTest, kdeKernelFunction, kdeBandwidth);
		this.mergeTest = mergeTest;
	}
	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior) {
		this(mergeAlpha, recursiveMergeTest, kdeKernelFunction, kdeBandwidth, mergeTest);
		this.smoothingPrior = smoothingPrior;
	}

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior, int mergeT0) {
		this(mergeAlpha, recursiveMergeTest, kdeKernelFunction, kdeBandwidth, mergeTest, smoothingPrior);
		this.mergeT0 = mergeT0;
	}

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest);
	}

	public PdttaLeaner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest, double smoothingPrior) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest, smoothingPrior);
	}


	@Override
	public Model train(List<TimedSequence> trainingSequences) {
		final PDTTA pdtta;
		treba.log1plus_init_wrapper();
		final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
		final long jobNumber = Double.doubleToLongBits(Math.random());
		String jobName = Long.toString(jobNumber);
		jobName = jobName.substring(0, 5);
		// create treba input file
		final String tempFilePrefix = tempDir.toString() + File.separatorChar + jobName + getClass().getName();
		final String trebaTrainSetFileString = tempFilePrefix + "train_set";
		try {
			createTrebaFile(trainingSequences, trebaTrainSetFileString);
			final String trebaAutomatonFile = tempFilePrefix + "fsm.fsm";
			final double loglikelihood = trainFsm(trebaTrainSetFileString, trebaAutomatonFile);
			logger.info("learned event automaton has loglikelihood of {}", loglikelihood);
			// compute paths through the automata for the training set and write to
			// 'trebaResultPathFile'
			final String trebaResultPathFile = tempFilePrefix + "train_likelihood";
			computeAutomatonPaths(trebaAutomatonFile, trebaTrainSetFileString, trebaResultPathFile);
			// parse the 'trebaResultPathFile'
			// Fill time interval buckets and fit PDFs for every bucket
			final Map<ZeroProbTransition, TDoubleList> timeValueBuckets = parseAutomatonPaths(trebaResultPathFile, trainingSequences);
			// do the fitting
			final Map<ZeroProbTransition, Distribution> transitionDistributions = fit(timeValueBuckets);
			// compute likelihood on test set for automaton and for time PDFs
			pdtta = new PDTTA(Paths.get(trebaAutomatonFile));
			pdtta.setTransitionDistributions(transitionDistributions);
			IoUtils.deleteFiles(new String[] { trebaTrainSetFileString, trebaAutomatonFile, trebaResultPathFile });
			treba.log1plus_free_wrapper();
			return pdtta;
		} catch (final IOException e) {
			logger.error("An unexpected error occured", e);
			e.printStackTrace();
		}
		return null;
	}

	private Map<ZeroProbTransition, Distribution> fit(Map<ZeroProbTransition, TDoubleList> timeValueBuckets) {
		final Map<ZeroProbTransition, Distribution> result = new HashMap<>();
		for (final ZeroProbTransition t : timeValueBuckets.keySet()) {
			result.put(t, fitDistribution(timeValueBuckets.get(t)));
		}
		return result;
	}

	@SuppressWarnings("boxing")
	private Distribution fitDistribution(TDoubleList transitionTimes) {
		final Vec v = new DenseVector(transitionTimes.toArray());
		final jsat.utils.Pair<Boolean, Double> sameValues = MyDistributionSearch.checkForDifferentValues(v);
		if (sameValues.getFirstItem()) {
			final Distribution d = new SingleValueDistribution(sameValues.getSecondItem());
			return d;
		} else {
			KernelFunction newKernelFunction = kdeKernelFunction;
			if (newKernelFunction == null) {
				newKernelFunction = KernelDensityEstimator.autoKernel(v);
			}
			double newKdeBandwidth = kdeBandwidth;
			if (newKdeBandwidth <= 0) {
				newKdeBandwidth = KernelDensityEstimator.BandwithGuassEstimate(v);
			}
			final KernelDensityEstimator kde = new KernelDensityEstimator(v, newKernelFunction, newKdeBandwidth);
			return kde;
		}
	}

	private Map<ZeroProbTransition, TDoubleList> parseAutomatonPaths(String trebaResultPathFile, List<TimedSequence> timedSequences) throws IOException {
		// TODO when this is done in java, do this in memory instead of with files
		final Map<ZeroProbTransition, TDoubleList> result = new HashMap<>();
		final BufferedReader br = Files.newBufferedReader(Paths.get(trebaResultPathFile), StandardCharsets.UTF_8);
		String line = null;
		int rowIndex = 0;
		int currentState = -1;
		int followingState = -1;
		while ((line = br.readLine()) != null) {
			final String[] split = line.split("\\s+");
			final TDoubleList timeValues = timedSequences.get(rowIndex).getTimeValues();
			final TIntList eventValues = timedSequences.get(rowIndex).getEvents();
			if (split.length - 2 != timeValues.size()) {
				logger.error("There should be one more state than there are time values (time values fill the gaps between the states\n{}\n{}",
						Arrays.toString(split), timeValues);
				logger.error("Error occured in line={}", rowIndex);
				break;
			}
			// first element is likelihood; not interested in that right now
			for (int i = 1; i < split.length - 1; i++) {
				currentState = Integer.parseInt(split[i]);
				followingState = Integer.parseInt(split[i + 1]);
				addTimeValue(result, currentState, followingState, eventValues.get(i - 1), timeValues.get(i - 1));
			}

			rowIndex++;
		}
		if (rowIndex != timedSequences.size()) {
			logger.error("rowCount and sequences length do not match ({} / {})", rowIndex, timedSequences.size());
		}
		br.close();
		return result;
	}

	private void addTimeValue(Map<ZeroProbTransition, TDoubleList> result, int currentState, int followingState, int event, double timeValue) {
		final ZeroProbTransition t = new ZeroProbTransition(currentState, followingState, event);
		final TDoubleList list = result.get(t);
		if (list == null) {
			final TDoubleList tempList = new TDoubleArrayList();
			tempList.add(timeValue);
			result.put(t, tempList);
		} else {
			list.add(timeValue);
		}
	}

	@SuppressWarnings("null")
	private void computeAutomatonPaths(String trebaAutomatonFile, String trebaTrainFileString, String trebaResultPathFile) {
		// TODO do this in java!
		// treba.log1plus_taylor_init_wrapper();
		final observations o = treba.observations_read(trebaTrainFileString);
		final wfsa fsm = treba.wfsa_read_file(trebaAutomatonFile);
		final int obs_alphabet_size = treba.observations_alphabet_size(o);
		if (o == null) {
			logger.error("Error: the observations file could not be read:{}", trebaTrainFileString);
			System.exit(1);
		}
		if (fsm == null) {
			logger.error("Error: the fsm file could not be read:{}", trebaAutomatonFile);
			System.exit(1);
		}
		if (o != null && fsm != null && fsm.getAlphabet_size() < obs_alphabet_size) {
			logger.error("Error: the observations file has symbols outside the FSA alphabet.\n");
			System.exit(1);
		}
		if (trebaConstants.FORMAT_LOG2 != 0) {
			treba.wfsa_to_log2(fsm);
		}
		fsmStateCount = fsm.getNum_states();
		// write the visited states for each observation to the file (trebaResultPathFile)
		treba.forward_fsm_to_file(fsm, o, trebaConstants.DECODE_FORWARD_PROB, trebaResultPathFile);
		if (o != null) {
			treba.observations_destroy(o);
		}
		if (fsm != null) {
			treba.wfsa_destroy(fsm);
			// treba.log1plus_free_wrapper();
		}
	}

	private void createTrebaFile(List<TimedSequence> timedSequences, String trebaTrainFileString) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(trebaTrainFileString), StandardCharsets.UTF_8)) {
			for (final TimedSequence ts : timedSequences) {
				bw.write(ts.getEventString());
				bw.append('\n');
			}
			bw.close();
		}
	}

	public double trainFsm(String eventTrainFile, String fsmOutputFile) {
		int recursive_merge_test = 0;
		if (recursiveMergeTest) {
			recursive_merge_test = 1;
		}
		treba.setT0(mergeT0);
		treba.setPrior(smoothingPrior);
		double ll;
		observations o = treba.observations_read(eventTrainFile);
		if (o == null) {
			logger.error("Error reading observations file {}", eventTrainFile);
			System.exit(1);
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
		treba.wfsa_to_file(fsm, fsmOutputFile);

		if (fsm != null) {
			treba.wfsa_destroy(fsm);
		}
		if (o != null) {
			treba.observations_destroy(o);
		}
		return ll;
	}

}
