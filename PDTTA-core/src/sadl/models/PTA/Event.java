package sadl.models.PTA;

import java.util.Arrays;
import java.util.Iterator;

import jsat.distributions.empirical.KernelDensityEstimatorDifferentiable;

import org.apache.commons.lang3.Range;

public class Event implements Iterable<SplittedEvent> {

	protected String symbol;
	protected SplittedEvent[] splittedEvents;
	protected double[] times;

	private Event(String symbol, double[] times, SplittedEvent[] splittedEvents) {

		this.symbol = symbol;
		this.times = times;
		this.splittedEvents = splittedEvents;
	}

	public String getSymbol() {

		return symbol;
	}

	public SplittedEvent getSplittedEventFromTime(double time) {

		/*
		 * for (final SplittedEvent event : splittedEvents) { // TODO logn n search? if (event.inIntervall(time)) { return event; } }
		 */

		final int index = Arrays.binarySearch(splittedEvents, time);

		if (index >= 0) {
			return splittedEvents[index];
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
		final SplittedEvent[] events = new SplittedEvent[minPoints.length + 1];

		final Event event = new Event(symbol, times, events);

		final int x = 0;

		double minValue = 0;
		int minIndex = 0;

		for (int i = 0; i < minPoints.length; i++) {

			final int maxIndex = Math.abs(Arrays.binarySearch(times, minPoints[i])) - 2;
			final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
			final double variance = calculateVariance(minIndex, maxIndex, times, expectedValue);

			events[i] = new SplittedEvent(event, i + 1, expectedValue, variance, Range.between(minValue, minPoints[i]));
			minValue = minPoints[i];
			minIndex = maxIndex + 1;
		}

		final int maxIndex = times.length - 1;
		final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
		final double variance = calculateVariance(minIndex, maxIndex, times, expectedValue);

		events[events.length - 1] = new SplittedEvent(event, minPoints.length, expectedValue, variance, Range.between(minValue, Double.POSITIVE_INFINITY));

		return event;
	}

	@Override
	public Iterator<SplittedEvent> iterator() {

		return new Iterator<SplittedEvent>() {
			int i = 0;

			@Override
			public boolean hasNext() {

				return i < splittedEvents.length;
			}

			@Override
			public SplittedEvent next() {

				return splittedEvents[i++];
			}

		};
	}

	@Override
	public String toString() {

		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("Event " + symbol + "(");

		for (final SplittedEvent subEvent : this) {
			stringBuilder.append(subEvent.toString());
		}

		stringBuilder.append(")");

		return stringBuilder.toString();
	}

}
