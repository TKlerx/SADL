package sadl.models.PTA;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Event implements Iterable<SubEvent> {

	protected String symbol;
	protected TreeMap<Double, SubEvent> subEvents;
	protected double[] times;

	Event(String symbol, double[] times, TreeMap<Double, SubEvent> subEvents) {

		this.symbol = symbol;
		this.times = times;
		this.subEvents = subEvents;
	}

	public String getSymbol() {

		return symbol;
	}

	public SubEvent getSubEventByTime(double time) {

		final Entry<Double, SubEvent> subEventEntry = subEvents.floorEntry(time);

		if (subEventEntry == null) {
			return null;
		}

		final SubEvent subEvent = subEventEntry.getValue();

		if (subEvent.isInBounds(time)) {
			return subEvent;
		}

		return null;
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
