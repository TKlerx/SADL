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
package sadl.modellearner.rtiplus;

import java.nio.file.Path;
import java.util.List;
import java.util.NavigableSet;

import com.google.common.collect.TreeMultimap;

import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.modellearner.rtiplus.analysis.DistributionAnalysis;
import sadl.modellearner.rtiplus.analysis.QuantileAnalysis;
import sadl.modellearner.rtiplus.boolop.OrOperator;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAInput;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
public class SearchingPDRTALearner extends SimplePDRTALearner {

	public enum SearchMeasure {
		AIC, SIZE
	}

	private final int maxOperationsToSearch;
	private final boolean searchParallel;
	private final SearchMeasure measure;

	/**
	 * Creates a searching RTI+ learner as it was implemented by Verwer
	 * 
	 * @param sig
	 * @param numHistoBins
	 * @param doNotMergeWithRoot
	 * @param testParallel
	 * @param searchMeasure
	 * @param searchParallel
	 * @param dir
	 */
	public SearchingPDRTALearner(double sig, int numHistoBins, boolean doNotMergeWithRoot, boolean testParallel, SearchMeasure searchMeasure,
			boolean searchParallel, Path dir) {

		this(sig, new QuantileAnalysis(numHistoBins), new LikelihoodRatioTester(false), SplitPosition.LEFT, doNotMergeWithRoot, testParallel, 10, searchMeasure,
				searchParallel, "AOO", dir);
	}

	/**
	 * Creates a searching RTI+ learner without IDA
	 * 
	 * @param sig
	 * @param histoBinDistritutionAnalysis
	 * @param operationTester
	 * @param splitPos
	 * @param doNotMergeWithRoot
	 * @param testParallel
	 * @param maxOperationsToSearch
	 * @param searchMeasure
	 * @param searchParallel
	 * @param boolOps
	 * @param dir
	 */
	public SearchingPDRTALearner(double sig, DistributionAnalysis histoBinDistritutionAnalysis, OperationTester operationTester, SplitPosition splitPos,
			boolean doNotMergeWithRoot, boolean testParallel, int maxOperationsToSearch, SearchMeasure searchMeasure, boolean searchParallel, String boolOps,
			Path dir) {

		this(sig, histoBinDistritutionAnalysis, operationTester, splitPos, doNotMergeWithRoot, testParallel, null, true, false, 0.0, maxOperationsToSearch,
				searchMeasure, searchParallel, boolOps, dir);
	}

	public SearchingPDRTALearner(double sig, DistributionAnalysis histoBinDistritutionAnalysis, OperationTester operationTester, SplitPosition splitPos,
			boolean doNotMergeWithRoot, boolean testParallel, DistributionAnalysis intervalDistributionAnalysis, boolean removeBorderGapsOnly,
			boolean performIDAActively, double intervalExpansionRate, int maxOperationsToSearch, SearchMeasure searchMeasure, boolean searchParallel,
			String boolOps, Path dir) {

		super(sig, histoBinDistritutionAnalysis, operationTester, splitPos, doNotMergeWithRoot, testParallel, intervalDistributionAnalysis,
				removeBorderGapsOnly, performIDAActively, intervalExpansionRate, boolOps, dir);

		this.maxOperationsToSearch = maxOperationsToSearch;
		this.searchParallel = searchParallel;
		this.measure = searchMeasure;
	}

	@Override
	@SuppressWarnings("boxing")
	public ProbabilisticModel train(TimedInput trainingSequences) {

		logger.info("RTI+: Building automaton from input sequences");

		final boolean expand = intervalDistriAnalysis != null;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histoBinDistriAnalysis, expand ? intervalExpRate : 0.0);
		final PDRTA a = new PDRTA(in);

		// TODO log new params
		logger.info("Parameters are: significance={} distrCheckType={}", significance);
		logger.info("Histogram Bins are: {}", a.getHistBinsString());

		logger.info("*** Performing searching RTI+ ***");
		startTime = System.currentTimeMillis();
		final StateColoring sc = new StateColoring(a);
		sc.setRed(a.getRoot());
		tester.setColoring(sc);
		mainModel = a;
		search(a, sc);

		if (intervalDistriAnalysis != null && !performIDAActively) {
			logger.info("Running IDA passively after training");
			runIDAPassively(a);
		}

		logger.info("Final PDRTA contains {} states and {} transitions", a.getStateCount(), a.getSize());
		logger.info("Trained PDRTA with quality: Likelihood={} and AIC={}", Math.exp(NaiveLikelihoodRatioTester.calcLikelihood(a).getRatio()), calcAIC(a));

		a.cleanUp();

