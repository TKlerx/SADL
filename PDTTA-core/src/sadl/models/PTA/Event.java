package sadl.models.PTA;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import jsat.distributions.empirical.KernelDensityEstimatorDifferentiable;

import org.apache.commons.lang3.Range;

public class Event implements Iterable<SubEvent> {

	protected String symbol;
	protected TreeMap<Double, SubEvent> subEvents;
	protected double[] times;

	private Event(String symbol, double[] times, TreeMap<Double, SubEvent> subEvents) {

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

	private static double calculateVariance(int fromIndex, int toIndex, double[] times, double expectedValue) {

		double variance = 0.0d;

		for (int i = fromIndex; i <= toIndex; i++) {
			variance = variance + Math.abs(times[i] - expectedValue);
		}

		return (variance / (toIndex - fromIndex + 1));
	}

	private static double calculateExpectedValue(int fromIndex, int toIndex, double[] times) {

		double expectedValue = 0.0d;

		for (int i = fromIndex; i <= toIndex; i++) {
			expectedValue = expectedValue + times[i];
		}

		return (expectedValue / (toIndex - fromIndex + 1));
	}

	public static Event generateEvent(String symbol, double[] times){

		Arrays.sort(times);

		final KernelDensityEstimatorDifferentiable kernelDensity = new KernelDensityEstimatorDifferentiable(times);
		final Double[] minPoints = kernelDensity.getMinima();
		final TreeMap<Double, SubEvent> events = new TreeMap<>();

		final Event event = new Event(symbol, times, events);

		final int x = 0;

		double minValue = 0;
		int minIndex = 0;

		for (int i = 0; i < minPoints.length; i++) {

			final int maxIndex = Math.abs(Arrays.binarySearch(times, minPoints[i])) - 2; // TODO from index // check
			final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
			final double variance = calculateVariance(minIndex, maxIndex, times, expectedValue);

			events.put(minValue, new SubEvent(event, i + 1, expectedValue, variance, Range.between(minValue, minPoints[i])));
			minValue = minPoints[i];
			minIndex = maxIndex + 1;
		}

		final int maxIndex = times.length - 1;
		final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
		final double variance = calculateVariance(minIndex, maxIndex, times, expectedValue);

		events.put(minValue, new SubEvent(event, minPoints.length + 1, expectedValue, variance, Range.between(minValue, Double.POSITIVE_INFINITY)));

		final Iterator<Entry<Double, SubEvent>> subEventsIterator = events.entrySet().iterator();
		SubEvent currentSubEvent = subEventsIterator.next().getValue();

		while (subEventsIterator.hasNext()){
			final SubEvent nextSubEvent = subEventsIterator.next().getValue();

			currentSubEvent.nextSubEvent = nextSubEvent;
			nextSubEvent.previousSubEvent = currentSubEvent;
			currentSubEvent = nextSubEvent;
		}

		System.out.println("Created event: " + event);

		return event;
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
