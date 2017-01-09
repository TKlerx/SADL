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

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class IQROutlierAnalysis extends OutlierDistanceAnalysis {

	private final boolean onlyFarOuts;

	public IQROutlierAnalysis(double strength, boolean onlyFarOuts) {
		this(strength, onlyFarOuts, null, -1);
	}

	public IQROutlierAnalysis(double strength, boolean onlyFarOuts, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
		this.onlyFarOuts = onlyFarOuts;
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		distValues.sort();
		final double q1 = StatisticsUtil.calculateQ1(distValues, false);
		final double q3 = StatisticsUtil.calculateQ3(distValues, false);
		final double coeff = onlyFarOuts ? 3.0 : 1.5;
		return (int) Math.ceil(((q3 + (q3 - q1) * coeff) / 2.0));
	}

}
