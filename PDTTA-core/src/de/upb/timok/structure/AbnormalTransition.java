/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.upb.timok.structure;

import de.upb.timok.constants.AnomalyInsertionType;

public class AbnormalTransition extends Transition {

	/**
	 * 
	 */
	private static final long serialVersionUID = 837909038123359196L;
	private final AnomalyInsertionType anomalyType;

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

}
