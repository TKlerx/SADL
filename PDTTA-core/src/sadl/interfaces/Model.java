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

package sadl.interfaces;

import gnu.trove.list.TDoubleList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.util.Pair;

import sadl.input.TimedWord;

/**
 * 
 * @author Timo Klerx
 *
 */
public interface Model {

	Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s);

	default List<Function<TimedWord, Pair<TDoubleList, TDoubleList>>> getAvailableCalcMethods() {
		final List<Function<TimedWord, Pair<TDoubleList, TDoubleList>>> m = new ArrayList<>();
		m.add(this::calculateProbabilities);
		return m;
	}

}
