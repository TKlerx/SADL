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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import jsat.distributions.ChiSquared;
import sadl.modellearner.rtiplus.OperationUtil;
import sadl.modellearner.rtiplus.StateColoring;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;
import sadl.models.pdrta.TimedTail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * @author Fabian Witter
 *
 */
public class LikelihoodRatioTester implements OperationTester {

	private StateColoring stateColoring;
	private final boolean advancedPooling;

	public LikelihoodRatioTester(boolean advancedPooling) {
		this.advancedPooling = advancedPooling;
	}

	@Override
	public double testSplit(PDRTAState s, int symAlphIdx, int time) {

		final PDRTAState t = s.getTarget(symAlphIdx, time);
		assert (t != null);

		if (!stateColoring.isRed(s)) {
			throw new IllegalArgumentException("s must be red!");
		} else if (!stateColoring.isBlue(t)) {
			throw new IllegalArgumentException("Target must be blue!");
		}

		// Abort because LRT will never be calculated for any state in the tree
		if (t.getTotalOutEvents() < (2 * PDRTA.getMinData())) {
			return -1.0;
		}

		final Multimap<Integer, TimedTail> mHist = HashMultimap.create();
		final Multimap<Integer, TimedTail> mSym = HashMultimap.create();
		final Set<Entry<Integer, TimedTail>> tails = s.getInterval(symAlphIdx, time).getTails().entries();
		for (final Entry<Integer, TimedTail> eT : tails) {
			if (eT.getKey() <= time && eT.getValue().getNextTail() != null) {
				mHist.put(eT.getValue().getNextTail().getHistBarIndex(), eT.getValue().getNextTail());
				mSym.put(eT.getValue().getNextTail().getSymbolAlphIndex(), eT.getValue().getNextTail());
			}
		}

		final LikelihoodValue lv = new LikelihoodValue(0.0, 0);
		lv.add(intTestSplit(t, mHist, mSym));

		// TODO delete. only for debug
		// System.out.println("p=" + (-2.0 * lv.ratio) + " , df="
		// + lv.additionalParam);

		return compareToChiSquared(lv);
	}

	private double compareToChiSquared(LikelihoodValue lv) {

		if (lv.additionalParam > 0) {
			final ChiSquared c = new ChiSquared(lv.additionalParam);
			return 1.0 - c.cdf(-2.0 * lv.ratio);
		} else {
			return -1.0;
		}
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

		// LRT_FIX Deleted because of new && condition
		// if (blue.getTotalOutEvents() < minData) {
		// return -1.0;
		// }

		final PDRTA cA = new PDRTA(a);
		final PDRTAState cR = cA.getState(red.getIndex());
		final PDRTAState cB = cA.getState(blue.getIndex());

		final StateColoring cColoring = new StateColoring(stateColoring, cA);

		final LikelihoodValue lv = new LikelihoodValue(0.0, 0);
		lv.add(OperationUtil.merge(cR, cB, cColoring, true, advancedPooling));

		// TODO delete. only for debug
		// System.out.println("p=" + (-2.0 * lv.ratio) + " , df="
		// + lv.additionalParam);

		return compareToChiSquared(lv);
	}

	/**
	 * Belongs to testSplit_C
	 * 
	 * @param s
	 * @param m
	 * @return
	 */
	private LikelihoodValue intTestSplit(PDRTAState s, Multimap<Integer, TimedTail> mHist, Multimap<Integer, TimedTail> mSym) {

		final PDRTA a = s.getPDRTA();

		final LikelihoodValue lv = new LikelihoodValue(0.0, 0);
		lv.add(StateStatistic.getLikelihoodRatioSym(s, mSym, advancedPooling));
		lv.add(StateStatistic.getLikelihoodRatioTime(s, mHist, advancedPooling));

		Interval in;
		TimedTail nt;
		Multimap<Integer, TimedTail> mNextHist, mNextSym;
		Collection<TimedTail> m;
		for (int i = 0; i < a.getAlphSize(); i++) {
			assert (s.getIntervals(i).size() == 1);
			in = s.getIntervals(i).firstEntry().getValue();
			// LRT_FIX : || -> && stop recursion
			if (in.getTails().size() >= 2 * PDRTA.getMinData()) {
				assert (in.getTarget() != null);
				mNextHist = HashMultimap.create();
				mNextSym = HashMultimap.create();
				m = mSym.get(i);
				for (final TimedTail tail : m) {
					assert (in.containsTail(tail));
					nt = tail.getNextTail();
					if (nt != null) {
						mNextHist.put(nt.getHistBarIndex(), nt);
						mNextSym.put(nt.getSymbolAlphIndex(), nt);
					}
				}
				lv.add(intTestSplit(in.getTarget(), mNextHist, mNextSym));
			}
		}
		return lv;
	}

	@Override
	public void setColoring(StateColoring sc) {
		stateColoring = sc;
	}

}
