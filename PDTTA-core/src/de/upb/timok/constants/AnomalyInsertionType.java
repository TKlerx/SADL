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
package de.upb.timok.constants;

public enum AnomalyInsertionType {
	TYPE_ONE(1), TYPE_TWO(2), TYPE_THREE(3), TYPE_FOUR(4), TYPE_FIVE(5), ALL(6), NONE(0);
	private final int typeIndex;

	private AnomalyInsertionType(int typeIndex) {
		this.typeIndex = typeIndex;
	}

	public int getTypeIndex() {
		return typeIndex;
	}

}
