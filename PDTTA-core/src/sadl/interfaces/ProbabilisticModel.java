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
package sadl.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.util.Pair;

import gnu.trove.list.TDoubleList;
import sadl.input.TimedWord;

/**
 * 
 * @author Timo Klerx
 *
 */
public interface ProbabilisticModel extends Model {
	/**
	 * Calculates the symbol and time value probabilities for the given word. The first list contains the symbol probabilities and the second list the time
	 * value probabilities.
	 * @param s the timed word
	 * @return the pair of symbol and time value probabilities
	 */
	Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s);

	default Map<String, Function<TimedWord, Pair<TDoubleList, TDoubleList>>> getAvailableCalcMethods() {
		final Map<String, Function<TimedWord, Pair<TDoubleList, TDoubleList>>> m = new HashMap<>();
		m.put("default", this::calculateProbabilities);
		return m;
	}

	default Function<TimedWord, Pair<TDoubleList, TDoubleList>> getCalcMethodByName(String methodName) {
		final Map<String, Function<TimedWord, Pair<TDoubleList, TDoubleList>>> m = getAvailableCalcMethods();
		return m.get(methodName);
	}

}
