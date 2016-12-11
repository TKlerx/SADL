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
package sadl.models.pdrta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;

import com.google.common.collect.Multimap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.tester.LikelihoodValue;

/**
 * This class manages the time and symbol probabilities for a {@link PDRTAState} which are needed for calculating the Likelihood Ratio Test and anomaly
 * detection. It also provides static methods for calculating the Likelihood Ratio.
 * 
 * @author Fabian Witter
 * 
 */
public class StateStatistic implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Contains the sizes (width) of the histogram bins
	 */
	private int[] histBarSizes;

	/**
	 * Contains the number of outgoing {@link TimedTail}s for each histogram bin while training
	 */
	private int[] timeCount;

	/**
	 * Contains the number of outgoing {@link TimedTail}s for each symbol while training
	 */
	private int[] symbolCount;

	/**
	 * Contains the total number of outgoing {@link TimedTail}s while training
	 */
	private int totalOutCount;

	/**
	 * Contains the number of incoming {@link TimedTail}s while training
	 */
	private int totalInCount;

	/**
	 * Determines if this {@link StateStatistic} exists while training or during anomaly detection
	 */
	private boolean trainMode;

	/**
	 * Contains the probabilities for outgoing {@link TimedTail}s for each histogram bin during anomaly detection
	 */
	private double[] timeProbs;

	/**
	 * Contains the probabilities for outgoing {@link TimedTail}s for each symbol during anomaly detection
	 */
	private double[] symbolProbs;

	/**
	 * Contains the probability for incoming {@link TimedTail}s to end in the {@link PDRTAState} during anomaly detection
	 */
	private double tailEndProb;

	/**
	 * Contains the probabilities for transitions to be used by {@link TimedTail}s during anomaly detection
	 */
	private TIntObjectHashMap<TObjectDoubleHashMap<Interval>> intervalProbs;

	/**
	 * Creates an initial {@link StateStatistic} for training
	 * 
	 * @param numSymbols
	 *            The number of symbols from the input set
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 * @return An initial {@link StateStatistic} for training
	 */
	protected static StateStatistic initStat(int numSymbols, int[] histoBarSizes) {

		return new StateStatistic(numSymbols, histoBarSizes);
	}

	/**
	 * Creates a {@link StateStatistic} reconstructed from a already trained and persisted {@link PDRTA}
	 * 
	 * @param alphSize
	 *            The number of symbols
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 * @param stats
	 *            A set containing the probabilities embedded in formated {@link String}s
	 * @return A {@link StateStatistic} reconstructed from a already trained and persisted {@link PDRTA}
	 */
	protected static StateStatistic reconstructStat(int alphSize, int[] histoBarSizes, Collection<String> stats) {

		double tailEndProb = 0.0;
		double[] timeProbs = new double[0];
		double[] symbolProbs = new double[0];

		assert (stats.size() == 3);
		for (String s : stats) {
			if (s.matches("^\\d+ T.+")) {
				// Sample: 0 TIME 0.0 / 0.0 / 0.0
				final String[] p = s.split(" ", 3)[2].split(" / ");
				timeProbs = new double[p.length];
				for (int i = 0; i < p.length; i++) {
					try {
						timeProbs[i] = Double.parseDouble(p[i]);
					} catch (final NumberFormatException e) {
						e.printStackTrace();
					}
				}
				if (timeProbs.length != histoBarSizes.length) {
					throw new IllegalArgumentException("Content is not correct!");
				}
			} else if (s.matches("^\\d+ S.+")) {
				// Sample: 0 SYM 0.0 / 0.0 / 0.0
				final String[] p = s.split(" ", 3)[2].split(" / ");
				symbolProbs = new double[p.length];
				for (int i = 0; i < p.length; i++) {
					try {
						symbolProbs[i] = Double.parseDouble(p[i]);
					} catch (final NumberFormatException e) {
						e.printStackTrace();
					}
				}
				if (symbolProbs.length != alphSize) {
					throw new IllegalArgumentException("Content is not correct!");
				}
			} else if (s.matches("^\\d+ \\[.+")) {
				// Sample: 0 [ xlabel = "0.0", fillcolor = "#FFA9A9" ];
				s = s.split("\"", 3)[1];
				try {
					tailEndProb = Double.parseDouble(s);
				} catch (final NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}

		return new StateStatistic(histoBarSizes, timeProbs, symbolProbs, tailEndProb);
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the symbol distributions for merging two {@link PDRTAState}s
	 * 
	 * @param s1
	 *            First {@link PDRTAState} for merging
	 * @param s2
	 *            Second {@link PDRTAState} for merging
	 * @return The {@link LikelihoodValue} of the symbol distributions for merging
	 */
	public static LikelihoodValue getLikelihoodRatioSym(PDRTAState s1, PDRTAState s2, boolean advancedPooling, CalcRatio cr) {

		final StateStatistic st1 = s1.getStat();
		final StateStatistic st2 = s2.getStat();
		final PDRTA a = s1.getPDRTA();
		final int minData = PDRTA.getMinData();

		if (!st1.trainMode || !st2.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
		if (SimplePDRTALearner.bOp[2].eval(st1.totalOutCount < minData, st2.totalOutCount < minData)) {
			return new LikelihoodValue();
		} else {
			return calcInterimLRT(a, st1.symbolCount, st2.symbolCount, advancedPooling, cr);
		}
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the histogram bin distributions for merging two {@link PDRTAState}s
	 * 
	 * @param s1
	 *            First {@link PDRTAState} for merging
	 * @param s2
	 *            Second {@link PDRTAState} for merging
	 * @return The {@link LikelihoodValue} of the histogram bin distributions for merging
	 */
	public static LikelihoodValue getLikelihoodRatioTime(PDRTAState s1, PDRTAState s2, boolean advancedPooling, CalcRatio cr) {

		final StateStatistic st1 = s1.getStat();
		final StateStatistic st2 = s2.getStat();
		final PDRTA a = s1.getPDRTA();
		final int minData = PDRTA.getMinData();

		if (!st1.trainMode || !st2.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
		if (SimplePDRTALearner.bOp[2].eval(st1.totalOutCount < minData, st2.totalOutCount < minData)) {
			return new LikelihoodValue();
		} else {
			return calcInterimLRT(a, st1.timeCount, st2.timeCount, advancedPooling, cr);
		}
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the symbol distributions for splitting a transition. This done by splitting the set of {@link TimedTail}s in a
	 * {@link PDRTAState}
	 * 
	 * @param s
	 *            The {@link PDRTAState} for splitting
	 * @param mSym
	 *            The Set of {@link TimedTail}s to be split apart clustered by symbol index
	 * @return The {@link LikelihoodValue} of the symbol distributions for splitting a transition
	 */
	public static LikelihoodValue getLikelihoodRatioSym(PDRTAState s, Multimap<Integer, TimedTail> mSym, boolean advancedPooling, CalcRatio cr) {

		final StateStatistic st = s.getStat();
		final PDRTA a = s.getPDRTA();
		final int minData = PDRTA.getMinData();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
		if (SimplePDRTALearner.bOp[2].eval((st.totalOutCount - mSym.size()) < minData, mSym.size() < minData)) {
			return new LikelihoodValue();
		}

		final int[] part1SymCount = Arrays.copyOf(st.symbolCount, st.symbolCount.length);
		final int[] part2SymCount = new int[st.symbolCount.length];
		for (final Entry<Integer, Collection<TimedTail>> eCol : mSym.asMap().entrySet()) {
			part1SymCount[eCol.getKey().intValue()] -= eCol.getValue().size();
			part2SymCount[eCol.getKey().intValue()] += eCol.getValue().size();
		}

		return calcInterimLRT(a, part1SymCount, part2SymCount, advancedPooling, cr);
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the histogram bin distributions for splitting a transition. This done by splitting the set of {@link TimedTail}
	 * s in a {@link PDRTAState}
	 * 
	 * @param s
	 *            The {@link PDRTAState} for splitting
	 * @param mHist
	 *            The Set of {@link TimedTail}s to be split apart clustered by histogram index
	 * @return The {@link LikelihoodValue} of the histogram bin distributions for splitting a transition
	 */
	public static LikelihoodValue getLikelihoodRatioTime(PDRTAState s, Multimap<Integer, TimedTail> mHist, boolean advancedPooling, CalcRatio cr) {

		final StateStatistic st = s.getStat();
		final PDRTA a = s.getPDRTA();
		final int minData = PDRTA.getMinData();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
		if (SimplePDRTALearner.bOp[2].eval((st.totalOutCount - mHist.size()) < minData, mHist.size() < minData)) {
			return new LikelihoodValue();
		}

		final int[] part1TimeCount = Arrays.copyOf(st.timeCount, st.timeCount.length);
		final int[] part2TimeCount = new int[st.timeCount.length];
		for (final Entry<Integer, Collection<TimedTail>> eCol : mHist.asMap().entrySet()) {
			part1TimeCount[eCol.getKey().intValue()] -= eCol.getValue().size();
			part2TimeCount[eCol.getKey().intValue()] += eCol.getValue().size();
		}

		return calcInterimLRT(a, part1TimeCount, part2TimeCount, advancedPooling, cr);
	}

	public static LikelihoodValue getLikelihoodTime(PDRTAState s) {

		final StateStatistic st = s.getStat();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		return calcLikelihood(st.timeCount, st.totalOutCount);
	}

	public static LikelihoodValue getLikelihoodSym(PDRTAState s) {

		final StateStatistic st = s.getStat();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		return calcLikelihood(st.symbolCount, st.totalOutCount);
	}

	private static LikelihoodValue calcLikelihood(int[] count, int total) {

		double ratio = 0.0;
		int params = 0;
		for (int i = 0; i < count.length; i++) {
			if (count[i] > 0) {
				ratio += Math.log((double) count[i] / (double) total) * count[i];
				params++;
			}
		}

		// LRT_FIX : Check if params or count.length (example in paper says count.length)
		// Does not work with params -> Degrees of Freedom for compared states may not be equal then
		// *****
		// Tested whether or not to subtract 1 of the degrees of freedom
		// -> Results are equal and decided to use subtraction according to paper
		// -> Contradiction with Verwer's implementation of LRT with pooling
		if (params > 0) {
			return new LikelihoodValue(ratio, count.length - 1);
		} else {
			return new LikelihoodValue();
		}
	}

	/**
	 * Creates a deep copy of a given {@link StateStatistic}
	 * 
	 * @param st
	 *            The {@link StateStatistic} to be copied
	 */
	protected StateStatistic(StateStatistic st) {

		if (st.trainMode) {
			histBarSizes = st.histBarSizes;
			timeCount = Arrays.copyOf(st.timeCount, st.timeCount.length);
			symbolCount = Arrays.copyOf(st.symbolCount, st.symbolCount.length);
			totalOutCount = st.totalOutCount;
			totalInCount = st.totalInCount;
			trainMode = true;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Adds an incoming {@link TimedTail} to the statistics while training
	 * 
	 * @param t
	 *            The incoming {@link TimedTail} to be added
	 */
	protected void addToStats(TimedTail t) {

		if (trainMode && t != null) {
			totalInCount++;
			t = t.getNextTail();
			if (t != null) {
				totalOutCount++;
				symbolCount[t.getSymbolAlphIndex()]++;
				timeCount[t.getHistBarIndex()]++;
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Merges another {@link StateStatistic} into this statistic while training. This is used when two {@link PDRTAState}s are merged.
	 * 
	 * @param st
	 *            The {@link StateStatistic} to be merged
	 */
	public void merge(StateStatistic st) {

		assert (trainMode);
		assert (timeCount.length == st.timeCount.length);
		assert (symbolCount.length == st.symbolCount.length);
		assert (histBarSizes.length == st.histBarSizes.length);

		for (int i = 0; i < timeCount.length; i++) {
			timeCount[i] += st.timeCount[i];
		}
		for (int i = 0; i < symbolCount.length; i++) {
			symbolCount[i] += st.symbolCount[i];
		}
		totalOutCount += st.totalOutCount;
		totalInCount += st.totalInCount;
	}

	/**
	 * Adds the probability for a transition to be used by a {@link TimedTail} when reconstructing the {@link StateStatistic} from an already trained and
	 * persisted {@link PDRTA}
	 * 
	 * @param in
	 *            A transition
	 * @param prob
	 *            The probability corresponding to the transition
	 */
	protected void addInterval(int symIdx, Interval in, double prob) {

		if (!trainMode) {
			addToMap(symIdx, in, prob);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private void addToMap(int symIdx, Interval in, double prob) {
		TObjectDoubleHashMap<Interval> m;
		if (intervalProbs.containsKey(symIdx)) {
			m = intervalProbs.get(symIdx);
		} else {
			m = new TObjectDoubleHashMap<>();
			intervalProbs.put(symIdx, m);
		}
		m.put(in, prob);
	}

	/**
	 * Returns the probability for incoming {@link TimedTail}s to end in the {@link PDRTAState}
	 * 
	 * @return The probability for incoming {@link TimedTail}s to end in the {@link PDRTAState}
	 */
	protected double getTailEndProb() {

		if (trainMode) {
			return 1.0 - ((double) totalOutCount / (double) totalInCount);
		} else {
			return tailEndProb;
		}
	}

	/**
	 * Returns the probability for a given transition to be used by {@link TimedTail}s
	 * 
	 * @param in
	 *            The transition to get the probability for
	 * @return The probability the transition to be used by {@link TimedTail}s
	 */
	protected double getTransProb(int symIdx, Interval in) {

		if (trainMode) {
			return (double) in.getTails().size() / (double) totalOutCount;
		} else {
			if (intervalProbs.containsKey(symIdx)) {
				final TObjectDoubleHashMap<Interval> m = intervalProbs.get(symIdx);
				if (m.containsKey(in)) {
					return m.get(in);
				}
			}
			return 0.0;
		}
	}

	/**
	 * Returns the probability for a given {@link TimedTail} according to the independent symbol and histogram bin probabilities
	 * 
	 * @param t
	 *            The {@link TimedTail} to get the probability for
	 * @return The probability for a given {@link TimedTail} according to the independent symbol and histogram bin probabilities
	 */
	protected double[] getHistProb(TimedTail t) {

		if (t.getHistBarIndex() < 0 || t.getSymbolAlphIndex() < 0) {
			return new double[] { 0.0, 0.0 };
		}

		if (trainMode) {
			final double timeP = (double) timeCount[t.getHistBarIndex()] / (double) totalOutCount;
			final double symP = (double) symbolCount[t.getSymbolAlphIndex()] / (double) totalOutCount;
			return new double[] { symP, (timeP / histBarSizes[t.getHistBarIndex()]) };
		} else {
			final double timeP = (timeProbs[t.getHistBarIndex()] / histBarSizes[t.getHistBarIndex()]);
			final double symP = symbolProbs[t.getSymbolAlphIndex()];
			return new double[] { symP, timeP };
		}
	}

	/**
	 * Returns the number of outgoing {@link TimedTail}s while training
	 * 
	 * @return The number of outgoing {@link TimedTail}s
	 */
	public int getTotalOutEvents() {

		if (trainMode) {
			return totalOutCount;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns a {@link String} representation of the histogram bin distribution for persistence
	 * 
	 * @return The {@link String} representation of the histogram bin distribution
	 */
	protected String getTimeProbsString() {

		String s = "";
		if (trainMode) {
			int c;
			if (totalOutCount == 0) {
				c = Integer.MAX_VALUE;
			} else {
				c = totalOutCount;
			}
			double p = (double) timeCount[0] / (double) c;
			s += p;
			for (int i = 1; i < timeCount.length; i++) {
				p = (double) timeCount[i] / (double) c;
				s += " / " + p;
			}
		} else {
			s += timeProbs[0];
			for (int i = 1; i < timeProbs.length; i++) {
				s += " / " + timeProbs[i];
			}
		}
		return s;
	}

	/**
	 * Returns a {@link String} representation of the symbol distribution for persistence
	 * 
	 * @return The {@link String} representation of the symbol distribution
	 */
	protected String getSymbolProbsString() {

		String s = "";
		if (trainMode) {
			int c;
			if (totalOutCount == 0) {
				c = Integer.MAX_VALUE;
			} else {
				c = totalOutCount;
			}
			double p = (double) symbolCount[0] / (double) c;
			s += p;
			for (int i = 1; i < symbolCount.length; i++) {
				p = (double) symbolCount[i] / (double) c;
				s += " / " + p;
			}
		} else {
			s += symbolProbs[0];
			for (int i = 1; i < symbolProbs.length; i++) {
				s += " / " + symbolProbs[i];
			}
		}
		return s;
	}

	/**
	 * Creates an initial {@link StateStatistic} for training
	 * 
	 * @param numSymbols
	 *            The number of symbols from the input set
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 */
	private StateStatistic(int numSymbols, int[] histoBarSizes) {

		histBarSizes = histoBarSizes;
		timeCount = new int[histoBarSizes.length];
		symbolCount = new int[numSymbols];
		totalOutCount = 0;
		totalInCount = 0;
		trainMode = true;
	}

	/**
	 * Creates a {@link StateStatistic} reconstructed from a already trained and persisted {@link PDRTA}
	 * 
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 * @param timeProbs
	 *            The probabilities for outgoing {@link TimedTail}s for each histogram bin
	 * @param symbolProbs
	 *            The probabilities for outgoing {@link TimedTail}s for each symbol
	 * @param tailEndProb
	 *            The probability for incoming {@link TimedTail}s to end in the {@link PDRTAState}
	 */
	private StateStatistic(int[] histoBarSizes, double[] timeProbs, double[] symbolProbs, double tailEndProb) {

		histBarSizes = histoBarSizes;
		this.timeProbs = timeProbs;
		this.symbolProbs = symbolProbs;
		this.tailEndProb = tailEndProb;
		this.intervalProbs = new TIntObjectHashMap<>();
		this.trainMode = false;
	}

	/**
	 * Calculates the {@link LikelihoodValue} for two given sets of {@link TimedTail} counts in a {@link PDRTA}
	 * 
	 * @param a
	 *            The {@link PDRTA}
	 * @param v1
	 *            The first set of {@link TimedTail} counts
	 * @param v2
	 *            The second set of {@link TimedTail} counts
	 * @return The {@link LikelihoodValue} for two given sets of {@link TimedTail} counts
	 */
	private static LikelihoodValue calcInterimLRT(PDRTA a, int[] v1, int[] v2, boolean advancedPooling, CalcRatio cr) {

		double ratio = 0.0;
		int parameters = 0;
		final int minData = PDRTA.getMinData();

		assert (v1.length == v2.length);

		final TIntArrayList[] pooled = poolStats(v1, v2, minData, advancedPooling);
		final int total1 = pooled[0].get(pooled[0].size() - 1);
		final int total2 = pooled[1].get(pooled[1].size() - 1);

		// Calculating ratio and parameters
		for (int i = 0; i < (pooled[0].size() - 1); i++) {
			ratio += cr.calc(pooled[0].get(i), total1, pooled[1].get(i), total2);
		}
		// Minus 1 because sum of values is last item (not counting as parameter)
		parameters = pooled[0].size() - 1;

		// LRT_FIX : Thesis: parameters -1 only explained without pooling, Impl: parameters
		// parameters--;

		if (parameters >= 0) {
			return new LikelihoodValue(ratio, parameters);
		} else {
			return new LikelihoodValue();
		}
	}

	/**
	 * Calculates the Likelihood Ratio for two given {@link TimedTail} counts
	 * 
	 * @param v1
	 *            The first specific counts
	 * @param v1Total
	 *            The first total counts
	 * @param v2
	 *            The second specific counts
	 * @param v2Total
	 *            The second total counts
	 * @return The Likelihood Ratio for two given {@link TimedTail} counts
	 */
	public static double calcLRTRatio(int v1, int v1Total, int v2, int v2Total) {

		double v1Prob = 1.0;
		if (v1 > 0) {
			v1Prob = (double) v1 / (double) v1Total;
		}
		double v2Prob = 1.0;
		if (v2 > 0) {
			v2Prob = (double) v2 / (double) v2Total;
		}
		double bothProb = 1.0;
		if (v1 > 0 || v2 > 0) {
			bothProb = (double) (v1 + v2) / (double) (v1Total + v2Total);
		}

		double ratio = 0.0;
		ratio += v1 * Math.log(bothProb);
		ratio -= v1 * Math.log(v1Prob);
		ratio += v2 * Math.log(bothProb);
		ratio -= v2 * Math.log(v2Prob);

		// TODO Test double problem
		// double ratio2 = 0.0;
		// ratio2 += (v1 + v2) * Math.log(bothProb);
		// ratio2 -= v1 * Math.log(v1Prob);
		// ratio2 -= v2 * Math.log(v2Prob);
		// if (Double.compare(ratio, ratio2) != 0) {
		// System.out.println("ratio1 and ratio2 are not equal.\nratio1="
		// + ratio + "\nratio2=" + ratio2);
		// }
		return ratio;
	}

	public static double calcFMRatio(int v1, int v1Total, int v2, int v2Total) {

		final int minData = PDRTA.getMinData();

		final double total = v1 + v2;
		final double expected1 = (v1Total * total) / (v1Total + v2Total);
		final double expected2 = (v2Total * total) / (v1Total + v2Total);

		double top1 = v1 - expected1;
		double top2 = v2 - expected2;

		// Yates correction for continuity
		if (v1 < minData || v2 < minData) {
			if (top1 < 0) {
				top1 = -top1;
			}
			top1 -= 0.5;
			if (top2 < 0) {
				top2 = -top2;
			}
			top2 -= 0.5;
		}

		// FIXME Check what happens if expected is 0 => Is NaN ok?
		return ((top1 * top1) / expected1) + ((top2 * top2) / expected2);
	}

	private static TIntArrayList[] poolStats(int[] stat1, int[] stat2, int minData, boolean advanced) {

		assert (stat1.length == stat2.length);

		final TIntArrayList pooled1 = new TIntArrayList(stat1.length + 1);
		final TIntArrayList pooled2 = new TIntArrayList(stat1.length + 1);

		int iPool = 0;
		pooled1.add(0);
		pooled2.add(0);

		int sum1 = 0;
		int sum2 = 0;

		for (int i = 0; i < stat1.length; i++) {
			sum1 += stat1[i];
			sum2 += stat2[i];
			// POOL_FIX : Operator for pooling (thesis: AND, impl: AND, own: AND)
			if (SimplePDRTALearner.bOp[0].eval(stat1[i] < minData, stat2[i] < minData)) {
				// Number of sequences is less than minData
				if (advanced && !SimplePDRTALearner.bOp[0].eval(pooled1.get(iPool) < minData, pooled2.get(iPool) < minData)) {
					// Close full pool and open new pool
					iPool = pooled1.size();
					pooled1.add(stat1[i]);
					pooled2.add(stat2[i]);
				} else {
					// Add to existing pool
					pooled1.set(iPool, pooled1.get(iPool) + stat1[i]);
					pooled2.set(iPool, pooled2.get(iPool) + stat2[i]);
				}
			} else {
				// Number of sequences is more or equal minData
				pooled1.add(stat1[i]);
				pooled2.add(stat2[i]);
			}
		}

		// Discard small pools
		// POOL_FIX : Operator for pool discarding (thesis: missing, impl: [LRT: OR, FM: AND], own: AND)
		if (SimplePDRTALearner.bOp[1].eval(pooled1.get(iPool) < minData, pooled2.get(iPool) < minData)) {
			sum1 -= pooled1.removeAt(iPool);
			sum2 -= pooled2.removeAt(iPool);
		}

		// Append sum of values
		pooled1.add(sum1);
		pooled2.add(sum2);

		assert (pooled1.size() == pooled2.size());

		final TIntArrayList[] pools = new TIntArrayList[2];
		pools[0] = pooled1;
		pools[1] = pooled2;
		return pools;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(histBarSizes);
		result = prime * result + ((intervalProbs == null) ? 0 : intervalProbs.hashCode());
		result = prime * result + Arrays.hashCode(symbolCount);
		result = prime * result + Arrays.hashCode(symbolProbs);
		long temp;
		temp = Double.doubleToLongBits(tailEndProb);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(timeCount);
		result = prime * result + Arrays.hashCode(timeProbs);
		result = prime * result + totalInCount;
		result = prime * result + totalOutCount;
		result = prime * result + (trainMode ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final StateStatistic other = (StateStatistic) obj;
		if (!Arrays.equals(histBarSizes, other.histBarSizes)) {
			return false;
		}
		if (intervalProbs == null) {
			if (other.intervalProbs != null) {
				return false;
			}
		} else if (!intervalProbs.equals(other.intervalProbs)) {
			return false;
		}
		if (!Arrays.equals(symbolCount, other.symbolCount)) {
			return false;
		}
		if (!Arrays.equals(symbolProbs, other.symbolProbs)) {
			return false;
		}
		if (Double.doubleToLongBits(tailEndProb) != Double.doubleToLongBits(other.tailEndProb)) {
			return false;
		}
		if (!Arrays.equals(timeCount, other.timeCount)) {
			return false;
		}
		if (!Arrays.equals(timeProbs, other.timeProbs)) {
			return false;
		}
		if (totalInCount != other.totalInCount) {
			return false;
		}
		if (totalOutCount != other.totalOutCount) {
			return false;
		}
		if (trainMode != other.trainMode) {
			return false;
		}
		return true;
	}

	void cleanUp(PDRTAState s) {

		intervalProbs = new TIntObjectHashMap<>();
		for (int i = 0; i < symbolCount.length; i++) {
			final NavigableMap<Integer, Interval> ins = s.getIntervals(i);
			for (final Interval in : ins.values()) {
				addToMap(i, in, getTransProb(i, in));
			}
		}

		symbolProbs = new double[symbolCount.length];
		timeProbs = new double[histBarSizes.length];
		if (totalOutCount > 0) {
			for (int i = 0; i < symbolCount.length; i++) {
				symbolProbs[i] = (double) symbolCount[i] / (double) totalOutCount;
			}
			for (int i = 0; i < histBarSizes.length; i++) {
				timeProbs[i] = (double) timeCount[i] / (double) totalOutCount;
			}
		} else {
			Arrays.fill(symbolProbs, 0.0);
			Arrays.fill(timeProbs, 0.0);
		}

		tailEndProb = getTailEndProb();

		trainMode = false;
		timeCount = null;
		symbolCount = null;
		totalOutCount = -1;
		totalInCount = -1;
	}

	public interface CalcRatio {
		double calc(int v1, int t1, int v2, int t2);
	}

}
