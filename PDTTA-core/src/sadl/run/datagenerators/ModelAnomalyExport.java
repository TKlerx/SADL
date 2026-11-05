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
package sadl.run.datagenerators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.pta.Event;
import sadl.models.pta.SubEvent;
import sadl.utils.IoUtils;

final class ModelAnomalyExport {
	private ModelAnomalyExport() {
	}

	static Map<String, String> baseSymbols(Map<String, Event> splitEvents) {
		final Map<String, String> baseSymbols = new HashMap<>();
		for (final Event event : splitEvents.values()) {
			for (final SubEvent subEvent : event) {
				baseSymbols.put(subEvent.getSymbol(), event.getSymbol());
			}
		}
		return baseSymbols;
	}

	static void write(Path file, TimedInput training, TimedInput evaluation, Map<String, String> baseSymbols) throws IOException {
		if (baseSymbols.isEmpty()) {
			IoUtils.writeTrainTestFile(file, training, evaluation);
		} else {
			IoUtils.writeTrainTestFile(file, restoreSymbols(training, baseSymbols), restoreSymbols(evaluation, baseSymbols));
		}
	}

	private static TimedInput restoreSymbols(TimedInput input, Map<String, String> baseSymbols) {
		final List<TimedWord> restored = new ArrayList<>(input.size());
		for (final TimedWord word : input) {
			final List<String> symbols = new ArrayList<>(word.length());
			for (int i = 0; i < word.length(); i++) {
				final String symbol = word.getSymbol(i);
				symbols.add(baseSymbols.getOrDefault(symbol, symbol));
			}
			restored.add(new TimedWord(symbols, word.getTimeValues(), word.getLabel()));
		}
		return new TimedInput(restored);
	}
}
