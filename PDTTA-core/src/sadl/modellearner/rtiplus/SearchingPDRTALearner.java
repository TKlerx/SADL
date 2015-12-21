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

package sadl.modellearner.rtiplus;

import java.util.List;
import java.util.NavigableSet;

import com.google.common.collect.TreeMultimap;

import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.modellearner.rtiplus.boolop.OrOperator;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
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

	private final int maxMergesToSearch = 10;
	private final int maxSplitsToSearch = 10;

	public SearchingPDRTALearner(double sig, String histBins, OperationTesterType testerType, DistributionCheckType distrCheckType, SplitPosition splitPos,
			String boolOps, String dir) {
		super(sig, histBins, testerType, distrCheckType, splitPos, boolOps, dir);
	}

	@Override
	public ProbabilisticModel train(TimedInput trainingSequences) {

		logger.info("RTI+: Building automaton from input sequences");

		final boolean expand = distrCheckType.compareTo(DistributionCheckType.STRICT) > 0;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histBinsStr, expand);
		final PDRTA a = new PDRTA(in);

		// TODO log new params
		logger.info("Parameters are: significance={} distrCheckType={}", significance, distrCheckType);
		logger.info("Histogram Bins are: {}", a.getHistBinsString());

		logger.info("*** Performing searching RTI+ ***");
		startTime = System.currentTimeMillis();
		final StateColoring sc = new StateColoring(a);
		sc.setRed(a.getRoot());
		tester.setColoring(sc);
		mainModel = a;
		greedyRTIplus(a, sc);

		logger.info("Final PDRTA contains {} states and {} transitions", a.getStateCount(), a.getSize());
		logger.info("Trained PDRTA with quality: Likelihood={} and AIC={}", Math.exp(NaiveLikelihoodRatioTester.calcLikelihood(a).getRatio()), calcAIC(a));

		a.cleanUp();

		logger.info("Time: {}", getDuration(startTime, System.currentTimeMillis()));
		logger.info("END");

		return a;
	}

	// TODO Try to reduce duplicated code regarding SimplePDRTALearner.complete(...)
	private void greedyRTIplus(PDRTA a, StateColoring sc) {

		final boolean preExit = (bOp[2] instanceof OrOperator) && distrCheckType.equals(DistributionCheckType.DISABLED);
		if (preExit) {
			logger.info("Pre-Exiting algorithm when number of tails falls below minData");
		}

		int counter = 0;
		Transition t;
		while ((t = getMostVisitedTrans(a, sc)) != null && !(preExit && t.in.getTails().size() >= PDRTA.getMinData())) {
			if (directory != null) {
				draw(a, true, directory, counter);
			}
			logger.debug("Automaton contains {} states and {} transitions", a.getStateCount(), a.getSize());
			logger.debug("Found most visited transition  {}  containing {} tails", t.toString(), t.in.getTails().size());
			counter++;

			if (!distrCheckType.equals(DistributionCheckType.DISABLED)) {
				logger.debug("Checking data distribution");
				final List<Interval> idaIns = checkDistribution(t.source, t.symAlphIdx, distrCheckType, sc);
				if (idaIns.size() > 0) {
					logger.debug("#{} DO: Split interval due to IDA into {} intervals", counter, idaIns.size());
					// TODO Printing the intervals may be to expensive just for logging
					final StringBuilder sb = new StringBuilder();
					for (final Interval in : idaIns) {
						sb.append("  ");
						sb.append(in.toString());
					}
					logger.trace("Resulting intervals are:{}", sb.toString());
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
				if (c >= maxSplitsToSearch) {
					break;
				}
				final PDRTA copy = new PDRTA(a);
				final StateColoring cColoring = new StateColoring(sc, copy);
				final Refinement cR = new Refinement(copy, r, cColoring);
				cR.refine();
				complete(copy, cColoring);
				// TODO Create algo param for selecting between AIC and size
				// final double modelScore = copy.getSize();
				final double modelScore = calcAIC(copy);
				all.put(modelScore, r);
				c++;
			}

			logger.debug("Calculating sizes for merges");
			c = 0;
			for (final Refinement r : merges) {
				if (c >= maxMergesToSearch) {
					break;
				}
				final PDRTA copy = new PDRTA(a);
				final StateColoring cColoring = new StateColoring(sc, copy);
				final Refinement cR = new Refinement(copy, r, cColoring);
				cR.refine();
				complete(copy, cColoring);
				// TODO Create algo param for selecting between AIC and size
				// final double modelScore = copy.getSize();
				final double modelScore = calcAIC(copy);
				all.put(modelScore, r);
				c++;
			}

			assert (all.size() <= (maxMergesToSearch + maxSplitsToSearch));
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
			draw(a, true, directory, counter);
		}
	}

}
