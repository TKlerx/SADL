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
package sadl.modellearner.rtiplus;

import com.google.common.collect.TreeMultimap;

import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.TimedTail;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
class Refinement implements Comparable<Refinement> {

	private enum OpType {
		SPLIT, MERGE
	}

	private final PDRTAState source;
	private final PDRTAState target;
	private final int symbolAlphIdx;
	private final int time;
	private OpType type;
	private final double score;

	private final StateColoring stateColoring;

	/**
	 * For Merges
	 * 
	 * @param s
	 * @param t
	 * @param score
	 * @param sc
	 */
	Refinement(PDRTAState s, PDRTAState t, double score, StateColoring sc) {

		this(s, t, -1, -1, score, sc);
		assert (sc.isRed(s));
		assert (sc.isBlue(t));
		type = OpType.MERGE;
	}

	/**
	 * For Splits
	 * 
	 * @param s
	 * @param alphIdx
	 * @param t
	 * @param score
	 * @param sc
	 */
	Refinement(PDRTAState s, int alphIdx, int t, double score, StateColoring sc) {

		this(s, null, alphIdx, t, score, sc);
		assert (sc.isRed(s));
		assert (s.getInterval(alphIdx, t) != null);
		type = OpType.SPLIT;
	}

	private Refinement(PDRTAState s, PDRTAState t, int alphIdx, int ti, double sco, StateColoring sc) {

		source = s;
		target = t;
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
		score = r.score;
		type = r.type;
		symbolAlphIdx = r.symbolAlphIdx;
		time = r.time;
		stateColoring = newSC;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		if (type == OpType.MERGE) {
			sb.append("merge (").append(source.getIndex()).append(")>-<(").append(target.getIndex()).append(") to (").append(source.getIndex()).append(")");
		} else if (type == OpType.SPLIT) {
			sb.append("split ((").append(source.getIndex()).append("))---").append(source.getPDRTA().getSymbol(symbolAlphIdx)).append("-[")
			.append(source.getInterval(symbolAlphIdx, time).get().getBegin()).append(",").append(source.getInterval(symbolAlphIdx, time).get().getEnd())
			.append("]---> @ ").append(time);
			if (Settings.isDebug()) {
				sb.append("  Distr.: [");
				final Interval in = source.getInterval(symbolAlphIdx, time).get();
				final TreeMultimap<Integer, TimedTail> t = in.getTails();
				for (int i = in.getBegin(); i <= in.getEnd(); i++) {
					if (i == time + 1) {
						sb.append(" ][");
					}
					sb.append(" ").append(i).append("/");
					if (t.containsKey(new Integer(i))) {
						sb.append(t.get(new Integer(i)).size());
					} else {
						sb.append("0");
					}
				}
				sb.append(" ]");
			}
		}
		sb.append(" Score: ").append(score);
		return sb.toString();
	}

	void refine() {

		if (type == OpType.MERGE) {
			OperationUtil.merge(source, target, stateColoring, false, false, null);
		} else if (type == OpType.SPLIT) {
			OperationUtil.split(source, symbolAlphIdx, time, stateColoring);
		}
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
		final Refinement other = (Refinement) obj;
		if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		if (symbolAlphIdx != other.symbolAlphIdx) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		if (time != other.time) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + symbolAlphIdx;
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + time;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public int compareTo(Refinement r) {

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
