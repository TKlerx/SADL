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
package sadl.modellearner.rtiplus;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.modellearner.rtiplus.tester.LikelihoodValue;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.StateStatistic;
import sadl.models.pdrta.StateStatistic.CalcRatio;

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
				// Get all present intervals of s
				final List<Interval> ins = s.getIntervals().stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null)
						.collect(Collectors.toList());
				for (final Interval in : ins) {
					final PDRTAState t = in.getTarget();
					if (t != null && t.equals(s2)) {
						in.setTarget(s1);
					}
				}
			}
			// Merge neighbored intervals with same source and target (undo previous splits)
			for (final PDRTAState s : sc) {
				for (int i = 0; i < a.getAlphSize(); i++) {
					final Optional<Iterator<Entry<Integer, Interval>>> it = s.getIntervals(i).map(m -> m.descendingMap().entrySet().iterator());
					if (it.isPresent()) {
						if (it.get().hasNext()) {
							Interval neighbor = it.get().next().getValue();
							while (it.get().hasNext()) {
								final Interval in = it.get().next().getValue();
								if (neighbor != null && in != null && neighbor.getTarget().equals(s1) && in.getTarget().equals(s1)) {
									assert (neighbor.getBegin() - 1 == in.getEnd());
									neighbor.merge(in);
									it.get().remove();
								} else {
									neighbor = in;
								}
							}
						}
					}
				}
			}
			// Split overlapping intervals of second state
			for (int i = 0; i < a.getAlphSize(); i++) {
				if (s1.getIntervals(i).isPresent() && s2.getIntervals(i).isPresent()) {
					assert (s2.getIntervals(i).get().size() == 1);
					final Set<Entry<Integer, Interval>> inReds = s1.getIntervals(i).get().entrySet();
					for (final Entry<Integer, Interval> eIn : inReds) {
						if (eIn.getValue() != null && s2.getInterval(i, eIn.getKey().intValue()).isPresent()) {
							assert (eIn.getValue().getBegin() == s2.getInterval(i, eIn.getKey().intValue()).get().getBegin());
						}
						if (eIn.getKey().intValue() < a.getMaxTimeDelay()) {
							if (s2.getInterval(i, eIn.getKey().intValue()).isPresent()) {
								split(s2, i, eIn.getKey().intValue(), sc);
							} else {
								// Remaining interval is empty
								s2.getIntervals(i).get().put(eIn.getKey(), null);
							}
						}
					}
					assert (s2.getIntervals(i).get().size() == s1.getIntervals(i).get().size());
				}
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
	public static LikelihoodValue merge(PDRTAState s1, PDRTAState s2, StateColoring sc, boolean test, boolean advancedPooling, CalcRatio cr) {

		final PDRTA a = s1.getPDRTA();
		assert (a == s2.getPDRTA());
		assert (!sc.isRed(s2));

		LikelihoodValue lv = null;
		if (test) {
			lv = new LikelihoodValue();
			lv.add(StateStatistic.getLikelihoodRatioSym(s1, s2, advancedPooling, cr));
			lv.add(StateStatistic.getLikelihoodRatioTime(s1, s2, advancedPooling, cr));
		}

		preMerge(s1, s2, sc);

		// Merge intervals
		for (int i = 0; i < a.getAlphSize(); i++) {
			final Optional<NavigableMap<Integer, Interval>> ins1 = s1.getIntervals(i);
			final Optional<NavigableMap<Integer, Interval>> ins2 = s2.getIntervals(i);
			if (!ins1.isPresent()) {
				// Move interval map from s2 to s1
				s1.getIntervals().set(i, ins2.orElse(null));
				if (sc.isRed(s1) && ins2.isPresent()) {
					// Color all targets blue
					ins2.get().values().stream().filter(in -> in != null).forEach(in -> sc.setBlue(in.getTarget()));
				}
			} else if (ins2.isPresent()) {
				// Merge non-deterministic intervals
				Iterator<Entry<Integer, Interval>> it1, it2;
				Interval in1, in2;
				assert (ins1.map(m -> new Integer(m.size())).orElse(new Integer(-1)).intValue() == ins2.map(m -> new Integer(m.size())).orElse(new Integer(-1))
						.intValue());
				it1 = s1.getIntervals(i).get().entrySet().iterator();
				it2 = s2.getIntervals(i).get().entrySet().iterator();
				while (it1.hasNext()) {
					in1 = it1.next().getValue();
					in2 = it2.next().getValue();
					if (in2 != null) {
						assert (!in2.isEmpty());
						assert (in2.getTarget() != null);
						if (in1 != null) {
							assert (!in1.isEmpty());
							assert (in1.getTarget() != null);
							assert (in1.getBegin() == in2.getBegin());
							assert (in1.getEnd() == in2.getEnd());
							in1.getTails().putAll(in2.getTails());
							if (test) {
								int out1, out2;
								out1 = in1.getTarget().getStat().getTotalOutEvents();
								out2 = in2.getTarget().getStat().getTotalOutEvents();
								// LRT_FIX : Operator for calculation interruption (thesis: AND, impl: OR, own: AND) => stop recursion
								// Abort recursion when s1 is in a subtree (not red) and not enough data is available for testing
								// Attention: Verwer's implementation stops even if s1 is red and there can be further computations!
								if (sc.isRed(in1.getTarget()) || !SimplePDRTALearner.bOp[2].eval(out1 < PDRTA.getMinData(), out2 < PDRTA.getMinData())) {
									lv.add(merge(in1.getTarget(), in2.getTarget(), sc, test, advancedPooling, cr));
								}
							} else {
								merge(in1.getTarget(), in2.getTarget(), sc, test, advancedPooling, cr);
							}
						} else {
							// Move interval from s2 to s1
							assert (ins1.get().containsKey(new Integer(in2.getEnd())));
							ins1.get().put(new Integer(in2.getEnd()), in2);
							if (sc.isRed(s1)) {
								sc.setBlue(in2.getTarget());
							}
						}
					}
				}
				assert (!it2.hasNext());
			}
		}

		a.recycleState(s2, sc);

		return lv;
	}

	public static Pair<Optional<Interval>, Optional<Interval>> split(PDRTAState s, int symAlphIdx, int time, StateColoring sc) {

		final PDRTA a = s.getPDRTA();

		final Optional<NavigableMap<Integer, Interval>> ins = s.getIntervals(symAlphIdx);
		if (!ins.isPresent()) {
			throw new IllegalArgumentException("No intervals exist for the given symbol");
		}

		final Optional<Interval> optIn = s.getInterval(symAlphIdx, time);
		if (!optIn.isPresent()) {
			throw new IllegalArgumentException("The interval to be split does not exist");
		}

		Interval in = optIn.get();
		Interval newIn = in.split(time);

		assert (newIn != in);
		assert (newIn.getTarget() == null);
		assert (newIn.getEnd() == in.getBegin() - 1);

		if (newIn.isEmpty()) {
			ins.get().put(new Integer(newIn.getEnd()), null);
			newIn = null;
		} else {
			ins.get().put(new Integer(newIn.getEnd()), newIn);
			if (in.isEmpty()) {
				// replace in by newIn
				newIn.setTarget(in.getTarget());
				ins.get().put(new Integer(in.getEnd()), null);
				in = null;
			} else if (sc != null) {
				// Recreate sub APTAs for both intervals
				// Create new sub APTA for first interval
				final PDRTAState newState = a.acquireState();
				newIn.setTarget(newState);
				if (sc.isRed(s)) {
					sc.setBlue(newState);
				}
				newIn.getTails().entries().forEach(e -> newState.addTail(e.getValue()));
				a.createSubTAPTA(newState);
				// Create new sub APTA for second interval
				a.recycleSubAPTA(in.getTarget(), sc);
				final PDRTAState state = a.acquireState();
				in.setTarget(state);
				if (sc.isRed(s)) {
					sc.setBlue(state);
				}
				in.getTails().entries().forEach(e -> state.addTail(e.getValue()));
				a.createSubTAPTA(state);
			}
		}

		return Pair.of(Optional.ofNullable(newIn), Optional.ofNullable(in));
	}

	@SuppressWarnings("boxing")
	public static Pair<TIntList, TIntList> distributionsMapToLists(SortedMap<Integer, Integer> distributionsMap) {

		final TIntList values = new TIntArrayList(distributionsMap.size());
		final TIntList frequencies = new TIntArrayList(distributionsMap.size());
		distributionsMap.entrySet().forEach(e -> {
			values.add(e.getKey());
			frequencies.add(e.getValue());
		});
		return Pair.of(values, frequencies);
	}

}
