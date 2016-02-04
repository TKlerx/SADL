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
package sadl.constants;

public enum Algoname {
	RTI, PDTTA, BUTLA, PETRI_NET, TPTA, PDFA;

	public static Algoname getAlgoname(String string) {
		for (final Algoname loopAlg : Algoname.values()) {
			if (loopAlg.name().equalsIgnoreCase(string)) {
				return loopAlg;
			}
		}
		return null;
	}
}
