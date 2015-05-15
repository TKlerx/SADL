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

package sadl.models.pdrta;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import sadl.modellearner.rtiplus.tester.LikelihoodValue;

import com.google.common.collect.Multimap;

/**
 * This class manages the time and symbol probabilities for a {@link PDRTAState}
 * which are needed for calculating the Likelihood Ratio Test and anomaly
 * detection. It also provides static methods for calculating the Likelihood
 * Ratio.
 * 
 * @author Fabian Witter
 * 
 */
public class StateStatistic {

	/**
	 * Contains the sizes (width) of the histogram bins
	 */
	private int[] histBarSizes;

	/**
	 * Contains the number of outgoing {@link TimedTail}s for each histogram bin
	 * while training
	 */
	private int[] timeCount;

	/**
	 * Contains the number of outgoing {@link TimedTail}s for each symbol while
	 * training
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
	 * Determines if this {@link StateStatistic} exists while training or during
	 * anomaly detection
	 */
	private boolean trainMode;

	/**
	 * Contains the probabilities for outgoing {@link TimedTail}s for each
	 * histogram bin during anomaly detection
	 */
	private double[] timeProbs;

	/**
	 * Contains the probabilities for outgoing {@link TimedTail}s for each
	 * symbol during anomaly detection
	 */
	private double[] symbolProbs;

	/**
	 * Contains the probability for incoming {@link TimedTail}s to end in the
	 * {@link PDRTAState} during anomaly detection
	 */
	private double tailEndProb;

