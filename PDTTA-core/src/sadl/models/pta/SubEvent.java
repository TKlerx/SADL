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

import jsat.distributions.empirical.NormalRandomized;

import org.apache.commons.math3.util.Precision;

public class SubEvent {

	protected Event event;
	protected String subEventNumber;
	protected HalfClosedInterval anomalyInterval;
	protected HalfClosedInterval warningInterval;
	protected HalfClosedInterval boundInterval;
	protected double expectedValue;
	protected double deviation;

	protected NormalRandomized normalFunction;

	protected SubEvent previousSubEvent;
	protected SubEvent nextSubEvent;

	public SubEvent(Event event, String subEventNumber, double expectedValue, double deviation, HalfClosedInterval boundInterval,
			HalfClosedInterval anomalyInterval, HalfClosedInterval warningInterval) {

		if (event == null || Double.isNaN(expectedValue) || Double.isNaN(deviation) || boundInterval == null || anomalyInterval == null
				|| warningInterval == null || expectedValue < 0.0 || deviation < 0.0) {
			throw new IllegalArgumentException();
		}

		this.event = event;
		this.subEventNumber = subEventNumber;
		this.expectedValue = expectedValue;
		this.deviation = deviation;

		if (deviation > 0){
			normalFunction = new NormalRandomized(expectedValue, deviation);
		}

		this.boundInterval = boundInterval;
		this.warningInterval = warningInterval;
		setAnomalyBounds(anomalyInterval);
	}

	public String getSymbol() {

		return event.getSymbol() + "." + subEventNumber;
	}

	public boolean isAnomaly(double time) {

		return !anomalyInterval.contains(time);
	}

	public boolean isInCriticalArea(double time) {

		if (!isAnomaly(time) && !contains(time)) {
			return true;
		}

		return false;
	}

	public boolean isInLeftCriticalArea(double time) {
		if (!isAnomaly(time) && time < boundInterval.getMinimum()) {

			return true;
		}

		return false;
	}

	public boolean isInRightCriticalArea(double time) {
		if (!isAnomaly(time) && boundInterval.getMaximum() > time) {

			return true;
		}

		return false;
	}

	public boolean contains(double time) {

		if (boundInterval.contains(time)) {
			return true;
		}

		return false;
	}


	public double getLeftBound() {

		return boundInterval.getMinimum();
	}

	public double getRightBound() {

		return boundInterval.getMaximum();
	}

	public double getLeftAnomalyBound() {

		return anomalyInterval.getMinimum();
	}

	public double getRightAnomalyBound() {

		return anomalyInterval.getMaximum();
	}

	public HalfClosedInterval getAnomalyBounds() {

		return anomalyInterval;
	}

	public void setAnomalyBounds(HalfClosedInterval bounds) {

		if (bounds == null || bounds.getMinimum() < 0.0) {
			throw new IllegalArgumentException();
		}

		anomalyInterval = bounds;

		if (!anomalyInterval.contains(warningInterval)) {

			warningInterval = anomalyInterval.getIntersectionWith(warningInterval);
		}
	}

	public void setBounds(HalfClosedInterval bounds) {

		this.boundInterval = bounds;
	}

	public void setLeftBound(double bound) {

		boundInterval.setMinimum(bound);
	}

	public void setRightBound(double bound) {

		boundInterval.setMaximum(bound);
	}

	public String getNumber() {

		return subEventNumber;
	}

	public double getExpectedValue() {

		return expectedValue;
	}

	public double getDeviation() {

		return deviation;
	}

	public HalfClosedInterval getInterval() {

		return anomalyInterval.getIntersectionWith(boundInterval);
	}

	/*
	 * public HalfClosedInterval getIntervalInState(PTAState state) {
	 * 
	 * if (state == null) {
	 * throw new IllegalArgumentException();
	 * }
	 * 
	 * return new HalfClosedInterval(getLeftIntervalBoundInState(state), getRightIntervalBoundInState(state));
	 * }
	 */

