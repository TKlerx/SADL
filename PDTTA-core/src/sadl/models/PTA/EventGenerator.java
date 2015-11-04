package sadl.models.PTA;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import jsat.distributions.empirical.KernelDensityEstimatorDifferentiable;

import org.apache.commons.lang3.Range;

public class EventGenerator {

	protected double bandwidth;

	public EventGenerator(double bandwidth) {

		this.bandwidth = bandwidth;
	}

	public Event generateSplittedEvent(String symbol, double[] times) {

		Arrays.sort(times);

		final KernelDensityEstimatorDifferentiable kernelDensity = new KernelDensityEstimatorDifferentiable(times, bandwidth);
		final Double[] minPoints = kernelDensity.getMinima();
		final TreeMap<Double, SubEvent> events = new TreeMap<>();

		final Event event = new Event(symbol, times, events);

		double minValue = 0;
		int minIndex = 0;

		for (int i = 0; i < minPoints.length; i++) {

			final int maxIndex = Math.abs(Arrays.binarySearch(times, minPoints[i])) - 2; // TODO from index // check
			final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
			final double variance = calculateDeviation(minIndex, maxIndex, times, expectedValue);

			events.put(minValue, new SubEvent(event, i + 1, expectedValue, variance, Range.between(minValue, minPoints[i])));
			minValue = minPoints[i];
			minIndex = maxIndex + 1;
		}

		final int maxIndex = times.length - 1;
		final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
		final double variance = calculateDeviation(minIndex, maxIndex, times, expectedValue);

		events.put(minValue, new SubEvent(event, minPoints.length + 1, expectedValue, variance, Range.between(minValue, Double.POSITIVE_INFINITY)));

		final Iterator<Entry<Double, SubEvent>> subEventsIterator = events.entrySet().iterator();
		SubEvent currentSubEvent = subEventsIterator.next().getValue();

		while (subEventsIterator.hasNext()) {
			final SubEvent nextSubEvent = subEventsIterator.next().getValue();

			currentSubEvent.nextSubEvent = nextSubEvent;
			nextSubEvent.previousSubEvent = currentSubEvent;
			currentSubEvent = nextSubEvent;
		}

		System.out.println("Created event: " + event);

		return event;
	}

	public Event generateNotSplittedEvent(String symbol, double[] times) {

		final TreeMap<Double, SubEvent> events = new TreeMap<>();
		final Event event = new Event(symbol, times, events);

		Arrays.sort(times);
		final double expectedValue = calculateExpectedValue(0, times.length - 1, times);
		final double variance = calculateDeviation(0, times.length - 1, times, expectedValue);
		events.put(0.0, new SubEvent(event, 1, expectedValue, variance, Range.between(0.0, Double.POSITIVE_INFINITY)));

		return event;
	}

	private static double calculateDeviation(int fromIndex, int toIndex, double[] times, double expectedValue) {

		double deviation = 0.0d;

		for (int i = fromIndex; i <= toIndex; i++) {
			deviation = deviation + Math.abs(times[i] - expectedValue);
		}

		return (deviation / (toIndex - fromIndex + 1));
	}

	private static double calculateExpectedValue(int fromIndex, int toIndex, double[] times) {

		double expectedValue = 0.0d;

		for (int i = fromIndex; i <= toIndex; i++) {
			expectedValue = expectedValue + times[i];
		}

		return (expectedValue / (toIndex - fromIndex + 1));
	}
}
