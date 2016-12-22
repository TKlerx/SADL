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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.TreeMultimap;

/**
 * This class defines a delay guard for a transition inside a {@link PDRTA}. Moreover it contains the target {@link PDRTAState} the transition is pointing to
 * and, while training, the {@link TimedTail}s using the transition. Therefore an {@link Interval} itself can also be described as a transition without a
 * symbol.
 * 
 * @author Fabian Witter
 * 
 */
public class Interval implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines the inclusive begin of the interval
	 */
	private int begin;

	/**
	 * Defines the inclusive end of the interval
	 */
	private int end;

	/**
	 * Contains the {@link TimedTail}s using the transition while training
	 */
	private final TreeMultimap<Integer, TimedTail> tails;

	/**
	 * The {@link PDRTAState} the transition is pointing to
	 */
	private PDRTAState target;

	/**
	 * Returns the inclusive begin of the interval
	 * 
	 * @return The inclusive begin of the interval
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * Returns the inclusive end of the interval
	 * 
	 * @return The inclusive end of the interval
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * States whether this transition contains {@link TimedTail}s
	 * 
	 * @return Returns {@code true} if the transition contains no {@link TimedTail}s. Returns {@code false} if there are {@link TimedTail}s that use this
	 *         transition.
	 */
	public boolean isEmpty() {
		return tails.isEmpty();
	}

	/**
	 * Returns the {@link TreeMultimap} containing the {@link TimedTail}s using this transition
	 * 
	 * @return A {@link TreeMultimap} containing {@link TimedTail}s
	 */
	public TreeMultimap<Integer, TimedTail> getTails() {
		return tails;
	}

	/**
	 * Returns the target {@link PDRTAState} of this transition
	 * 
	 * @return The target {@link PDRTAState} of this transition. Returns {@code null} if there is no target.
	 */
	public PDRTAState getTarget() {
		return target;
	}

	/**
	 * Returns a visual representation of this interval with the slots occupied by {@link TimedTail}s marked. If the interval range is large it will be scaled
	 * down.
	 * 
	 * @return A visual representation of this interval
	 */
	@Override
	public String toString() {

		final int maxLen = 30;
		final Set<Integer> compl = new HashSet<>();
		final Set<Integer> part = new HashSet<>();
		int endIn;
		if ((end - begin + 1) > maxLen) {
			final double step = (double) (end - begin + 1) / (double) maxLen;
			double pos = -1.0;
			for (int i = 0; i < maxLen; i++) {
				final int s = (int) Math.rint(pos) + 1;
				final int e = (int) Math.rint(pos + step);
				boolean full = true;
				boolean empty = true;
				for (int j = s; j <= e; j++) {
					if (tails.containsKey(new Integer(begin + j))) {
						empty = false;
					} else {
						full = false;
					}
				}
				if (!empty && full) {
					compl.add(new Integer(begin + i));
				} else if (!empty && !full) {
					part.add(new Integer(begin + i));
				}
				pos += step;
			}
			assert (Math.rint(pos + 1.0) == (end - begin + 1));
			endIn = maxLen - 1;
		} else {
			compl.addAll(tails.keySet());
			endIn = end - begin;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(begin + "[");
		for (int i = 0; i <= endIn; i++) {
			final Integer integer = new Integer(begin + i);
			if (compl.contains(integer)) {
				sb.append("+");
			} else if (part.contains(integer)) {
				sb.append("-");
			} else {
				sb.append(" ");
			}
		}
		sb.append("]" + end);
		return sb.toString();
	}

	/**
	 * Creates a {@link TreeMap} containing only one {@link Interval} with given begin and end. This is used for building a (sub) timed augmented prefix tree
	 * acceptor.
	 * 
	 * @param minTimeDelay
	 *            The inclusive begin of the interval
	 * @param maxTimeDelay
	 *            The inclusive end of the interval
	 * @return Map containing only one {@link Interval}
	 */
	protected static NavigableMap<Integer, Interval> createInitialIntervalMap(int minTimeDelay, int maxTimeDelay) {

		final NavigableMap<Integer, Interval> map = new TreeMap<>();
		final Interval in = new Interval(minTimeDelay, maxTimeDelay);
		map.put(new Integer(in.getEnd()), in);
		return map;
	}

	/**
	 * Creates a deep copy of the given interval. It does not copy the {@link TimedTail}s from the {@link PDRTAInput}.
	 * 
	 * @param in
	 *            Interval to be copied
	 */
	protected Interval(Interval in) {

		begin = in.begin;
		end = in.end;
		tails = TreeMultimap.create(in.tails);
		target = in.target;
	}

	/**
	 * Splits this {@link Interval} into two neighbored intervals. This is done by cutting the lower part of the interval defined by the parameter and creating
	 * a new {@link Interval} for this part. After this operation the begin of the interval will be {@code time+1}.
	 * 
	 * @param time
	 *            The time value where the interval will be split after. This value has to be part of the interval and be less than the end.
	 * @return The newly created interval from the lower part of the old interval. This interval has the old begin value and as end the specified time value. It
	 *         also has no target {@link PDRTAState}.
	 */
	public Interval split(int time) {

		if (!contains(time) || end == time) {
			throw new IllegalArgumentException("Time value not suitable -> split [" + begin + "," + end + "] @ " + time);
		}
		final Interval newIn = new Interval(begin, time);
		// TODO Check whether a TreeMultiMap for tails is necessary
		final Iterator<Entry<Integer, TimedTail>> itTails = tails.entries().iterator();
		while (itTails.hasNext()) {
			final Entry<Integer, TimedTail> eT = itTails.next();
			if (eT.getKey().intValue() <= time) {
				newIn.tails.put(eT.getKey(), eT.getValue());
				itTails.remove();
				assert (!tails.containsEntry(eT.getKey(), eT.getValue()));
			}
		}
		begin = time + 1;
		return newIn;
	}

	/**
	 * Merges this {@link Interval} with a given neighbored interval. This operation is the counterpart of the {@link Interval#split(int)} operation. After this
	 * operation the delay guards of this interval will be extended and {@link TimedTail}s will be added from the given interval. The target {@link PDRTAState}
	 * will not be affected.
	 * 
	 * @param in
	 *            A neighbored interval that will be merged. Neighbored means that the given interval ends directly before this interval begins or the given
	 *            interval begins directly after this interval ends. The given object will not be changed.
	 */
	public void merge(Interval in) {

		if ((begin - 1 == in.end || end + 1 == in.begin) && target == in.target) {
			if (begin - 1 == in.end) {
				begin = in.begin;
			} else {
				end = in.end;
			}
			tails.putAll(in.tails);
		} else {
			throw new IllegalArgumentException("Intervals not neighbored -> merge [" + begin + "," + end + "] >-< [" + in.begin + "," + in.end + "]");
		}
	}

	/**
	 * Adds the given {@link TimedTail} to this transition
	 * 
	 * @param tail
	 *            The {@link TimedTail} using this transition while training a {@link PDRTA}
	 */
	protected void addTail(TimedTail tail) {
		tails.put(new Integer(tail.getTimeDelay()), tail);
	}

	/**
	 * Sets the given {@link PDRTAState} as target for this transition
	 * 
	 * @param state
	 *            The target to be set
	 */
	public void setTarget(PDRTAState state) {

		if (state == null) {
			throw new IllegalArgumentException("The target state of an interval must not be null");
		}
		target = state;
	}

	/**
	 * Checks whether this transition contains the given {@link TimedTail}
	 * 
	 * @param tail
	 *            The {@link TimedTail} to be checked
	 * @return Returns {@code true} if the given tail uses this transition. Returns {@code false} otherwise.
	 */
	public boolean containsTail(TimedTail tail) {
		if (tail != null) {
			return tails.containsEntry(new Integer(tail.getTimeDelay()), tail);
		} else {
			return false;
		}
	}

	/**
	 * Checks whether this {@link Interval}'s delay guards contain the given time value
	 * 
	 * @param time
	 *            The time value to be checked
	 * @return Returns {@code true} if the given time value is between the delay guards. Returns {@code false} otherwise.
	 */
	protected boolean contains(int time) {

		if (time >= begin && time <= end) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates an {@link Interval} with specified begin and end. It has no target {@link PDRTAState} and contains no {@link TimedTail}s.
	 * 
	 * @param b
	 *            The inclusive begin of the interval
	 * @param e
	 *            The inclusive end of the interval
	 */
	Interval(int b, int e) {

		if (b <= e) {
			begin = b;
			end = e;
		} else {
			begin = e;
			end = b;
		}
		tails = TreeMultimap.create();
		target = null;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + begin;
		result = prime * result + end;
		result = prime * result + ((target == null) ? 0 : target.getIndex());
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
		final Interval other = (Interval) obj;
		if (begin != other.begin) {
			return false;
		}
		if (end != other.end) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (other.target != null) {
			if (target.getIndex() != other.target.getIndex()) {
				return false;
			}
		}
		return true;
	}

	void cleanUp() {
		tails.clear();
	}

}
