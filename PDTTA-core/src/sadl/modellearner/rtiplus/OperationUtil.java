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

package sadl.modellearner.rtiplus;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import sadl.modellearner.rtiplus.tester.LikelihoodValue;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;
import sadl.models.pdrta.TimedTail;

/**
 * 
 * @author Fabian Witter
 *
 */
public class OperationUtil {

	private OperationUtil() {
		// Do not use
	}

	private static void preMerge(PDRTAState s1, PDRTAState s2, StateColoring sc) {

		if (sc.isRed(s1)) {
			final PDRTA a = s1.getPDRTA();
			// Let transitions pointing to second state point to first state
			for (final PDRTAState s : sc) {
				for (int i = 0; i < a.getAlphSize(); i++) {
					final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
					for (final Entry<Integer, Interval> eIn : ins) {
						final PDRTAState t = eIn.getValue().getTarget();
						if (t != null && t == s2) {
							eIn.getValue().setTarget(s1);
						}
					}
				}
			}
			// Merge neighbored intervals with same source and target (undo previous splits)
			for (final PDRTAState s : sc) {
				for (int i = 0; i < a.getAlphSize(); i++) {
					final Iterator<Entry<Integer, Interval>> it = s.getIntervals(i).descendingMap().entrySet().iterator();
					if (it.hasNext()) {
						Interval neighbor = it.next().getValue();
						while (it.hasNext()) {
							final Interval in = it.next().getValue();
							if (neighbor.getTarget() != null && in.getTarget() != null && neighbor.getTarget() == s1 && in.getTarget() == s1) {
								assert (neighbor.getBegin() - 1 == in.getEnd());
								neighbor.merge(in);
								it.remove();
							} else {
								neighbor = in;
							}
						}
					}
				}
			}
			// Split overlapping intervals of second state
			for (int i = 0; i < a.getAlphSize(); i++) {
				assert (s2.getIntervals(i).size() == 1);
				final Set<Entry<Integer, Interval>> iRs = s1.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : iRs) {
					assert (eIn.getValue().getBegin() == s2.getInterval(i, eIn.getKey()).getBegin());
					if (eIn.getValue().getEnd() < s2.getInterval(i, eIn.getKey()).getEnd()) {
						split(s2, i, eIn.getKey(), sc);
					}
				}
				assert (s2.getIntervals(i).size() == s1.getIntervals(i).size());
			}
		}
		// Merge statistics
		s1.getStat().merge(s2.getStat());
	}

	/**
	 * Belongs to testMerge_C
	 * 
	 * @param s1
	 * @param s2
	 * @return LikelihoodValue
	 */
	@SuppressWarnings("null")
	public static LikelihoodValue merge(PDRTAState s1, PDRTAState s2, StateColoring sc, boolean test, boolean advancedPooling) {

		final PDRTA a = s1.getPDRTA();
		assert (a == s2.getPDRTA());
		assert (!sc.isRed(s2));
		a.removeState(s2);

		LikelihoodValue lv = null;
		if (test) {
			lv = new LikelihoodValue(0.0, 0);
			lv.add(StateStatistic.getLikelihoodRatioSym(s1, s2, advancedPooling));
			lv.add(StateStatistic.getLikelihoodRatioTime(s1, s2, advancedPooling));
		}

		preMerge(s1, s2, sc);

		// Merge intervals
		Iterator<Entry<Integer, Interval>> it1, it2;
		Interval in1, in2;
		int out1, out2;
		for (int i = 0; i < a.getAlphSize(); i++) {
			it1 = s1.getIntervals(i).entrySet().iterator();
			it2 = s2.getIntervals(i).entrySet().iterator();
			while (it1.hasNext()) {
				in1 = it1.next().getValue();
				in2 = it2.next().getValue();
				if (in2.getTarget() != null) {
					assert (!in2.isEmpty());
					if (in1.getTarget() != null) {
						assert (!in1.isEmpty());
						if (test) {
							out1 = in1.getTarget().getStat().getTotalOutEvents();
							out2 = in1.getTarget().getStat().getTotalOutEvents();
							// Abort recursion when s1 is in a subtree and not
							// enough data is available for testing
							if (sc.isRed(in1.getTarget()) || !(out1 < PDRTA.getMinData() && out2 < PDRTA.getMinData())) {
								lv.add(merge(in1.getTarget(), in2.getTarget(), sc, test, advancedPooling));
							}
						} else {
							merge(in1.getTarget(), in2.getTarget(), sc, test, advancedPooling);
						}
						in1.getTails().putAll(in2.getTails());
					} else {
						// LRT_FIX : calculation of LRT necessary?
						// Move subtree of s2 to s1
						assert (s1.getIntervals(i).containsKey(in2.getEnd()));
						s1.getIntervals(i).put(in2.getEnd(), in2);
						if (sc.isRed(s1)) {
							sc.setBlue(in2.getTarget());
						}
					}
				}
			}
			assert (!it2.hasNext());
		}

		return lv;
	}

	static void split(PDRTAState s, int symAlphIdx, int time, StateColoring sc) {

		final PDRTA a = s.getPDRTA();

		final Interval in = s.getInterval(symAlphIdx, time);

		final Interval newIn = in.split(time);
		s.getIntervals(symAlphIdx).put(newIn.getEnd(), newIn);

		assert (newIn == in);
		assert (newIn.getTarget() == null);
		assert (newIn.getEnd() == in.getBegin() - 1);
		assert (s.getInterval(symAlphIdx, time) == newIn);

		if (!newIn.isEmpty()) {
			if (!in.isEmpty()) {
				// Create new sub APTA for first interval
				PDRTAState state = a.createState();
				newIn.setTarget(state);
				if (sc.isRed(s)) {
					sc.setBlue(state);
				}
				Set<Entry<Integer, TimedTail>> tails = newIn.getTails().entries();
				for (final Entry<Integer, TimedTail> e : tails) {
					state.addTail(e.getValue());
				}
				a.createSubTAPTA(state);
				// Create new sub APTA for second interval
				a.removeSubAPTA(in.getTarget());
				state = a.createState();
				in.setTarget(state);
				if (sc.isRed(s)) {
					sc.setBlue(state);
				}
				tails = in.getTails().entries();
				for (final Entry<Integer, TimedTail> e : tails) {
					state.addTail(e.getValue());
				}
				a.createSubTAPTA(state);
			} else {
				newIn.setTarget(in.getTarget());
				in.setTarget(null);
			}
		}
	}

}
