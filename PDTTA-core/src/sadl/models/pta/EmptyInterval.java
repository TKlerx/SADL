/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.models.pta;

public class EmptyInterval extends HalfClosedInterval {

	public EmptyInterval() {
		super(0, 0);
	}

	@Override
	public double getMinimum() {

		throw new UnsupportedOperationException();
	}

	@Override
	public double getMaximum() {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setMinimum(double newMin) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setMaximum(double newMax) {

		throw new UnsupportedOperationException();
	}

	@Override
	public boolean cutLeft(double value) {

		return false;
	}

	@Override
	public boolean cutRight(double value) {

		return false;
	}

	@Override
	public boolean contains(double value) {

		return false;
	}

	// true if interval is included in current interval
	@Override
	public boolean contains(HalfClosedInterval value) {

		if (value instanceof EmptyInterval) {
			return true;
		}

		return false;
	}

	@Override
	public boolean intersects(HalfClosedInterval value) {

		if (contains(value)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean included(HalfClosedInterval value) {

		if (contains(value)) {
			return true;
		}

		return false;
	}

	// returns the intersaction of two intervals
	@Override
	public HalfClosedInterval getIntersectionWith(HalfClosedInterval value) {

		return this;
	}

	@Override
	public String toString() {

		return "[]";
	}

	@Override
	public HalfClosedInterval clone() {

		return new EmptyInterval();
	}
}
