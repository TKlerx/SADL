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

import org.apache.commons.math3.util.Precision;

public class HalfClosedInterval implements Cloneable {
	protected double min;
	protected double max;

	public HalfClosedInterval(double min, double max) {

		if (Double.isNaN(min) || Double.isNaN(max) || min > max) {
			throw new IllegalArgumentException();
		}

		this.min = min;
		this.max = max;
	}

	public double getMinimum() {

		return min;
	}

	public double getMaximum() {

		return max;
	}

	public void setMinimum(double newMin) {

		if (Double.isNaN(newMin) || newMin > max) {
			throw new IllegalArgumentException();
		}

		min = newMin;
	}

	public void setMaximum(double newMax) {

		if (Double.isNaN(newMax) || newMax < min) {
			throw new IllegalArgumentException();
		}

		max = newMax;
	}

	public boolean cutLeft(double value) {

		if (this.contains(value)) {

			min = value;
			return true;
		}

		return false;
	}

	public boolean cutRight(double value) {

		if (this.contains(value)) {

			max = value;
			return true;
		}

		return false;
	}

	public boolean contains(double value) {

		if (min <= value && value < max) {
			return true;
		}

		return false;
	}

	// true if interval is included in current interval
	public boolean contains(HalfClosedInterval value) {

		if (value instanceof EmptyInterval) {
			return true;
		}

		final double valueMin = value.getMinimum();
		final double valueMax = value.getMaximum();

		if (min <= valueMin && valueMax <= max) {
			return true;
		}

		return false;
	}

	public boolean intersects(HalfClosedInterval value) {

		if (value instanceof EmptyInterval) {
			return true;
		}

		if (contains(value.getMinimum()) || value.contains(min) || (contains(value.getMaximum()) && !Precision.equals(min, value.getMaximum()))
				|| (value.contains(max) && !Precision.equals(max, value.getMinimum()))) {
			return true;
		}

		return false;
	}

	public boolean included(HalfClosedInterval value) {

		if (value instanceof EmptyInterval) {
			return true;
		}

		if (contains(value.getMinimum()) && (contains(value.getMaximum()) || Precision.equals(max, value.getMaximum()))) {
			return true;
		}

		return false;
	}

	// returns the intersaction of two intervals
	public HalfClosedInterval getIntersectionWith(HalfClosedInterval value) {

		if (!intersects(value)) {
			return new EmptyInterval();
		}

		double minIntersection;
		double maxIntersection;

		final double minValue = value.getMinimum();
		final double maxValue = value.getMaximum();

		if (min > minValue) {
			minIntersection = min;
		}else{
			minIntersection = minValue;
		}

		if (max < maxValue) {
			maxIntersection = max;
		}else{
			maxIntersection = maxValue;
		}

		return new HalfClosedInterval(minIntersection, maxIntersection);
	}

	@Override
	public String toString() {

		if (Precision.equals(min, max)) {
			return "[" + min + ";" + max + "]";
		}

		return "[" + min + ";" + max + ")";
	}

	@Override
	public HalfClosedInterval clone() {

		return new HalfClosedInterval(min, max);
	}

}
