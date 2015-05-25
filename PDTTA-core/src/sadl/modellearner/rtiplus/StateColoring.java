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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;

public class StateColoring implements Iterable<PDRTAState> {

	// TODO Collect only the states indices
	private final Set<PDRTAState> redStates;
	private final Set<PDRTAState> blueStates;

	private final PDRTA a;

	StateColoring(PDRTA a) {

		this.redStates = new HashSet<>();
		this.blueStates = new HashSet<>();
		this.a = a;
	}

	public StateColoring(StateColoring sc, PDRTA a) {

		this.redStates = new HashSet<>(sc.redStates);
		this.blueStates = new HashSet<>(sc.blueStates);
		this.a = a;
	}

	public void setRed(PDRTAState s) {

		assert (a == s.getPDRTA());
		if (s != null && !redStates.contains(s)) {
			redStates.add(s);
			blueStates.remove(s);
			for (int i = 0; i < a.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final PDRTAState t = eIn.getValue().getTarget();
					if (t != null && !redStates.contains(t)) {
						assert (a.containsState(t));
						blueStates.add(t);
					}
				}
			}
		}
		System.out.print("BLUE STATES: ");
		for (final PDRTAState x : blueStates) {
			System.out.print(x.getIndex() + "  ");
		}
		System.out.println();
	}

	public void setBlue(PDRTAState s) {

		assert (a == s.getPDRTA());
		if (s != null && !redStates.contains(s)) {
			blueStates.add(s);
		}
	}

	public boolean isRed(PDRTAState s) {
		return redStates.contains(s);
	}

	public boolean isBlue(PDRTAState s) {
		System.out.print("BLUE STATES: ");
		for (final PDRTAState x : blueStates) {
			System.out.print(x.getIndex() + "  ");
		}
		System.out.println();
		return blueStates.contains(s);
	}

	public int getNumRedStates() {
		return redStates.size();
	}

	@Override
	public Iterator<PDRTAState> iterator() {
		return new Iterator<PDRTAState>() {
			Iterator<PDRTAState> it = redStates.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public PDRTAState next() {
				return it.next();
			}
		};
	}

}
