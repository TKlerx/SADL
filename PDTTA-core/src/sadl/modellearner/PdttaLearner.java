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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jsat.distributions.Distribution;
import jsat.distributions.MyDistributionSearch;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.empirical.MyKernelDensityEstimator;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.ModelLearner;
import sadl.models.PDFA;
import sadl.models.PDTTA;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;

/**
 * 
 * @author Timo Klerx
 *
 */
public class PdttaLearner implements ModelLearner {
	private static Logger logger = LoggerFactory.getLogger(PdttaLearner.class);
	KernelFunction kdeKernelFunction;
	double kdeBandwidth;
	private final PdfaLearner pdfaLearner;

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest), null, 0);
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest), kdeKernelFunction, kdeBandwidth);
	}

	public PdttaLearner(PdfaLearner pdfaLearner, KernelFunction kdeKernelFunction, double kdeBandwidth) {
		this.kdeKernelFunction = kdeKernelFunction;
		this.kdeBandwidth = kdeBandwidth;
		this.pdfaLearner = pdfaLearner;
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest), kdeKernelFunction, kdeBandwidth);

	}
	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior), kdeKernelFunction, kdeBandwidth);

	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior, int mergeT0) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior, mergeT0), kdeKernelFunction, kdeBandwidth);

	}


	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest);
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest, double smoothingPrior) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest, smoothingPrior);
	}


	@Override
	public PDTTA train(TimedInput trainingSequences) {
		final PDTTA pdtta;
		final PDFA pdfa = pdfaLearner.train(trainingSequences);
		try {
			final Map<ZeroProbTransition, TDoubleList> timeValueBuckets = fillTimeValueBuckets(pdfa, trainingSequences);
			final Map<ZeroProbTransition, Distribution> transitionDistributions = fit(timeValueBuckets);
			pdtta = new PDTTA(pdfa, transitionDistributions);
			pdtta.setAlphabet(trainingSequences);
			pdtta.makeImmutable();
			return pdtta;
		} catch (final IOException e) {
			logger.error("An unexpected error occured", e);
			e.printStackTrace();
		}
		return null;
	}

	protected Map<ZeroProbTransition, TDoubleList> fillTimeValueBuckets(PDFA pdfa, TimedInput trainingSequences) {
		final Map<ZeroProbTransition, TDoubleList> result = new HashMap<>();
		int currentState = -1;
		int followingState = -1;
		for (final TimedWord word : trainingSequences) {
			currentState = pdfa.getStartState();
			for (int i = 0; i < word.length(); i++) {
				final String symbol = word.getSymbol(i);
				final int timeValue = word.getTimeValue(i);
				final Transition t = pdfa.getTransition(currentState, symbol);
				followingState = t.getToState();
				addTimeValue(result, currentState, followingState, symbol, timeValue);
				currentState = followingState;
			}
		}
		return result;
	}


	protected static void addTimeValue(Map<ZeroProbTransition, TDoubleList> result, int currentState, int followingState, String event, double timeValue) {
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

	protected Map<ZeroProbTransition, Distribution> fit(Map<ZeroProbTransition, TDoubleList> timeValueBuckets) {
		final Map<ZeroProbTransition, Distribution> result = new HashMap<>();
		for (final ZeroProbTransition t : timeValueBuckets.keySet()) {
			result.put(t, fitDistribution(timeValueBuckets.get(t)));
		}
		return result;
	}

	@SuppressWarnings("boxing")
	protected Distribution fitDistribution(TDoubleList transitionTimes) {
		final Vec v = new DenseVector(transitionTimes.toArray());
		final jsat.utils.Pair<Boolean, Double> sameValues = MyDistributionSearch.checkForDifferentValues(v);
		if (sameValues.getFirstItem()) {
			final Distribution d = new SingleValueDistribution(sameValues.getSecondItem());
			return d;
		} else {
			KernelFunction newKernelFunction = kdeKernelFunction;
			if (newKernelFunction == null) {
				newKernelFunction = MyKernelDensityEstimator.autoKernel(v);
			}
			double newKdeBandwidth = kdeBandwidth;
			if (newKdeBandwidth <= 0) {
				newKdeBandwidth = MyKernelDensityEstimator.BandwithGuassEstimate(v);
			}
			final MyKernelDensityEstimator kde = new MyKernelDensityEstimator(v, newKernelFunction, newKdeBandwidth);
			return kde;
		}
	}
}