	/*
	 * public double getLeftIntervalBoundInState(PTAState state) {
	 * 
	 * if (state == null) {
	 * throw new IllegalArgumentException();
	 * }
	 * 
	 * if (previousSubEvent != null && state.outTransitions.containsKey(previousSubEvent.getSymbol())) {
	 * if (this.hasLeftCriticalArea()) {
	 * return getLeftBound();
	 * } else if (previousSubEvent instanceof SubEventCriticalArea
	 * && !state.outTransitions.containsKey(previousSubEvent.getPreviousSubEvent().getSymbol())) {
	 * return previousSubEvent.getLeftBound();
	 * }
	 * }
	 * 
	 * return getLeftAnomalyBound();
	 * }
	 */

	/*
	 * public double getRightIntervalBoundInState(PTAState state) {
	 * 
	 * if (state == null) {
	 * throw new IllegalArgumentException();
	 * }
	 * 
	 * if (nextSubEvent != null && state.outTransitions.containsKey(this.getNextSubEvent().getSymbol())) {
	 * if (this.hasRightCriticalArea()) {
	 * return getRightBound();
	 * } else if (nextSubEvent instanceof SubEventCriticalArea && !state.outTransitions.containsKey(nextSubEvent.getNextSubEvent().getSymbol())) {
	 * return nextSubEvent.getRightBound();
	 * }
	 * }
	 * 
	 * return getRightBound();
	 * }
	 */

	public boolean hasLeftCriticalArea() {

		if (anomalyInterval.getMinimum() < boundInterval.getMinimum()) {
			return true;
		}

		return false;
	}

	public boolean hasRightCriticalArea() {

		if (boundInterval.getMaximum() < anomalyInterval.getMaximum()) {
			return true;
		}

		return false;
	}

	public boolean hasWarning(double time) {

		if (!isAnomaly(time) && !warningInterval.contains(time)) {
			return true;
		}

		return true;
	}

	public SubEvent getPreviousSubEvent() {

		return previousSubEvent;
	}

	public SubEvent getNextSubEvent() {

		return nextSubEvent;
	}

	/*
	 * public SubEvent getPreviousSubEventInState(PTAState state) {
	 * 
	 * final SubEvent prevSubEvent = this.getPreviousSubEvent();
	 * 
	 * while (prevSubEvent != null) {
	 * if (state.getInTransitions().contains(prevSubEvent)) {
	 * 
	 * }
	 * }
	 * }
	 * 
	 * public SubEvent getNextSubEventInState(PTAState state) {
	 * 
	 * return null;
	 * }
	 * 
	 */
	public Event getEvent() {
		return this.event;
	}

	public double generateRandomTime(HalfClosedInterval allowedInterval) {

		if (Precision.equals(deviation, 0)) {
			if (allowedInterval.contains(expectedValue)) {
				return expectedValue;
			} else {
				throw new IllegalArgumentException("Impossible to enter the given interval.");
			}
		}

		final double probability = normalFunction.cdf(allowedInterval.getMaximum()) - normalFunction.cdf(allowedInterval.getMinimum());

		if (probability < 0.01) {
			throw new IllegalArgumentException("Probability to enter the given interval is to low.");
		}

		double randomTime = 0.0d;

		do {
			randomTime = normalFunction.getRandomPoint();

			if (allowedInterval.contains(randomTime)) {
				return randomTime;
			}
		} while (true);

	}

	public double calculateProbability(double time) {

		if (Precision.equals(deviation, 0.0d)) {
			if (Precision.equals(time, expectedValue)) {
				return 1.0d;
			}
			else{
				return 0.0d;
			}
		}

		final double probability = normalFunction.cdf(time);
		double result;
		if (probability > 0.5d){
			result = (1 - probability) * 2.0d;
		} else {
			result = probability * 2.0d;
		}
		return result;
	}

	public boolean isolateLeftArea(double value) {

		if (anomalyInterval.cutLeft(value)) {
			setAnomalyBounds(anomalyInterval);
			return true;
		}

		return false;
	}

	public boolean isolateRightArea(double value) {

		if (anomalyInterval.cutRight(value)) {
			setAnomalyBounds(anomalyInterval);
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.getSymbol() == null ? 0 : event.getSymbol().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SubEvent other = (SubEvent) obj;
		if (event == null) {
			if (other.event != null) {
				return false;
			}
		} else if (!event.getSymbol().equals(other.event.getSymbol())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {

		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(this.getSymbol() + "[" + anomalyInterval.getMinimum() + "[" + this.getLeftBound() + " " + this.getRightBound() + ")"
				+ anomalyInterval.getMaximum() + ")");

		return stringBuilder.toString();
	}

}
