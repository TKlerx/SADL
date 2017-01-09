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
package sadl.modellearner.rtiplus.analysis;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TIntLinkedList;

public abstract class OutlierDistanceAnalysis extends DistributionAnalysis {

	private final double factor;

	public OutlierDistanceAnalysis(double strength, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(fewElementsAnalysis, fewElementsLimit);

		if (strength <= 0.0) {
			throw new IllegalArgumentException("The strength must be positive!");
		}
		this.factor = 1.0 / strength;
	}

	@Override
	TIntList analyzeDistribution(TIntList values, TIntList frequencies, int begin, int end) {

		final TDoubleList distValues = new TDoubleArrayList(values.size() - 1);
		final TIntIterator it = values.iterator();
		if (it.hasNext()) {
			int prev = it.next();
			while (it.hasNext()) {
				final int curr = it.next();
				distValues.add(curr - prev - 1);
				prev = curr;
			}
		}

		final int outlierDist = getOutlierDistance(distValues);
		final int tolerance = (int) Math.rint(factor * outlierDist);

		final TIntList splits = new TIntLinkedList();

		final TIntIterator it2 = values.iterator();
		if (it2.hasNext()) {
			int t = it2.next();
			splits.add(t - tolerance - 1);
			while (it2.hasNext()) {
				final int t2 = it2.next();
				final int diff = t2 - t - 1;
				if (diff > 2 * tolerance) {
					splits.add(t + tolerance);
					splits.add(t2 - tolerance - 1);
				}
				t = t2;
			}
			splits.add(t + tolerance);
		}
		return splits;
	}

	abstract int getOutlierDistance(TDoubleList distValues);

}
