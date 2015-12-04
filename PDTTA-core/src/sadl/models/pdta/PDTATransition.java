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

package sadl.models.pdta;

import sadl.models.pta.HalfClosedInterval;
import sadl.models.pta.SubEvent;

public class PDTATransition {

	protected SubEvent event;
	protected PDTAState target;
	protected HalfClosedInterval interval;
	protected double propability;

	PDTATransition(SubEvent event, PDTAState target, HalfClosedInterval interval, double probability) {

		if (event == null) {
			throw new IllegalArgumentException("Event is empty");
		} else if (target == null) {
			throw new IllegalArgumentException("Target is empty");
		} else if (interval == null) {
			throw new IllegalArgumentException("Interval is empty");
		} else if (Double.isNaN(probability) || probability <= 0.0d || probability > 1.0d) {
			throw new IllegalArgumentException("Probability wrong parameter: " + probability);
		}

		this.event = event;
		this.target = target;
		this.interval = interval;
		this.propability = probability;

	}

	public SubEvent getEvent() {

		return event;
	}

	public PDTAState getTarget() {

		return target;
	}

	public double getPropability() {

		return propability;
	}

	public HalfClosedInterval getInterval() {

		return interval;
	}

	public boolean inInterval(double value) {

		if (interval.contains(value)) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {

		return event.getSymbol() + "[" + interval.getMinimum() + "," + interval.getMaximum() + ")=>" + target.id;
	}
}
