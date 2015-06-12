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

package sadl.models.pdrta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * 
 * @author Fabian Witter
 *
 */
public class PDRTAState implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PDRTA automaton;
	private final List<NavigableMap<Integer, Interval>> intervals;
	private final StateStatistic stat;
	private final int index;

	protected PDRTAState(PDRTA ta) {

		automaton = ta;
		intervals = new ArrayList<>();
		for (int i = 0; i < ta.getAlphSize(); ++i) {
			intervals.add(Interval.createInitialIntervalMap(ta.getMinTimeDelay(), ta.getMaxTimeDelay()));
		}
		stat = StateStatistic.initStat(ta.getAlphSize(), ta.getHistSizes());
		index = automaton.addState(this, automaton.getNumStates());
	}

	protected PDRTAState(PDRTA ta, int idx, StateStatistic st) {

		automaton = ta;
		intervals = new ArrayList<>();
		for (int i = 0; i < ta.getAlphSize(); ++i) {
			intervals.add(Interval.createInitialIntervalMap(ta.getMinTimeDelay(), ta.getMaxTimeDelay()));
		}
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
		intervals = new ArrayList<>();
		for (int i = 0; i < automaton.getAlphSize(); ++i) {
			final NavigableMap<Integer, Interval> newIns = new TreeMap<>();
			final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
			for (final Entry<Integer, Interval> eIn : ins) {
				newIns.put(eIn.getKey(), new Interval(eIn.getValue()));
			}
			intervals.add(newIns);
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

		if (automaton.hasInput() && tail != null) {
			stat.addToStats(tail);
			tail = tail.getNextTail();
			if (tail != null) {
				final Interval in = getInterval(tail.getSymbolAlphIndex(), tail.getTimeDelay());
				assert (in != null);
				in.addTail(tail);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public PDRTAState getTarget(TimedTail tail) {

		return getTarget(tail.getSymbolAlphIndex(), tail.getTimeDelay());
	}

	public PDRTAState getTarget(int symAlphIdx, int timeDel) {

		final Interval in = getInterval(symAlphIdx, timeDel);
		if (in == null) {
			return null;
		} else {
			return in.getTarget();
		}
	}

	public double getProbabilityTrans(TimedTail tail) {

		return getProbabilityTrans(tail.getSymbolAlphIndex(), tail.getTimeDelay());
	}

	public double getProbabilityTrans(int symAplhIdx, int timeDel) {

		final Interval in = getInterval(symAplhIdx, timeDel);
		if (in == null) {
			return 0.0;
		} else {
			return stat.getTransProb(symAplhIdx, in);
		}
	}

	public double getProbabilityHist(TimedTail tail) {

		return stat.getHistProb(tail);
	}

	public int getTotalOutEvents() {

		return stat.getTotalOutEvents();
	}

	int getSymCount(int symAlphIdx) {

		int count = 0;
		final Set<Entry<Integer, Interval>> ins = getIntervals(symAlphIdx).entrySet();
		for (final Entry<Integer, Interval> eIn : ins) {
			count += eIn.getValue().getTails().size();
		}
		return count;
	}

	public NavigableMap<Integer, Interval> getIntervals(int alphIdx) {
		return intervals.get(alphIdx);
	}

	public Interval getInterval(int alphIdx, int time) {

		if (alphIdx < 0) {
			return null;
		}
		final NavigableMap<Integer, Interval> intMap = intervals.get(alphIdx);
		final Integer key = intMap.ceilingKey(time);
		if (key == null) {
			return null;
		} else {
			final Interval in = intMap.get(key);
			if (in.contains(time)) {
				return in;
			} else {
				return null;
			}
		}
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
			final NavigableMap<Integer, Interval> m = intervals.get(i);
			for (final Interval in : m.values()) {
				in.cleanUp();
			}
		}
	}

}
