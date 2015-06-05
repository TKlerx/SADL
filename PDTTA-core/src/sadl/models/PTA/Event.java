package sadl.models.PTA;

import java.util.Arrays;

import jsat.distributions.empirical.KernelDensityEstimatorDifferentiable;

import org.apache.commons.lang3.Range;

public class Event {

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

		for (final SplittedEvent event : splittedEvents) {
			if (event.inIntervall(time)) {
				return event;
			}
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

		double deviation = 0.0d;

		for (int i = fromIndex; i <= toIndex; i++) {
			deviation = deviation + times[i];
		}

		return (deviation / (toIndex - fromIndex + 1));
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

}
