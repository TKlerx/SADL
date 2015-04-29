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

import sadl.constants.AnomalyInsertionType;

/**
 * 
 * @author Timo Klerx
 *
 */
public class AbnormalTransition extends Transition {

	/**
	 * 
	 */
	private static final long serialVersionUID = 837909038123359196L;
	private final AnomalyInsertionType anomalyType;

	@Override
	public AnomalyInsertionType getAnomalyInsertionType() {
		return anomalyType;
	}

	public AbnormalTransition(int fromState, int toState, int symbol, double probability, AnomalyInsertionType anomalyType) {
		super(fromState, toState, symbol, probability);
		this.anomalyType = anomalyType;
	}

	@Override
	public boolean isAbnormal() {
		return true;
	}

	@Override
	public String toString() {
		return "AbnormalTransition [anomalyType=" + anomalyType + ", fromState=" + fromState + ", toState=" + toState + ", symbol=" + symbol + ", probability="
				+ probability + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((anomalyType == null) ? 0 : anomalyType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbnormalTransition other = (AbnormalTransition) obj;
		if (anomalyType != other.anomalyType) {
			return false;
		}
		return true;
	}

}
