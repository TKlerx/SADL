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

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class MADOutlierAnalysis extends OutlierDistanceAnalysis {

	public enum MADConservatism {
		VERY_CONSERVATIVE(3.0), MODERATELY_CONSERVATIVE(2.5), POORLY_CONSERVATIVE(2.0);

		private final double coeff;

		private MADConservatism(double coeff) {
			this.coeff = coeff;
		}

		public double getCoefficent() {
			return this.coeff;
		}
	}

	private final MADConservatism coeff;

	public MADOutlierAnalysis(double strength, MADConservatism coeff) {
		this(strength, coeff, null, -1);
	}

	public MADOutlierAnalysis(double strength, MADConservatism coeff, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
		this.coeff = coeff;
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		final double median = StatisticsUtil.calculateMedian(distValues, true);
		final double mad = StatisticsUtil.calculateMAD(distValues, median);
		return (int) Math.ceil(((median + coeff.getCoefficent() * mad) / 2.0));
	}

}