	/**
	 * Contains the probabilities for transitions to be used by
	 * {@link TimedTail}s during anomaly detection
	 */
	private TObjectDoubleHashMap<Interval> intervalProbs;

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
	 * Creates a {@link StateStatistic} reconstructed from a already trained and
	 * persisted {@link PDRTA}
	 * 
	 * @param alphSize
	 *            The number of symbols
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 * @param stats
	 *            A set containing the probabilities embedded in formated
	 *            {@link String}s
	 * @return A {@link StateStatistic} reconstructed from a already trained and
	 *         persisted {@link PDRTA}
	 */
	protected static StateStatistic reconstructStat(int alphSize,
			int[] histoBarSizes, Collection<String> stats) {

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
					throw new IllegalArgumentException(
							"Content is not correct!");
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
					throw new IllegalArgumentException(
							"Content is not correct!");
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

		return new StateStatistic(histoBarSizes, timeProbs, symbolProbs,
				tailEndProb);
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the symbol distributions for
	 * merging two {@link PDRTAState}s
	 * 
	 * @param s1
	 *            First {@link PDRTAState} for merging
	 * @param s2
	 *            Second {@link PDRTAState} for merging
	 * @return The {@link LikelihoodValue} of the symbol distributions for
	 *         merging
	 */
	public static LikelihoodValue getLikelihoodRatioSym(PDRTAState s1,
			PDRTAState s2, boolean advancedPooling) {

		final StateStatistic st1 = s1.getStat();
		final StateStatistic st2 = s2.getStat();
		final PDRTA a = s1.getAutomaton();
		final int minData = PDRTA.getMinData();

		if (!st1.trainMode || !st2.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : || -> &&
		if (st1.totalOutCount < minData && st2.totalOutCount < minData) {
			return new LikelihoodValue(0.0, 0);
		} else {
			return calcInterimLRT(a, st1.symbolCount, st2.symbolCount,
					advancedPooling);
		}
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the histogram bin distributions
	 * for merging two {@link PDRTAState}s
	 * 
	 * @param s1
	 *            First {@link PDRTAState} for merging
	 * @param s2
	 *            Second {@link PDRTAState} for merging
	 * @return The {@link LikelihoodValue} of the histogram bin distributions
	 *         for merging
	 */
	public static LikelihoodValue getLikelihoodRatioTime(PDRTAState s1,
			PDRTAState s2, boolean advancedPooling) {

		final StateStatistic st1 = s1.getStat();
		final StateStatistic st2 = s2.getStat();
		final PDRTA a = s1.getAutomaton();
		final int minData = PDRTA.getMinData();

		if (!st1.trainMode || !st2.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : || -> &&
		if (st1.totalOutCount < minData && st2.totalOutCount < minData) {
			return new LikelihoodValue(0.0, 0);
		} else {
			return calcInterimLRT(a, st1.timeCount, st2.timeCount,
					advancedPooling);
		}
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the symbol distributions for
	 * splitting a transition. This done by splitting the set of
	 * {@link TimedTail}s in a {@link PDRTAState}
	 * 
	 * @param s
	 *            The {@link PDRTAState} for splitting
	 * @param mSym
	 *            The Set of {@link TimedTail}s to be split apart clustered by
	 *            symbol index
	 * @return The {@link LikelihoodValue} of the symbol distributions for
	 *         splitting a transition
	 */
	public static LikelihoodValue getLikelihoodRatioSym(PDRTAState s,
			Multimap<Integer, TimedTail> mSym, boolean advancedPooling) {

		final StateStatistic st = s.getStat();
		final PDRTA a = s.getAutomaton();
		final int minData = PDRTA.getMinData();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : || -> &&
		if ((st.totalOutCount - mSym.size()) < minData && mSym.size() < minData) {
			return new LikelihoodValue(0.0, 0);
		}

		final int[] part1SymCount = Arrays.copyOf(st.symbolCount,
				st.symbolCount.length);
		final int[] part2SymCount = new int[st.symbolCount.length];
		for (final Entry<Integer, Collection<TimedTail>> eCol : mSym.asMap()
				.entrySet()) {
			part1SymCount[eCol.getKey()] -= eCol.getValue().size();
			part2SymCount[eCol.getKey()] += eCol.getValue().size();
		}

		return calcInterimLRT(a, part1SymCount, part2SymCount, advancedPooling);
	}

	/**
	 * Calculates the {@link LikelihoodValue} of the histogram bin distributions
	 * for splitting a transition. This done by splitting the set of
	 * {@link TimedTail}s in a {@link PDRTAState}
	 * 
	 * @param s
	 *            The {@link PDRTAState} for splitting
	 * @param mHist
	 *            The Set of {@link TimedTail}s to be split apart clustered by
	 *            histogram index
	 * @return The {@link LikelihoodValue} of the histogram bin distributions
	 *         for splitting a transition
	 */
	public static LikelihoodValue getLikelihoodRatioTime(PDRTAState s,
			Multimap<Integer, TimedTail> mHist, boolean advancedPooling) {

		final StateStatistic st = s.getStat();
		final PDRTA a = s.getAutomaton();
		final int minData = PDRTA.getMinData();

		if (!st.trainMode) {
			throw new UnsupportedOperationException();
		}

		// LRT_FIX : || -> &&
		if ((st.totalOutCount - mHist.size()) < minData
				&& mHist.size() < minData) {
			return new LikelihoodValue(0.0, 0);
		}

		final int[] part1TimeCount = Arrays.copyOf(st.timeCount, st.timeCount.length);
		final int[] part2TimeCount = new int[st.timeCount.length];
		for (final Entry<Integer, Collection<TimedTail>> eCol : mHist.asMap()
				.entrySet()) {
			part1TimeCount[eCol.getKey()] -= eCol.getValue().size();
			part2TimeCount[eCol.getKey()] += eCol.getValue().size();
		}

		return calcInterimLRT(a, part1TimeCount, part2TimeCount,
				advancedPooling);
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
	 * Merges another {@link StateStatistic} into this statistic while training.
	 * This is used when two {@link PDRTAState}s are merged.
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
	 * Adds the probability for a transition to be used by a {@link TimedTail}
	 * when reconstructing the {@link StateStatistic} from an already trained
	 * and persisted {@link PDRTA}
	 * 
	 * @param in
	 *            A transition
	 * @param prob
	 *            The probability corresponding to the transition
	 */
	protected void addInterval(Interval in, double prob) {

		if (!trainMode) {
			intervalProbs.put(in, prob);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns the probability for incoming {@link TimedTail}s to end in the
	 * {@link PDRTAState}
	 * 
	 * @return The probability for incoming {@link TimedTail}s to end in the
	 *         {@link PDRTAState}
	 */
	protected double getTailEndProb() {

		if (trainMode) {
			return 1.0 - ((double) totalOutCount / (double) totalInCount);
		} else {
			return tailEndProb;
		}
	}

	/**
	 * Returns the probability for a given transition to be used by
	 * {@link TimedTail}s
	 * 
	 * @param in
	 *            The transition to get the probability for
	 * @return The probability the transition to be used by {@link TimedTail}s
	 */
	protected double getTransProb(Interval in) {

		if (trainMode) {
			return (double) in.getTails().size() / (double) totalOutCount;
		} else {
			if (intervalProbs.contains(in)) {
				return intervalProbs.get(in);
			} else {
				return 0.0;
			}
		}
	}

	/**
	 * Returns the probability for a given {@link TimedTail} according to the
	 * independent symbol and histogram bin probabilities
	 * 
	 * @param t
	 *            The {@link TimedTail} to get the probability for
	 * @return The probability for a given {@link TimedTail} according to the
	 *         independent symbol and histogram bin probabilities
	 */
	protected double getHistProb(TimedTail t) {

		if (t.getHistBarIndex() < 0 || t.getSymbolAlphIndex() < 0) {
			return 0.0;
		}

		if (trainMode) {
			final double timeP = (double) timeCount[t.getHistBarIndex()]
					/ (double) totalOutCount;
			final double symP = (double) symbolCount[t.getSymbolAlphIndex()]
					/ (double) totalOutCount;
			return symP * (timeP / histBarSizes[t.getHistBarIndex()]);
		} else {
			return symbolProbs[t.getSymbolAlphIndex()]
					* (timeProbs[t.getHistBarIndex()] / histBarSizes[t
					                                                 .getHistBarIndex()]);
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
	 * Returns a {@link String} representation of the histogram bin distribution
	 * for persistence
	 * 
	 * @return The {@link String} representation of the histogram bin
	 *         distribution
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
	 * Returns a {@link String} representation of the symbol distribution for
	 * persistence
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
	 * Creates a {@link StateStatistic} reconstructed from a already trained and
	 * persisted {@link PDRTA}
	 * 
	 * @param histoBarSizes
	 *            The sizes of the histogram bins
	 * @param timeProbs
	 *            The probabilities for outgoing {@link TimedTail}s for each
	 *            histogram bin
	 * @param symbolProbs
	 *            The probabilities for outgoing {@link TimedTail}s for each
	 *            symbol
	 * @param tailEndProb
	 *            The probability for incoming {@link TimedTail}s to end in the
	 *            {@link PDRTAState}
	 */
	private StateStatistic(int[] histoBarSizes, double[] timeProbs,
			double[] symbolProbs, double tailEndProb) {

		histBarSizes = histoBarSizes;
		this.timeProbs = timeProbs;
		this.symbolProbs = symbolProbs;
		this.tailEndProb = tailEndProb;
		this.intervalProbs = new TObjectDoubleHashMap<>();
		this.trainMode = false;
	}

	/**
	 * Calculates the {@link LikelihoodValue} for two given sets of
	 * {@link TimedTail} counts in a {@link PDRTA}
	 * 
	 * @param a
	 *            The {@link PDRTA}
	 * @param v1
	 *            The first set of {@link TimedTail} counts
	 * @param v2
	 *            The second set of {@link TimedTail} counts
	 * @return The {@link LikelihoodValue} for two given sets of
	 *         {@link TimedTail} counts
	 */
	private static LikelihoodValue calcInterimLRT(PDRTA a, int[] v1,
			int[] v2, boolean advancedPooling) {

		double ratio = 0.0;
		int parameters = 0;
		final int minData = PDRTA.getMinData();

		assert (v1.length == v2.length);

		final TIntArrayList[] pooled = poolStats(v1, v2, minData, advancedPooling);
		final int total1 = pooled[0].get(pooled[0].size() - 1);
		final int total2 = pooled[1].get(pooled[1].size() - 1);

		// Calculating ratio and parameters
		for (int i = 0; i < (pooled[0].size() - 1); i++) {
			ratio += calcRatio(pooled[0].get(i), total1, pooled[1].get(i),
					total2);
		}
		parameters = pooled[0].size() - 1;

		// LRT_FIX : parameters -1
		parameters--;

		if (parameters > 0) {
			return new LikelihoodValue(ratio, parameters);
		} else {
			// LRT_FIX : why not return negative parameters?
			return new LikelihoodValue(0.0, 0);
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
	private static double calcRatio(int v1, int v1Total, int v2, int v2Total) {

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

	private static TIntArrayList[] poolStats(int[] stat1, int[] stat2,
			int minData, boolean advanced) {

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
			if (stat1[i] < minData && stat2[i] < minData) {
				// Number of sequences is less than minData
				if (advanced
						&& (pooled1.get(iPool) >= minData || pooled2.get(iPool) >= minData)) {
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
		// POOL_FIX : || -> && ?
		if (pooled1.get(iPool) < minData || pooled2.get(iPool) < minData) {
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
}
