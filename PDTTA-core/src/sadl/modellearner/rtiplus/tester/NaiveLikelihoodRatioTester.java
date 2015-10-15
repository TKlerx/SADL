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

package sadl.modellearner.rtiplus.tester;

import jsat.distributions.ChiSquared;
import sadl.modellearner.rtiplus.OperationUtil;
import sadl.modellearner.rtiplus.StateColoring;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;

/**
 * 
 * @author Fabian Witter
 *
 */
public class NaiveLikelihoodRatioTester implements OperationTester {

	private StateColoring stateColoring;

	@Override
	public double testSplit(PDRTAState red, int symAlphIdx, int time) {

		final PDRTAState t = red.getTarget(symAlphIdx, time);
		assert (t != null);

		if (!stateColoring.isRed(red)) {
			throw new IllegalArgumentException("Source must be red!");
		} else if (!stateColoring.isBlue(t)) {
			throw new IllegalArgumentException("Target must be blue!");
		}

		final PDRTA a = red.getPDRTA();

		final PDRTA cA = new PDRTA(a);
		final PDRTAState cRed = cA.getState(red.getIndex());
		final StateColoring cColoring = new StateColoring(stateColoring, cA);

		OperationUtil.split(cRed, symAlphIdx, time, cColoring);

		return makeTest(calcLikelihood(a), calcLikelihood(cA));
	}

	@Override
	public double testMerge(PDRTAState red, PDRTAState blue) {

		if (!stateColoring.isRed(red)) {
			throw new IllegalArgumentException("First state must be red!");
		} else if (!stateColoring.isBlue(blue)) {
			throw new IllegalArgumentException("Second state must be blue!");
		}

		final PDRTA a = red.getPDRTA();
		assert (a == blue.getPDRTA());

		final PDRTA cA = new PDRTA(a);
		final PDRTAState cRed = cA.getState(red.getIndex());
		final PDRTAState cBlue = cA.getState(blue.getIndex());
		final StateColoring cColoring = new StateColoring(stateColoring, cA);

		OperationUtil.merge(cRed, cBlue, cColoring, false, false, null);

		return makeTest(calcLikelihood(cA), calcLikelihood(a));
	}

	@Override
	public void setColoring(StateColoring sc) {
		stateColoring = sc;
	}

	public static LikelihoodValue calcLikelihood(PDRTA a) {

		final LikelihoodValue lv = new LikelihoodValue();
		for (final PDRTAState s : a.getStates()) {
			lv.add(StateStatistic.getLikelihoodSym(s));
			lv.add(StateStatistic.getLikelihoodTime(s));
		}
		return lv;
	}

	private double makeTest(LikelihoodValue lvGeneral, LikelihoodValue lvSpecific) {

		final double ratio = lvGeneral.getRatio() - lvSpecific.getRatio();
		final int dof = lvSpecific.getParam() - lvGeneral.getParam();

		if (dof > 0) {
			final ChiSquared c = new ChiSquared(dof);
			return 1.0 - c.cdf(-2.0 * ratio);
		} else {
			return -1.0;
		}
	}

}
