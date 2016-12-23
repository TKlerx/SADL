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
import gnu.trove.list.linked.TIntLinkedList;

public abstract class DistributionAnalysis {

	private final DistributionAnalysis fewElementsAnalysis;
	private final int fewElementsLimit;

	public DistributionAnalysis(DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		this.fewElementsAnalysis = fewElementsAnalysis;
		this.fewElementsLimit = fewElementsLimit;
	}

	public final TIntList performAnalysis(TIntList values, TIntList frequencies, int begin, int end) {

		if (values == null || frequencies == null || values.size() != frequencies.size()) {
			throw new IllegalArgumentException("The lists for values and desitiy have to be of the same size!");
		}

		if (values.size() == 0) {
			return new TIntLinkedList(0);
		}

		final TIntList result;
		if (values.size() <= fewElementsLimit && fewElementsAnalysis != null) {
			result = fewElementsAnalysis.analyzeDistribution(values, frequencies, begin, end);
		} else {
			result = analyzeDistribution(values, frequencies, begin, end);
		}

		while (result.size() > 0 && result.get(0) < begin) {
			result.removeAt(0);
		}
		while (result.size() > 0 && result.get(result.size() - 1) >= end) {
			result.removeAt(result.size() - 1);
		}

		return result;
	}

	abstract TIntList analyzeDistribution(TIntList values, TIntList frequencies, int begin, int end);

}
