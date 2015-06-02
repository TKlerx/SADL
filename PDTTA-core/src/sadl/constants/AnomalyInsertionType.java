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

package sadl.constants;

/**
 * 
 * @author Timo Klerx
 *
 */
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
