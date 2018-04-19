/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
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

public class FrequencyAnalysis extends DistributionAnalysis {

	private final double rangeRatio;
	private final int trustedFrequency;

	public FrequencyAnalysis(int trustedFrequency, double rangeRatio) {
		this(trustedFrequency, rangeRatio, null, -1);
	}

	public FrequencyAnalysis(int trustedFrequency, double rangeRatio, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(fewElementsAnalysis, fewElementsLimit);

		if (trustedFrequency < 0) {
			throw new IllegalArgumentException("Trusted frequency value must not be negative");
		}
		if (rangeRatio < 0.0 || rangeRatio > 1.0) {
			throw new IllegalArgumentException("The range ratio must be in [0,1]");
		}

		this.trustedFrequency = trustedFrequency;
		this.rangeRatio = rangeRatio;
	}

	@Override
	TIntList analyzeDistribution(TIntList values, TIntList frequencies, int begin, int end) {

		final TIntList splits = new TIntArrayList(values.size() * 2);

		final int range = end - begin + 1;

		int prevVal = values.get(0);
		int prevTol = getTolerance(frequencies.get(0), range);
		splits.add(prevVal - prevTol - 1);
		for (int i = 1; i < values.size(); i++) {
			final int val = values.get(i);
			final int tol = getTolerance(frequencies.get(i), range);
			if ((prevTol + tol) < (val - prevVal - 1)) {
				splits.add(prevVal + prevTol);
				splits.add(val - tol - 1);
			}
			prevVal = val;
			prevTol = tol;
		}
		splits.add(prevVal + prevTol);

		return splits;
	}

	private int getTolerance(int freq, int range) {

		if (freq < trustedFrequency) {
			final double trustRate = (double) freq / (double) trustedFrequency;
			return (int) Math.rint(range * rangeRatio * (1.0 - trustRate));
		} else {
			return 0;
		}
	}

}
