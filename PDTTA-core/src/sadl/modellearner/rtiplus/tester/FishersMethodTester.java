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
package sadl.modellearner.rtiplus.tester;

import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;

/**
 * 
 * @author Fabian Witter
 *
 */
public class FishersMethodTester extends LikelihoodRatioTester {

	private static final double MAX_P_VALUE = 1.0 - 0.1e-100;
	private static final double MIN_P_VALUE = 0.1e-100;

	public FishersMethodTester(boolean advancedPooling) {
		super(advancedPooling);
	}

	@Override
	public double testSplit(PDRTAState red, int symAlphIdx, int time) {

		final LikelihoodValue lv = intTestSplit(red, symAlphIdx, time, StateStatistic::calcFMRatio);
		return applyFM(lv);
	}

	@Override
	public double testMerge(PDRTAState red, PDRTAState blue) {

		final LikelihoodValue lv = intTestMerge(red, blue, StateStatistic::calcFMRatio);
		return applyFM(lv);
	}

	private double applyFM(LikelihoodValue lv) {

		final LikelihoodValue lv2 = new LikelihoodValue();
		for (int i = 0; i < lv.ratios.size(); i++) {
			double z = compareToChiSquared(new LikelihoodValue(lv.ratios.get(i), lv.additionalParams.get(i)));
			if (z < MIN_P_VALUE) {
				z = MIN_P_VALUE;
			} else if (z > MAX_P_VALUE) {
				z = MAX_P_VALUE;
			}
			lv2.add(new LikelihoodValue(-2.0 * Math.log(z), 2));
		}
		return compareToChiSquared(lv2);
	}

}
