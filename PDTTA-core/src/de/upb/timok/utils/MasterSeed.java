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
package de.upb.timok.utils;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterSeed {
	private static Logger logger = LoggerFactory.getLogger(MasterSeed.class);

	private static Random r = new Random(1234);
	private static boolean wasSet = false;

	public static void setSeed(long seed) {
		if (wasSet) {
			logger.warn("Replacing Random object with new seed {}", seed);
		}
		r = new Random(seed);
		wasSet = true;
	}

	public static long nextLong() {
		return r.nextLong();
	}

	public static Random nextRandom() {
		return new Random(r.nextLong());
	}
}
