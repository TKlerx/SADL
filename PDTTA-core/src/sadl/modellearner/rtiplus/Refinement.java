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

import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;

/**
 * 
 * @author Fabian Witter
 *
 */
class Refinement implements Comparable<Refinement> {

	private enum OpType {
		SPLIT, MERGE
	}

	private final PDRTA ta;
	private final PDRTAState source;
	private PDRTAState target;
	private final int symbolAlphIdx;
	private final int time;
	private OpType type;
	private final double score;

	private final StateColoring stateColoring;

	Refinement(PDRTAState s, PDRTAState t, double score, StateColoring sc) {

		this(s, t, -1, -1, score, sc);
		assert (sc.isRed(s));
		assert (sc.isBlue(t));
		type = OpType.MERGE;
	}

	Refinement(PDRTAState s, int alphIdx, int t, double score, StateColoring sc) {

		this(s, null, alphIdx, t, score, sc);
		assert (sc.isRed(s));
		assert (s.getInterval(alphIdx, t) != null);
		type = OpType.SPLIT;
	}

	private Refinement(PDRTAState s, PDRTAState t, int alphIdx, int ti, double sco, StateColoring sc) {

		source = s;
		target = t;
		ta = s.getPDRTA();
		score = sco;
		symbolAlphIdx = alphIdx;
		time = ti;
		stateColoring = sc;
	}

	Refinement(PDRTA a, Refinement r, StateColoring newSC) {

		source = a.getState(r.source.getIndex());
		if (r.target != null) {
			target = a.getState(r.target.getIndex());
		} else {
			target = null;
		}
		ta = a;
		score = r.score;
		type = r.type;
		symbolAlphIdx = r.symbolAlphIdx;
		time = r.time;
		stateColoring = newSC;
	}

	@Override
	public String toString() {

		String s = null;
		if (type == OpType.MERGE) {
			s = "merge (" + source.getIndex() + ")>-<(" + target.getIndex() + ") to (" + source.getIndex() + ")";
		} else if (type == OpType.SPLIT) {
			s = "split ((" + source.getIndex() + "))---" + ta.getSymbol(symbolAlphIdx) + "-[" + source.getInterval(symbolAlphIdx, time).getBegin() + ","
					+ source.getInterval(symbolAlphIdx, time).getEnd() + "]---> @ " + time;
			// if (LOG_LVL.compareTo(LogLvl.DEBUG_DEEP) >= 0) {
			// s = s + "  Distr.: [";
			// Interval in = source.getInterval(symbolAlphIdx, time);
			// TreeMultimap<Integer, TimedTail> t = in.getTails();
			// for (int i = in.getBegin(); i <= in.getEnd(); i++) {
			// if (i == time + 1) {
			// s = s + " ][";
			// }
			// s = s + " " + i + "/";
			// if (t.containsKey(i)) {
			// s = s + t.get(i).size();
			// } else {
			// s = s + "0";
			// }
			// }
			// s = s + " ]";
			// }
		}
		s += " Score: " + score;
		return s;
	}

	void refine() {

		if (type == OpType.MERGE) {
			OperationUtil.merge(source, target, stateColoring, false, false, null);
		} else if (type == OpType.SPLIT) {
			OperationUtil.split(source, symbolAlphIdx, time, stateColoring);
		}
	}

	@Override
	public boolean equals(Object o) {

		if (!(o instanceof Refinement)) {
			return false;
		} else {
			final Refinement r = (Refinement) o;
			assert (ta.equals(r.ta));
			if (score == r.score) {
				if (type == OpType.SPLIT && r.type == OpType.SPLIT) {
					assert (source.equals(r.source));
					if (time == r.time && symbolAlphIdx == r.symbolAlphIdx) {
						return true;
					} else {
						return false;
					}
				} else if (type == OpType.MERGE && r.type == OpType.MERGE) {
					assert (target.equals(r.target));
					if (source.getIndex() == r.source.getIndex()) {
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	@Override
	public int compareTo(Refinement r) {

		assert (ta.equals(r.ta));
		if (score > r.score) {
			return 1;
		} else if (score < r.score) {
			return -1;
		} else {
			if (type == OpType.SPLIT && r.type == OpType.MERGE) {
				return 1;
			} else if (type == OpType.MERGE && r.type == OpType.SPLIT) {
				return -1;
			} else {
				if (type == OpType.SPLIT) {
					assert (source.equals(r.source));
					if (time < r.time) {
						return 1;
					} else if (time > r.time) {
						return -1;
					} else {
						if (symbolAlphIdx > r.symbolAlphIdx) {
							return 1;
						} else if (symbolAlphIdx < r.symbolAlphIdx) {
							return -1;
						} else {
							return 0;
						}
					}
				} else {
					assert (target.equals(r.target));
					if (source.getIndex() < r.source.getIndex()) {
						return 1;
					} else if (source.getIndex() > r.source.getIndex()) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		}
	}

}
