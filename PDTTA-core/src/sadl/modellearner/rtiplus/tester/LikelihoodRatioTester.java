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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import jsat.distributions.ChiSquared;
import sadl.modellearner.rtiplus.OperationUtil;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.StateColoring;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;
import sadl.models.pdrta.StateStatistic.CalcRatio;
import sadl.models.pdrta.TimedTail;

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

		final LikelihoodValue lv = intTestSplit(s, symAlphIdx, time, StateStatistic::calcLRTRatio);
		return compareToChiSquared(lv);
	}

	LikelihoodValue intTestSplit(PDRTAState s, int symAlphIdx, int time, CalcRatio cr) {

		final Optional<PDRTAState> t = s.getTarget(symAlphIdx, time);
		if (!t.isPresent()) {
			throw new IllegalArgumentException("Transition has no target state");
		}

		if (!stateColoring.isRed(s)) {
			throw new IllegalArgumentException("s must be red!");
		} else if (!stateColoring.isBlue(t.get())) {
			throw new IllegalArgumentException("Target must be blue!");
		}

		// Abort because LRT will never be calculated for any state in the tree
		if (t.get().getTotalOutEvents() < (2 * PDRTA.getMinData())) {
			return new LikelihoodValue();
		}

		final Multimap<Integer, TimedTail> mHist = HashMultimap.create();
		final Multimap<Integer, TimedTail> mSym = HashMultimap.create();
		final Optional<Set<Entry<Integer, TimedTail>>> tails = s.getInterval(symAlphIdx, time).map(in -> in.getTails().entries());
		if (!tails.isPresent()) {
			throw new IllegalArgumentException("Transition does not contain sequences");
		}

		for (final Entry<Integer, TimedTail> eT : tails.get()) {
			if (eT.getKey().intValue() <= time && eT.getValue().getNextTail() != null) {
				mHist.put(new Integer(eT.getValue().getNextTail().getHistBarIndex()), eT.getValue().getNextTail());
				mSym.put(new Integer(eT.getValue().getNextTail().getSymbolAlphIndex()), eT.getValue().getNextTail());
			}
		}

		final LikelihoodValue lv = new LikelihoodValue();
		lv.add(recTestSplit(t.get(), mHist, mSym, cr));

		// TODO delete. only for debug
		// System.out.println("p=" + (-2.0 * lv.ratio) + " , df="
		// + lv.additionalParam);

		return lv;
	}

	double compareToChiSquared(LikelihoodValue lv) {

		final int param = lv.getParam();
		if (param > 0) {
			final ChiSquared c = new ChiSquared(param);
			return 1.0 - c.cdf(-2.0 * lv.getRatio());
		} else {
			return -1.0;
		}
	}

	@Override
	public double testMerge(PDRTAState red, PDRTAState blue) {

		final LikelihoodValue lv = intTestMerge(red, blue, StateStatistic::calcLRTRatio);
		return compareToChiSquared(lv);
	}

	LikelihoodValue intTestMerge(PDRTAState red, PDRTAState blue, CalcRatio cr) {

		if (!stateColoring.isRed(red)) {
			throw new IllegalArgumentException("First state must be red!");
		} else if (!stateColoring.isBlue(blue)) {
			throw new IllegalArgumentException("Second state must be blue!");
		}

		final PDRTA a = red.getPDRTA();
		assert (a == blue.getPDRTA());

		// LRT_FIX : Deleted because of new && condition
		// if (blue.getTotalOutEvents() < minData) {
		// return -1.0;
		// }

		final PDRTA cA = new PDRTA(a);
		final PDRTAState cR = cA.getState(red.getIndex());
		final PDRTAState cB = cA.getState(blue.getIndex());

		final StateColoring cColoring = new StateColoring(stateColoring, cA);

		final LikelihoodValue lv = OperationUtil.merge(cR, cB, cColoring, true, advancedPooling, cr);

		// TODO delete. only for debug
		// System.out.println("p=" + (-2.0 * lv.ratio) + " , df="
		// + lv.additionalParam);

		return lv;
	}

	private LikelihoodValue recTestSplit(PDRTAState s, Multimap<Integer, TimedTail> mHist, Multimap<Integer, TimedTail> mSym, CalcRatio cr) {

		final PDRTA a = s.getPDRTA();
		final int minData = PDRTA.getMinData();

		final LikelihoodValue lv = new LikelihoodValue();
		lv.add(StateStatistic.getLikelihoodRatioSym(s, mSym, advancedPooling, cr));
		lv.add(StateStatistic.getLikelihoodRatioTime(s, mHist, advancedPooling, cr));

		Interval in;
		TimedTail nt;
		Multimap<Integer, TimedTail> mNextHist, mNextSym;
		Collection<TimedTail> m;
		for (int i = 0; i < a.getAlphSize(); i++) {
			final Optional<NavigableMap<Integer, Interval>> ins = s.getIntervals(i);
			if (ins.isPresent()) {
				// Only non-red states with initial intervals are processed
				assert (s.getIntervals(i).get().size() == 1);
				// Interval is always present
				in = s.getIntervals(i).get().firstEntry().getValue();
				if (in.getTails().size() > 0) {
					assert (in.getTarget() != null);
					mNextHist = HashMultimap.create();
					mNextSym = HashMultimap.create();
					m = mSym.get(new Integer(i));
					for (final TimedTail tail : m) {
						assert (in.containsTail(tail));
						nt = tail.getNextTail();
						if (nt != null) {
							mNextHist.put(new Integer(nt.getHistBarIndex()), nt);
							mNextSym.put(new Integer(nt.getSymbolAlphIndex()), nt);
						}
					}
					// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND) => stop recursion
					// In case of AND => if((in.getTails().size() < 2 * minData) before calculating new maps!
					if (!SimplePDRTALearner.bOp[2].eval((in.getTails().size() - mNextHist.size()) < minData, mNextHist.size() < minData)) {
						lv.add(recTestSplit(in.getTarget(), mNextHist, mNextSym, cr));
					}
				}
			}
		}
		return lv;
	}

	@Override
	public void setColoring(StateColoring sc) {
		stateColoring = sc;
	}

}
