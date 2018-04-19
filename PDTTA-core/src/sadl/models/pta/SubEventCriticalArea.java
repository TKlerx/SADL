/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.models.pta;


public class SubEventCriticalArea extends SubEvent {

	protected int almostSurelyCountPrevSubEvent;
	protected int almostSurelyCountNextSubEvent;

	public SubEventCriticalArea(Event event, String subEventNumber, double expectedValue, double deviation, HalfClosedInterval boundInterval,
			HalfClosedInterval anomalyInterval, HalfClosedInterval warningInterval, double enterProbPrevSubEvent, double enterProbNextSubEvent) {
		super(event, subEventNumber, expectedValue, deviation, boundInterval, anomalyInterval, warningInterval);

		almostSurelyCountPrevSubEvent = (int) (Math.log(0.0000000000000000001d) / Math.log(enterProbPrevSubEvent)) + 1;
		almostSurelyCountNextSubEvent = (int) (Math.log(0.0000000000000000001d) / Math.log(enterProbNextSubEvent)) + 1;
	}

	public int getAlmostSurelyCountPrev() {

		return almostSurelyCountPrevSubEvent;
	}

	public int getAlmostSurelyCountNext() {

		return almostSurelyCountNextSubEvent;
	}
}
