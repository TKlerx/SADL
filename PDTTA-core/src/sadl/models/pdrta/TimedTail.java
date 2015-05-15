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

/**
 * 
 * @author Fabian Witter
 *
 */
public class TimedTail implements Comparable<TimedTail> {

	private final int wordIdx;
	private final int tailIdx;

	private final String symbol;
	private final int timeDelay, symbolAlphIndex, histBarIndex;
	private TimedTail next;

	public TimedTail(String symbol, int timeDelay, TimedTail prev) {

		this.symbol = symbol;
		this.symbolAlphIndex = -1;
		this.timeDelay = timeDelay;
		this.histBarIndex = -1;
		this.wordIdx = -1;
		this.tailIdx = -1;
		this.next = null;
		if (prev != null) {
			prev.next = this;
		}
	}

	protected TimedTail(String symbol, int symbolAlphIndex, int timeDelay, int histBarIndex, int wordIdx, int tailIdx, TimedTail prev) {

		this.symbol = symbol;
		this.symbolAlphIndex = symbolAlphIndex;
		this.timeDelay = timeDelay;
		this.histBarIndex = histBarIndex;
		this.wordIdx = wordIdx;
		this.tailIdx = tailIdx;
		this.next = null;
		if (prev != null) {
			prev.next = this;
		}
	}

	public int getSymbolAlphIndex() {
		return symbolAlphIndex;
	}

	public int getHistBarIndex() {
		return histBarIndex;
	}

	public int getTimeDelay() {
		return timeDelay;
	}

	public TimedTail getNextTail() {
		return next;
	}

	@Override
	public boolean equals(Object o) {

		if (o instanceof TimedTail) {
			final TimedTail t = (TimedTail) o;
			if (timeDelay == t.timeDelay && symbol == t.symbol && wordIdx == t.wordIdx && tailIdx == t.tailIdx) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(TimedTail tail) {

		if (timeDelay > tail.timeDelay) {
			return 1;
		} else if (timeDelay < tail.timeDelay) {
			return -1;
		} else {
			if (symbol.compareTo(tail.symbol) == 1) {
				return 1;
			} else if (symbol.compareTo(tail.symbol) == -1) {
				return -1;
			} else {
				if (wordIdx > tail.wordIdx) {
					return 1;
				} else if (wordIdx < tail.wordIdx) {
					return -1;
				} else {
					if (tailIdx > tail.tailIdx) {
						return 1;
					} else if (tailIdx < tail.tailIdx) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		}
	}

}
