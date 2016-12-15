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

package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.procedure.TIntProcedure;

public class QuantileAnalysis extends DistributionAnalysis {

	private final int numQuantiles;

	public QuantileAnalysis(int numQuantiles) {
		this(numQuantiles, null, -1);
	}

	public QuantileAnalysis(int numQuantiles, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(fewElementsAnalysis, fewElementsLimit);

		if (numQuantiles <= 0) {
			throw new IllegalArgumentException("The number of quantiles have to be greater than zero!");
		}

		this.numQuantiles = numQuantiles;
	}

	@Override
	TIntList analyzeDistribution(TIntList values, TIntList frequency, int begin, int end) {

		final TIntList result = new TIntArrayList();
		final TIntList indexes = computeIndexes(frequency);
		if (indexes.size() > 0) {
			final double[] splitIndexes = calcSplitIndexes(indexes.get(indexes.size() - 1));
			int j = 0;
			int lastValue = -1;
			for (int i = 0; i < indexes.size(); i++) {
				if (j == splitIndexes.length) {
					break;
				}
				if (indexes.get(i) < splitIndexes[j] && splitIndexes[j] < (indexes.get(i) + 1)) {
					// Split index lies between two values
					final double vFloor = values.get(i);
					final double vCeil = values.get(i + 1);
					final double ratioFloor = splitIndexes[j] - indexes.get(i);
					final double ratioCeil = 1.0 - ratioFloor;
					final int val = (int) Math.rint((vFloor * ratioFloor) + (vCeil * ratioCeil));
					if (lastValue < val) {
						result.add(val);
						lastValue = val;
					}
					j++;
					i--; // Do not increase i
				} else if (splitIndexes[j] <= indexes.get(i)) {
					// Split index lies inside the range of a single value
					final int val = values.get(i);
					if (lastValue < val) {
						result.add(val);
						lastValue = val;
					}
					j++;
					i--; // Do not increase i
				}
			}
		}
		return result;
	}

	/**
	 * Computes the indexes of the values as if for each value {@code v_i} the number of entities specified in the frequencies list {@code f_i} existed in an
	 * artificial list. The returned list of indexes {@code l} contains the last index of value {@code v_i} in that artificial list at position {@code i}.
	 * 
	 * @param frequencies
	 *            The frequencies of values
	 * @return The list of indexes
	 * @throws {@link
	 *             IllegalArgumentException} if a frequency is smaller or equal {@code 0}
	 */
	private TIntList computeIndexes(TIntList frequencies) {

		final TIntList indexes = new TIntLinkedList();
		final boolean greaterZero = frequencies.forEach(new TIntProcedure() {
			int i = -1;

			@Override
			public boolean execute(int v) {
				if (v > 0) {
					i += v;
					indexes.add(i);
					return true;
				} else {
					return false;
				}
			}
		});
		if (greaterZero) {
			return indexes;
		} else {
			throw new IllegalArgumentException("Not all density values are greater than zero!");
		}
	}

	/**
	 * Computes the (floating point) split indexes of the artificial expanded list of values such that the split indexes are equally distributed over the range
	 * of indexes
	 * 
	 * @param maxIdx
	 *            The maximum index. The range of indexes will be defined from {@code 0} to {@code maxIdx}.
	 * @return The (floating point) split indexes
	 */
	private double[] calcSplitIndexes(int maxIdx) {

		final double[] borderIndexes = new double[numQuantiles - 1];
		for (int i = 1; i < numQuantiles; i++) {
			borderIndexes[i - 1] = ((double) i / (double) numQuantiles) * maxIdx;
		}
		return borderIndexes;
	}

}
