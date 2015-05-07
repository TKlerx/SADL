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

package sadl.run;

import java.nio.file.Paths;

import org.apache.commons.math3.util.Pair;

import sadl.input.TimedInput;
import sadl.utils.IoUtils;

public class Test {

	public static void main(String[] args) {
		Pair<TimedInput, TimedInput> bar;

		bar = IoUtils.readTrainTestFile(Paths.get("testfile"), (reader) -> {
			try {
				return TimedInput.parseAlt(reader, 0);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return null;
		});

		final TimedInput ti1 = bar.getKey();
		final TimedInput ti2 = bar.getValue();
		System.out.println();
	}

}
