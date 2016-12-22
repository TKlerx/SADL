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
package sadl.models.pdrta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 
 * @author Fabian Witter
 *
 */
public class PDRTAState implements Serializable {

	private static final long serialVersionUID = 1L;

	private final PDRTA automaton;
	private final List<NavigableMap<Integer, Interval>> intervals;
	private final StateStatistic stat;
	private final int index;

	PDRTAState(PDRTA ta) {

		automaton = ta;
		intervals = new ArrayList<>(Collections.nCopies(ta.getAlphSize(), null));
		stat = StateStatistic.initStat(ta.getAlphSize(), ta.getHistSizes());
		index = automaton.addState(this, automaton.getStateCount());
	}

	PDRTAState(PDRTA ta, int idx, StateStatistic st) {

		automaton = ta;
		intervals = new ArrayList<>(Collections.nCopies(ta.getAlphSize(), null));
		stat = st;
		index = automaton.addState(this, idx);
		if (index != idx) {
			throw new IllegalStateException("Index " + idx + " already exists!");
		}
	}

	public PDRTA getPDRTA() {
		return automaton;
	}

	public StateStatistic getStat() {
		return stat;
	}

	protected PDRTAState(PDRTAState s, PDRTA a) {

		automaton = a;
		intervals = new ArrayList<>(Collections.nCopies(automaton.getAlphSize(), null));
		for (int i = 0; i < automaton.getAlphSize(); ++i) {
			final Optional<Set<Entry<Integer, Interval>>> ins = s.getIntervals(i).map(m -> m.entrySet());
			if (ins.isPresent()) {
				final NavigableMap<Integer, Interval> newIns = new TreeMap<>();
				for (final Entry<Integer, Interval> eIn : ins.get()) {
					if (eIn.getValue() != null) {
						newIns.put(eIn.getKey(), new Interval(eIn.getValue()));
					} else {
						newIns.put(eIn.getKey(), null);
					}
				}
				intervals.set(i, newIns);
			}
		}
		stat = new StateStatistic(s.stat);
		index = automaton.addState(this, s.getIndex());
		if (index != s.index) {
			throw new IllegalStateException("Index " + s.index + " already exists!");
		}
	}

	/**
	 * Add incoming Tail
	 * 
	 * @param tail
	 */
	public void addTail(TimedTail tail) {

		if (automaton.hasInput()) {
			if (tail != null) {
				stat.addToStats(tail);
				final TimedTail t = tail.getNextTail();
				if (t != null) {
					Optional<NavigableMap<Integer, Interval>> inMap = getIntervals(t.getSymbolAlphIndex());
					if (!inMap.isPresent()) {
						inMap = Optional.of(Interval.createInitialIntervalMap(automaton.getMinTimeDelay(), automaton.getMaxTimeDelay()));
						intervals.set(t.getSymbolAlphIndex(), inMap.get());
					}
					assert (inMap.get().size() == 1);
					// Always contains existing initial interval
					inMap.get().firstEntry().getValue().addTail(t);
				}
			} else {
				throw new IllegalArgumentException("The given TimedTail must not be null!");
			}
		} else {
			throw new IllegalStateException("This operation is only allowed in training phase!");
		}
	}

	public Collection<PDRTAState> getTargets() {
		return intervals.stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null).map(in -> in.getTarget())
				.collect(Collectors.toSet());
	}

	public Optional<PDRTAState> getTarget(TimedTail tail) {
		return getTarget(tail.getSymbolAlphIndex(), tail.getTimeDelay());
	}

	public Optional<PDRTAState> getTarget(int symAlphIdx, int timeDel) {
		return getInterval(symAlphIdx, timeDel).map(in -> in.getTarget());
	}

	public double getProbabilityTrans(TimedTail tail) {
		return getProbabilityTrans(tail.getSymbolAlphIndex(), tail.getTimeDelay());
	}

	public double getProbabilityTrans(int symAplhIdx, int timeDel) {

		final Optional<Interval> in = getInterval(symAplhIdx, timeDel);
		return getProbabilityTrans(symAplhIdx, in);

	}

	public double getProbabilityTrans(int symAplhIdx, Interval in) {
		return getProbabilityTrans(symAplhIdx, Optional.ofNullable(in));
	}

	public double getProbabilityTrans(int symAplhIdx, Optional<Interval> in) {

		if (in == null) {
			return 0.0;
		} else {
			return stat.getTransProb(symAplhIdx, in);
		}
	}

	public int getTotalOutEvents() {
		return stat.getTotalOutEvents();
	}

	public double getSequenceEndProb() {
		return stat.getTailEndProb();
	}

	public List<NavigableMap<Integer, Interval>> getIntervals() {
		return intervals;
	}

	public Optional<NavigableMap<Integer, Interval>> getIntervals(int alphIdx) {
		return Optional.ofNullable(intervals.get(alphIdx));
	}

	public Optional<Interval> getInterval(int alphIdx, int time) {

		if (alphIdx < 0 || alphIdx >= automaton.getAlphSize()) {
			return Optional.empty();
		}
		final Optional<Interval> in = getIntervals(alphIdx).map(m -> m.ceilingEntry(new Integer(time))).map(e -> e.getValue());
		if (in.isPresent()) {
			assert (in.get().contains(time));
		}
		return in;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((intervals == null) ? 0 : intervals.hashCode());
		result = prime * result + ((stat == null) ? 0 : stat.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PDRTAState other = (PDRTAState) obj;
		if (index != other.index) {
			return false;
		}
		if (intervals == null) {
			if (other.intervals != null) {
				return false;
			}
		} else if (!intervals.equals(other.intervals)) {
			return false;
		}
		if (stat == null) {
			if (other.stat != null) {
				return false;
			}
		} else if (!stat.equals(other.stat)) {
			return false;
		}
		return true;
	}

	public int getIndex() {
		return index;
	}

	void cleanUp() {

		stat.cleanUp(this);
		for (int i = 0; i < automaton.getAlphSize(); i++) {
			// Clean up all present intervals
			getIntervals(i).map(m -> m.values()).ifPresent(col -> col.stream().filter(in -> in != null).forEach(in -> in.cleanUp()));
		}
	}

}
