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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.MyDistributionSearch;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.empirical.MyKernelDensityEstimator;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import sadl.constants.MergeTest;
import sadl.input.TimedInput;
import sadl.interfaces.ModelLearner;
import sadl.interfaces.TauEstimator;
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
	private final TauEstimator tauEstimator;

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest), null, 0);
	}

	public PdttaLearner(PdfaLearner pdfaLearner, KernelFunction kdeKernelFunction, double kdeBandwidth) {
		this(pdfaLearner, kdeKernelFunction, kdeBandwidth, null);
	}

	public PdttaLearner(PdfaLearner pdfaLearner, KernelFunction kdeKernelFunction, double kdeBandwidth, TauEstimator tauEstimation) {
		this.kdeKernelFunction = kdeKernelFunction;
		this.kdeBandwidth = kdeBandwidth;
		this.pdfaLearner = pdfaLearner;
		this.tauEstimator = tauEstimation;
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			TauEstimator tauEstimation) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest), kdeKernelFunction, kdeBandwidth, tauEstimation);
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest), kdeKernelFunction, kdeBandwidth, null);
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior), kdeKernelFunction, kdeBandwidth);

	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior, TauEstimator tauEstimatior) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior), kdeKernelFunction, kdeBandwidth, tauEstimatior);

	}


	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, KernelFunction kdeKernelFunction, double kdeBandwidth, MergeTest mergeTest,
			double smoothingPrior, int mergeT0, TauEstimator tauEstimation) {
		this(new TrebaPdfaLearner(mergeAlpha, recursiveMergeTest, mergeTest, smoothingPrior, mergeT0), kdeKernelFunction, kdeBandwidth, tauEstimation);

	}


	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest);
	}

	public PdttaLearner(double mergeAlpha, boolean recursiveMergeTest, MergeTest mergeTest, double smoothingPrior) {
		this(mergeAlpha, recursiveMergeTest, null, -1, mergeTest, smoothingPrior);
	}


	@Override
	public PDTTA train(TimedInput trainingSequences) {

		final PDFA pdfa = pdfaLearner.train(trainingSequences);
		try {
			// for debugging why the hell parallel execution leads to different and non deterministic results even though the maps (and even the automata) are
			// the same!
			// final Map<ZeroProbTransition, TDoubleList> timeValueBucketsPar = fillTimeValueBucketsParallel(pdfa, trainingSequences);
			final Map<ZeroProbTransition, TDoubleList> timeValueBucketsSeq = fillTimeValueBuckets(pdfa, trainingSequences);

			// timeValueBucketsPar.values().forEach(list -> list.sort());
			// timeValueBucketsSeq.values().forEach(list -> list.sort());
			// if (!timeValueBucketsPar.equals(timeValueBucketsSeq)) {
			// throw new IllegalStateException();
			// }

			// final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributionsPar = fit(timeValueBucketsPar);
			final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributionsSeq = fit(timeValueBucketsSeq);
			// if (!transitionDistributionsPar.equals(transitionDistributionsSeq)) {
			// throw new IllegalStateException();
			// }

			// final PDTTA pdttaPar = new PDTTA(pdfa, transitionDistributionsPar, tauEstimator);
			// pdttaPar.setAlphabet(trainingSequences);
			// pdttaPar.preprocess();
			// pdttaPar.makeImmutable();

			final PDTTA pdttaSeq = new PDTTA(pdfa, transitionDistributionsSeq, tauEstimator);
			pdttaSeq.setAlphabet(trainingSequences);
			pdttaSeq.preprocess();
			pdttaSeq.makeImmutable();
			//
			// if (!pdttaSeq.equals(pdttaPar)) {
			// throw new IllegalStateException();
			// }

			logger.info("Learned PDTTA.");
			return pdttaSeq;
		} catch (final IOException e) {
			logger.error("An unexpected error occured", e);
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unused")
	private Map<ZeroProbTransition, TDoubleList> fillTimeValueBucketsParallel(PDFA pdfa, TimedInput trainingSequences) {
		final Map<ZeroProbTransition, TDoubleList> result = new HashMap<>();
		// TODO check why parallelism destroys determinism
		final Lock l = new ReentrantLock();
		trainingSequences.getWords().parallelStream().forEach(word -> {
			int currentState = -1;
			int followingState = -1;
			currentState = pdfa.getStartState();
			for (int i = 0; i < word.length(); i++) {
				final String symbol = word.getSymbol(i);
				final int timeValue = word.getTimeValue(i);
				final Transition t = pdfa.getTransition(currentState, symbol);
				followingState = t.getToState();
				l.lock();
				addTimeValue(result, currentState, followingState, symbol, timeValue);
				l.unlock();
				currentState = followingState;
			}
		});
		return result;
	}

	protected Map<ZeroProbTransition, TDoubleList> fillTimeValueBuckets(PDFA pdfa, TimedInput trainingSequences) {

		final Map<ZeroProbTransition, TDoubleList> result2 = new HashMap<>();
		trainingSequences.getWords().stream().forEach(word -> {
			int currentState = -1;
			int followingState = -1;
			currentState = pdfa.getStartState();
			for (int i = 0; i < word.length(); i++) {
				final String symbol = word.getSymbol(i);
				final int timeValue = word.getTimeValue(i);
				final Transition t = pdfa.getTransition(currentState, symbol);
				followingState = t.getToState();
				addTimeValue(result2, currentState, followingState, symbol, timeValue);
				currentState = followingState;
			}
		});

		// works if done with result2, does not work with result. Even though both maps are the same (except order).
		return result2;
	}


	protected static void addTimeValue(Map<ZeroProbTransition, TDoubleList> result, int currentState, int followingState, String event,
			double timeValue) {
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

	protected Map<ZeroProbTransition, ContinuousDistribution> fit(Map<ZeroProbTransition, TDoubleList> timeValueBuckets) {
		// parallel (does not destroy determinism)
		final Map<ZeroProbTransition, ContinuousDistribution> result = Collections.synchronizedMap(new HashMap<>());
		timeValueBuckets.keySet().parallelStream().forEach(t -> result.put(t, fitDistribution(timeValueBuckets.get(t))));
		return result;
	}

	@SuppressWarnings("boxing")
	protected ContinuousDistribution fitDistribution(TDoubleList transitionTimes) {
		final Vec v = new DenseVector(transitionTimes.toArray());
		final jsat.utils.Pair<Boolean, Double> sameValues = MyDistributionSearch.checkForDifferentValues(v);
		if (sameValues.getFirstItem()) {
			final ContinuousDistribution d = new SingleValueDistribution(sameValues.getSecondItem());
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
