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

/**
 * 
 * @author Fabian Witter
 *
 */
public class TimedTail implements Comparable<TimedTail>, Serializable {

	private static final long serialVersionUID = 1358055456043720632L;

	private final int wordIdx, tailIdx;

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

	public String getSymbol() {
		return symbol;
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
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + symbolAlphIndex;
		result = prime * result + tailIdx;
		result = prime * result + timeDelay;
		result = prime * result + wordIdx;
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
		final TimedTail other = (TimedTail) obj;
		if (symbolAlphIndex != other.symbolAlphIndex) {
			return false;
		}
		if (tailIdx != other.tailIdx) {
			return false;
		}
		if (timeDelay != other.timeDelay) {
			return false;
		}
		if (wordIdx != other.wordIdx) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(TimedTail tail) {

		if (timeDelay > tail.timeDelay) {
			return 1;
		} else if (timeDelay < tail.timeDelay) {
			return -1;
		} else {
			if (symbolAlphIndex > tail.symbolAlphIndex) {
				return 1;
			} else if (symbolAlphIndex < tail.symbolAlphIndex) {
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
