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

package sadl.models.pta;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import sadl.utils.MasterSeed;

public class Event implements Iterable<SubEvent> {

	protected String symbol;
	protected TreeMap<Double, SubEvent> subEvents;

	// protected double[] times;
	Random rand;
	public Event(String symbol, TreeMap<Double, SubEvent> subEvents) {

		this.symbol = symbol;
		// this.times = times; // TODO remove?
		this.subEvents = subEvents;
		rand = MasterSeed.nextRandom();
	}

	public String getSymbol() {

		return symbol;
	}

	public int getSubEventsCount() {

		return subEvents.size();
	}

	public SubEvent getSubEventByTime(double time) {

		final Entry<Double, SubEvent> subEventEntry = subEvents.floorEntry(time);

		if (subEventEntry == null) {
			return null;
		}

		final SubEvent subEvent = subEventEntry.getValue();

		if (subEvent.contains(time)) {
			return subEvent;
		}

		return null;
	}

	public SubEvent getRandomSubEvent() {
		return (SubEvent) subEvents.values().toArray()[rand.nextInt(subEvents.size())];
	}


	@Override
	public Iterator<SubEvent> iterator() {

		return new Iterator<SubEvent>() {
			Iterator<Entry<Double, SubEvent>> iterator = subEvents.entrySet().iterator();

			@Override
			public boolean hasNext() {

				return iterator.hasNext();
			}

			@Override
			public SubEvent next() {

				return iterator.next().getValue();
			}

		};
	}

	@Override
	public String toString() {

		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(symbol + ":");

		for (final SubEvent subEvent : this) {
			stringBuilder.append(subEvent.toString());
		}

		stringBuilder.append(")");

		return stringBuilder.toString();
	}

}
