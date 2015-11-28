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

package sadl.detectors;

import gnu.trove.list.TDoubleList;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.input.TimedWord;
import sadl.models.pdta.PDTA;
import sadl.models.pdta.PDTAState;
import sadl.models.pdta.PDTATransition;
import sadl.models.pta.SubEvent;

public class AnodaDetector extends AnomalyDetector {

	public AnodaDetector(ProbabilityAggregationMethod aggType) {
		super(aggType);
	}

	@Override
	public boolean isAnomaly(TimedWord word) {

		final PDTA pdta = (PDTA) super.model;

		PDTAState currentState = pdta.getRoot();

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDTATransition transition = currentState.getTransition(eventSymbol, time);

			if (transition == null) {
				// System.out.println("ERROR: (" + currentState.getId() + ")");
				return true;
			}

			final SubEvent event = transition.getEvent();

			if (event.hasWarning(time)) {
				// System.out.println("WARNING: time in warning arrea. (" + currentState.getId() + ")");
			}

			if (event.isInCriticalArea(time)) {
				// System.out.println("WARNING: time in critical area. Wrong decision possible. (" + currentState.getId() + ")");
			}

			currentState = transition.getTarget();
		}

		if (!currentState.isFinalState()) {
			// System.out.println("ERROR: ended not in final state. (" + currentState.getId() + ")");
			return true;
		}

		return false;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {

		throw new UnsupportedOperationException();
	}
}
