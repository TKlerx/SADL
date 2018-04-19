/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import sadl.input.TimedInput;
import sadl.input.TimedWord;

public class SeqAvgLength {

	public static void main(String[] args) throws IOException {

		final TimedInput inp = TimedInput.parse(Paths.get(args[0]));
		System.out.println("Stats for input: " + args[0]);
		calcAvgLength(inp);

	}

	public static void calcAvgLength(TimedInput inp) {

		final List<TimedWord> words = inp.getWords();
		if (words.size() == 0) {
			System.out.println("Input is empty!");
			return;
		}

		final SummaryStatistics stats = new SummaryStatistics();
		words.stream().mapToInt(w -> w.length()).asDoubleStream().forEach(x -> stats.addValue(x));

		System.out.println("NumSeq: " + words.size());
		System.out.println("Mean: " + stats.getMean());
		System.out.println("StdDev: " + stats.getStandardDeviation());
		System.out.println("Min: " + stats.getMin());
		System.out.println("Max: " + stats.getMax());
	}

}
