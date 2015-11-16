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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import jsat.distributions.Normal;
import jsat.distributions.empirical.KernelDensityEstimatorButla;

import org.apache.commons.lang3.Range;

import sadl.constants.KDEFormelVariant;

public class EventGenerator {

	protected double bandwidth;
	protected double anomalyNormalPoint;
	protected double warningNormalPoint;
	protected KDEFormelVariant formel;

	public EventGenerator(double bandwidth, double anomalyProbability, double warningProbability, KDEFormelVariant formel) {

		this.bandwidth = bandwidth;

		final Normal standardNormalFunction = new Normal();
		anomalyNormalPoint = standardNormalFunction.invCdf(anomalyProbability);
		warningNormalPoint = standardNormalFunction.invCdf(warningProbability);
		this.formel = formel;
	}

	public Event generateSplittedEvent(String symbol, double[] times) {

		Arrays.sort(times);

		final KernelDensityEstimatorButla kde = new KernelDensityEstimatorButla(times, formel, bandwidth);
		final Double[] minPoints = kde.getMinima();
		final TreeMap<Double, SubEvent> events = new TreeMap<>();

		final Event event = new Event(symbol, times, events);

		double minValue = 0;
		int minIndex = 0;

		for (int i = 0; i < minPoints.length; i++) {

			final int maxIndex = Math.abs(Arrays.binarySearch(times, minPoints[i])) - 2; // TODO from index // check
			final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
			final double deviation = calculateDeviation(minIndex, maxIndex, times, expectedValue);

			final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
			final Range<Double> anomalyInterval = Range.between(Math.max(0, expectedValue - differenceAnomaly), expectedValue + differenceAnomaly);
			final double differenceWarning = Math.abs(warningNormalPoint * deviation);
			final Range<Double> warningInterval = Range.between(Math.max(0, expectedValue - differenceWarning), expectedValue + differenceWarning);

			events.put(minValue, new SubEvent(event, String.valueOf(i + 1), expectedValue, deviation, Range.between(minValue, minPoints[i]), anomalyInterval,
					warningInterval));
			minValue = minPoints[i];
			minIndex = maxIndex + 1;
		}

		final int maxIndex = times.length - 1;
		final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
		final double deviation = calculateDeviation(minIndex, maxIndex, times, expectedValue);

		final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
		final Range<Double> anomalyInterval = Range.between(Math.max(0, expectedValue - differenceAnomaly), expectedValue + differenceAnomaly);
		final double differenceWarning = Math.abs(warningNormalPoint * deviation);
		final Range<Double> warningInterval = Range.between(Math.max(0, expectedValue - differenceWarning), expectedValue + differenceWarning);

		events.put(minValue,
				new SubEvent(event, String.valueOf(minPoints.length + 1), expectedValue, deviation, Range.between(minValue, Double.POSITIVE_INFINITY),
						anomalyInterval, warningInterval));

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
		final double deviation = calculateDeviation(0, times.length - 1, times, expectedValue);

		final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
		final Range<Double> anomalyInterval = Range.between(Math.max(0, expectedValue - differenceAnomaly), expectedValue + differenceAnomaly);
		final double differenceWarning = Math.abs(warningNormalPoint * deviation);
		final Range<Double> warningInterval = Range.between(Math.max(0, expectedValue - differenceWarning), expectedValue + differenceWarning);

		events.put(0.0, new SubEvent(event, String.valueOf(1), expectedValue, deviation, Range.between(0.0, Double.POSITIVE_INFINITY), anomalyInterval,
				warningInterval));

		return event;
	}

	public Event generateSplittedEventWithIsolatedCriticalArea(String symbol, double[] times) {

		final Event event = generateSplittedEvent(symbol, times);

		for (final SubEvent subEvent : event) {
			if (subEvent.hasRightCriticalArea()) {

				final Range<Double> interval = Range.between(subEvent.getRightBound(), subEvent.getRightAnomalyBound());
				final SubEvent criticalArea = new SubEvent(event, subEvent.getNumber() + ".5", subEvent.getExpectedValue(), subEvent.getDeviation(), interval,
						interval, interval);
				final SubEvent nextSubEvent = subEvent.nextSubEvent;
				subEvent.isolateRightCriticalArea();
				nextSubEvent.isolateLeftCriticalArea();
				subEvent.nextSubEvent = criticalArea;
				nextSubEvent.previousSubEvent = criticalArea;
				criticalArea.previousSubEvent = subEvent;
				criticalArea.nextSubEvent = nextSubEvent;
			}

		}

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
