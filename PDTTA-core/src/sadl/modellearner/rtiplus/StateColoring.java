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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;

public class StateColoring implements Iterable<PDRTAState> {

	private final TIntHashSet redStates;
	private final TIntHashSet blueStates;

	private final PDRTA a;

	StateColoring(PDRTA a) {

		this.redStates = new TIntHashSet();
		this.blueStates = new TIntHashSet();
		this.a = a;
	}

	public StateColoring(StateColoring sc, PDRTA a) {

		this.redStates = new TIntHashSet(sc.redStates);
		this.blueStates = new TIntHashSet(sc.blueStates);
		this.a = a;
	}

	public void setRed(PDRTAState s) {

		assert (a == s.getPDRTA());
		if (s != null && !redStates.contains(s.getIndex())) {
			redStates.add(s.getIndex());
			blueStates.remove(s.getIndex());
			for (int i = 0; i < a.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final PDRTAState t = eIn.getValue().getTarget();
					if (t != null && !redStates.contains(t.getIndex())) {
						assert (a.containsState(t));
						blueStates.add(t.getIndex());
					}
				}
			}
		}
	}

	public void setBlue(PDRTAState s) {

		assert (a == s.getPDRTA());
		if (s != null && !redStates.contains(s.getIndex())) {
			blueStates.add(s.getIndex());
		}
	}

	public boolean isRed(PDRTAState s) {

		if (s != null) {
			return redStates.contains(s.getIndex());
		}
		return false;
	}

	public boolean isBlue(PDRTAState s) {

		if (s != null) {
			return blueStates.contains(s.getIndex());
		}
		return false;
	}

	public int getNumRedStates() {
		return redStates.size();
	}

	public void remove(PDRTAState s) {

		if (!redStates.contains(s.getIndex())) {
			blueStates.remove(s.getIndex());
		} else {
			throw new IllegalStateException("Red states must not be removed");
		}
	}

	public List<PDRTAState> getRedStates() {
		final List<PDRTAState> result = new ArrayList<>();
		redStates.forEach((TIntProcedure) value -> {
			result.add(a.getState(value));
			return true;
		});
		return result;
	}

	@Override
	public Iterator<PDRTAState> iterator() {
		return new Iterator<PDRTAState>() {
			TIntIterator it = redStates.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public PDRTAState next() {
				return a.getState(it.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
