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
package sadl.structure;

import java.io.Serializable;

/**
 * 
 * @author Timo Klerx
 *
 */
public class ZeroProbTransition extends Transition implements Serializable {

	private static final long serialVersionUID = -5201104088874076824L;

	public ZeroProbTransition(int fromState, int toState, String symbol) {
		super();
		this.fromState = fromState;
		this.toState = toState;
		this.symbol = symbol;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromState;
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + toState;
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
		final Transition other = (Transition) obj;
		if (fromState != other.fromState) {
			return false;
		}
		if (!symbol.equals(other.symbol)) {
			return false;
		}
		if (toState != other.toState) {
			return false;
		}
		return true;
	}


	@Override
	public double getProbability() {
		throw new UnsupportedOperationException("This method is not supported for this class");
	}

	@Override
	public ZeroProbTransition toZeroProbTransition() {
		return this;
	}

	@Override
	public String toString() {
		return "ZeroProbTransition [fromState=" + fromState + ", toState=" + toState + ", symbol=" + symbol + "]";
	}

}
