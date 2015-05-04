/**
 * This file is part of SADL, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.structure;

import java.io.Serializable;

import sadl.constants.AnomalyInsertionType;

/**
 * 
 * @author Timo Klerx
 *
 */
public class Transition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8764538459984228024L;
	public static final int STOP_TRAVERSING_SYMBOL = -1;
	int fromState, toState, symbol;
	double probability;

	public int getFromState() {
		return fromState;
	}

	public int getToState() {
		return toState;
	}

	public boolean isAbnormal() {
		return false;
	}

	public AnomalyInsertionType getAnomalyInsertionType() {
		return AnomalyInsertionType.NONE;
	}

	public int getSymbol() {
		return symbol;
	}

	public double getProbability() {
		return probability;
	}

	Transition() {

	}

	public Transition(int fromState, int toState, int symbol, double probability) {
		super();
		this.fromState = fromState;
		this.toState = toState;
		this.symbol = symbol;
		this.probability = probability;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromState;
		long temp;
		temp = Double.doubleToLongBits(probability);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + symbol;
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
		if (Double.doubleToLongBits(probability) != Double.doubleToLongBits(other.probability)) {
			return false;
		}
		if (symbol != other.symbol) {
			return false;
		}
		if (toState != other.toState) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Transition [fromState=" + fromState + ", toState=" + toState + ", symbol=" + symbol + ", probability=" + probability + "]";
	}

	public ZeroProbTransition toZeroProbTransition() {
		return new ZeroProbTransition(getFromState(), getToState(), getSymbol());
	}

	public boolean isStopTraversingTransition() {
		return symbol == STOP_TRAVERSING_SYMBOL;
	}


}
