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
package sadl.models.distances;

import sadl.input.TimedWord;

public class NLCS implements DistanceMeasure {

	@Override
	public double distanceBetween(TimedWord s1, TimedWord s2) {
		if ((s1 == null) || (s2 == null) || (s1.length() == 0)
				|| ((s2.length() == 0))) {
			return 0;
		}
		if (s1.length() != s2.length()) {
			throw new UnsupportedOperationException(
					"Only implemented for sequences of same length");
		}

		int maxLen = 0;
		final int fl = s1.length();
		final int sl = s2.length();
		final int[][] table = new int[fl + 1][sl + 1];

		for (int i = 1; i <= fl; i++) {
			for (int j = 1; j <= sl; j++) {
				if (s1.getSymbol(i - 1) == s2.getSymbol(j - 1)) {
					if ((i == 1) || (j == 1)) {
						table[i][j] = 1;
					} else {
						table[i][j] = table[i - 1][j - 1] + 1;
					}
					if (table[i][j] > maxLen) {
						maxLen = table[i][j];
					}
				}
			}
		}
		final double result = maxLen / (double) s1.length();
		return result;
	}

}
