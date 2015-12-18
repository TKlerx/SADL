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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import jsat.distributions.Normal;
import jsat.distributions.empirical.KernelDensityEstimatorButla;
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

		Arrays.parallelSort(times);

		final KernelDensityEstimatorButla kde;
		kde = new KernelDensityEstimatorButla(times, formel, bandwidth);
		final double[] minPoints = kde.getMinima();
		final TreeMap<Double, SubEvent> subEvents = new TreeMap<>();

		final Event event = new Event(symbol, subEvents);

		double minValue = 0;
		int minIndex = 0;

		for (int i = 0; i < minPoints.length; i++) {
			int maxIndex = Math.abs(Arrays.binarySearch(times, minPoints[i])) - 2;
			while (times[maxIndex + 1] < minPoints[i]) {
				maxIndex++;
			}
			if (minIndex > maxIndex) {
				continue;
			}
			final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
			final double deviation = calculateDeviation(minIndex, maxIndex, times, expectedValue);

			final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
			final HalfClosedInterval anomalyInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceAnomaly), expectedValue
					+ differenceAnomaly);
			final double differenceWarning = Math.abs(warningNormalPoint * deviation);
			final HalfClosedInterval warningInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceWarning), expectedValue
					+ differenceWarning);

			subEvents.put(minValue, new SubEvent(event, String.valueOf(i + 1), expectedValue, deviation, new HalfClosedInterval(minValue, minPoints[i]),
					anomalyInterval, warningInterval));
			minValue = minPoints[i];
			minIndex = maxIndex + 1;
		}

		final int maxIndex = times.length - 1;
		final double expectedValue = calculateExpectedValue(minIndex, maxIndex, times);
		final double deviation = calculateDeviation(minIndex, maxIndex, times, expectedValue);

		final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
		final HalfClosedInterval anomalyInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceAnomaly), expectedValue
				+ differenceAnomaly);
		final double differenceWarning = Math.abs(warningNormalPoint * deviation);
		final HalfClosedInterval warningInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceWarning), expectedValue
				+ differenceWarning);

		subEvents.put(minValue,
				new SubEvent(event, String.valueOf(minPoints.length + 1), expectedValue, deviation, new HalfClosedInterval(minValue,
						Double.POSITIVE_INFINITY),
						anomalyInterval, warningInterval));

		final Iterator<Entry<Double, SubEvent>> subEventsIterator = subEvents.entrySet().iterator();
		SubEvent currentSubEvent = subEventsIterator.next().getValue();

		while (subEventsIterator.hasNext()) {
			final SubEvent nextSubEvent = subEventsIterator.next().getValue();

			currentSubEvent.nextSubEvent = nextSubEvent;
			nextSubEvent.previousSubEvent = currentSubEvent;
			currentSubEvent = nextSubEvent;
		}

		return event;
	}

	public Event generateNotSplittedEvent(String symbol, double[] times) {

		final TreeMap<Double, SubEvent> subEvents = new TreeMap<>();
		final Event event = new Event(symbol, subEvents);

		Arrays.parallelSort(times);
		final double expectedValue = calculateExpectedValue(0, times.length - 1, times);
		final double deviation = calculateDeviation(0, times.length - 1, times, expectedValue);

		final double differenceAnomaly = Math.abs(anomalyNormalPoint * deviation);
		final HalfClosedInterval anomalyInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceAnomaly), expectedValue
				+ differenceAnomaly);
		final double differenceWarning = Math.abs(warningNormalPoint * deviation);
		final HalfClosedInterval warningInterval = new HalfClosedInterval(Math.max(0, expectedValue - differenceWarning), expectedValue
				+ differenceWarning);

		subEvents.put(0.0, new SubEvent(event, String.valueOf(1), expectedValue, deviation, new HalfClosedInterval(0.0, Double.POSITIVE_INFINITY),
				anomalyInterval,
				warningInterval));

		return event;
	}

	public Event generateNotTimedEvent(String symbol, double[] times) {

		final TreeMap<Double, SubEvent> subEvents = new TreeMap<>();
		final Event event = new Event(symbol, subEvents);

		Arrays.parallelSort(times);
		final double expectedValue = calculateExpectedValue(0, times.length - 1, times);
		final double deviation = calculateDeviation(0, times.length - 1, times, expectedValue);

		final HalfClosedInterval anomalyInterval = new HalfClosedInterval(0.0, Double.POSITIVE_INFINITY);
		final HalfClosedInterval warningInterval = new HalfClosedInterval(0.0, Double.POSITIVE_INFINITY);

		subEvents.put(0.0, new SubEvent(event, String.valueOf(1), expectedValue, deviation, new HalfClosedInterval(0.0, Double.POSITIVE_INFINITY),
				anomalyInterval,
				warningInterval));

		return event;
	}

	public Event generateSplittedEventWithIsolatedCriticalArea(String symbol, double[] times) {

		final Event event = generateSplittedEvent(symbol, times);
		final ArrayList<SubEventCriticalArea> criticalAreas = new ArrayList<>();
		final TreeMap<Double, SubEvent> newSubEvents = new TreeMap<>();
		final Event newEvent = new Event(symbol, newSubEvents);

		for (final SubEvent subEvent : event) {
			if (subEvent.hasRightCriticalArea()) {
				final SubEvent nextSubEvent = subEvent.getNextSubEvent();
				final HalfClosedInterval interval1 = nextSubEvent.getAnomalyBounds();
				final HalfClosedInterval interval2 = subEvent.getAnomalyBounds();

				if (interval1.intersects(interval2) && subEvent.getRightAnomalyBound() < nextSubEvent.getRightBound()
						&& subEvent.getRightAnomalyBound() < subEvent.getRightBound() && subEvent.getLeftBound() < nextSubEvent.getLeftAnomalyBound()
						&& subEvent.getLeftAnomalyBound() < nextSubEvent.getLeftAnomalyBound()) {
					final HalfClosedInterval intersection = interval1.getIntersectionWith(interval2);
					final double probLeft = subEvent.calculateProbability(intersection.getMinimum()) / 2.0;
					final double probRight = nextSubEvent.calculateProbability(intersection.getMaximum()) / 2.0;

					final SubEventCriticalArea criticalArea = new SubEventCriticalArea(newEvent, subEvent.getNumber() + ".5",
							(intersection.getMinimum() + intersection.getMaximum()) / 2.0,
							intersection.getMaximum() - intersection.getMinimum(), intersection,
							intersection, intersection, probLeft, probRight);

					subEvent.isolateRightArea(intersection.getMinimum());
					subEvent.setRightBound(subEvent.getAnomalyBounds().getMaximum());
					nextSubEvent.isolateLeftArea(intersection.getMaximum());
					nextSubEvent.setLeftBound(nextSubEvent.getAnomalyBounds().getMinimum());

					subEvent.nextSubEvent = criticalArea;
					nextSubEvent.previousSubEvent = criticalArea;
					criticalArea.previousSubEvent = subEvent;
					criticalArea.nextSubEvent = nextSubEvent;
					criticalAreas.add(criticalArea);
				}
			}

			if (subEvent.hasLeftCriticalArea()) {
				final SubEvent previousSubEvent = subEvent.getPreviousSubEvent();
				final HalfClosedInterval interval1 = previousSubEvent.getAnomalyBounds();
				final HalfClosedInterval interval2 = subEvent.getAnomalyBounds();

				if (interval1.intersects(interval2) && previousSubEvent.getLeftBound() < subEvent.getLeftAnomalyBound()
						&& previousSubEvent.getLeftAnomalyBound() < subEvent.getLeftAnomalyBound()
						&& previousSubEvent.getRightAnomalyBound() < subEvent.getRightBound()
						&& previousSubEvent.getRightAnomalyBound() < subEvent.getRightAnomalyBound()) {
					final HalfClosedInterval intersection = interval1.getIntersectionWith(interval2);
					final double probLeft = previousSubEvent.calculateProbability(intersection.getMinimum()) / 2.0;
					final double probRight = subEvent.calculateProbability(intersection.getMaximum()) / 2.0;

					final SubEventCriticalArea criticalArea = new SubEventCriticalArea(newEvent, previousSubEvent.getNumber() + ".5",
							(intersection.getMinimum() + intersection.getMaximum()) / 2.0, intersection.getMaximum() - intersection.getMinimum(), intersection,
							intersection, intersection, probLeft, probRight);

					previousSubEvent.isolateRightArea(intersection.getMinimum());
					previousSubEvent.setRightBound(previousSubEvent.getAnomalyBounds().getMaximum());
					subEvent.isolateLeftArea(intersection.getMaximum());
					subEvent.setLeftBound(subEvent.getAnomalyBounds().getMinimum());

					previousSubEvent.nextSubEvent = criticalArea;
					subEvent.previousSubEvent = criticalArea;
					criticalArea.previousSubEvent = previousSubEvent;
					criticalArea.nextSubEvent = subEvent;
					criticalAreas.add(criticalArea);
				}
			}

			newSubEvents.put(subEvent.getLeftBound(), subEvent);
		}

		for (final SubEventCriticalArea criticalArea : criticalAreas) {

			newSubEvents.put(criticalArea.getLeftBound(), criticalArea);
		}

		return newEvent;
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