		logger.info("Time: {}", getDuration(startTime, System.currentTimeMillis()));
		logger.info("END");

		return a;
	}

	// TODO Try to reduce duplicated code regarding SimplePDRTALearner.complete(...)
	@SuppressWarnings("boxing")
	private void search(PDRTA a, StateColoring sc) {

		final boolean preExit = (bOp[2] instanceof OrOperator) && (intervalDistriAnalysis == null);
		if (preExit) {
			logger.info("Pre-Exiting algorithm when number of tails falls below minData");
		}

		int counter = 0;
		Transition t;
		while ((t = getMostVisitedTrans(a, sc)) != null && !(preExit && t.in.getTails().size() >= PDRTA.getMinData())) {
			if (directory != null) {
				draw(a, sc, true, directory, counter);
			}
			logger.debug("Automaton contains {} states and {} transitions", a.getStateCount(), a.getSize());
			logger.debug("Found most visited transition  {}  containing {} tails", t.toString(), t.in.getTails().size());
			counter++;

			if (intervalDistriAnalysis != null) {
				logger.debug("Checking data distribution");
				final List<Interval> idaIns = perfomIDA(t.source, t.symAlphIdx, t.in.getEnd(), sc);
				if (idaIns.size() > 0) {
					logger.debug("#{} DO: Split interval due to IDA into {} intervals", counter, idaIns.size());
					if (logger.isTraceEnabled()) {
						final StringBuilder sb = new StringBuilder();
						for (final Interval in : idaIns) {
							sb.append("  ");
							sb.append(in.toString());
						}
						logger.trace("Resulting intervals are:{}", sb.toString());
					}
					continue;
				} else {
					logger.debug("No splits because of data distributuion were perfomed in:  {}", t.in.toString());
					if (bOp[2] instanceof OrOperator && t.in.getTails().size() < PDRTA.getMinData()) {
						// Shortcut for skipping merges and splits when OR is selected
						if (mainModel == a) {
							logger.debug("#{} DO: Color state {} red", counter, t.target.getIndex());
						}
						sc.setRed(t.target);
						continue;
					}
				}
			}

			logger.debug("Testing splits");
			final NavigableSet<Refinement> splits = getSplitRefs(t, sc).descendingSet();
			logger.debug("Found {} possible splits", splits.size());
			logger.debug("Testing merges");
			final NavigableSet<Refinement> merges = getMergeRefs(t, sc).descendingSet();
			logger.debug("Found {} possible merges", merges.size());
			logger.debug("Calculating sizes for splits");

			final TreeMultimap<Double, Refinement> all = TreeMultimap.create();
			int c = 0;
			for (final Refinement r : splits) {
				if (c >= maxOperationsToSearch) {
					break;
				}
				final PDRTA copy = new PDRTA(a);
				final StateColoring cColoring = new StateColoring(sc, copy);
				final Refinement cR = new Refinement(copy, r, cColoring);
				cR.refine();
				complete(copy, cColoring);
				double modelScore;
				switch (measure) {
					case AIC:
						modelScore = calcAIC(copy);
						break;
					case SIZE:
						modelScore = copy.getSize();
						break;
					default:
						modelScore = copy.getSize();
						break;
				}
				all.put(modelScore, r);
				c++;
			}

			logger.debug("Calculating sizes for merges");
			c = 0;
			for (final Refinement r : merges) {
				if (c >= maxOperationsToSearch) {
					break;
				}
				final PDRTA copy = new PDRTA(a);
				final StateColoring cColoring = new StateColoring(sc, copy);
				final Refinement cR = new Refinement(copy, r, cColoring);
				cR.refine();
				complete(copy, cColoring);
				double modelScore;
				switch (measure) {
					case AIC:
						modelScore = calcAIC(copy);
						break;
					case SIZE:
						modelScore = copy.getSize();
						break;
					default:
						modelScore = copy.getSize();
						break;
				}
				all.put(modelScore, r);
				c++;
			}

			assert (all.size() <= (2 * maxOperationsToSearch));
			if (!all.isEmpty()) {
				final double minSize = all.keySet().first();
				final Refinement r = all.get(minSize).last();
				logger.debug("#{} DO: {}  quality={}", counter, r.toString(), minSize);
				r.refine();
			} else {
				logger.debug("#{} DO: Color state {} red", counter, t.target.getIndex());
				sc.setRed(t.target);
			}
			if (Settings.isDebug()) {
				a.checkConsistency();
			}
		}

		a.checkConsistency();
		assert (a.getStateCount() == sc.getNumRedStates());
		if (directory != null) {
			draw(a, sc, true, directory, counter);
		}
	}

}
