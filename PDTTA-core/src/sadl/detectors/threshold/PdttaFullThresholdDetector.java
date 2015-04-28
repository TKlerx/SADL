/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package sadl.detectors.threshold;

import sadl.constants.ProbabilityAggregationMethod;
import gnu.trove.list.TDoubleList;

public class PdttaFullThresholdDetector extends PdttaAggregatedThresholdDetector {
	double singleEventThreshold;
	double singleTimeThreshold;

	public PdttaFullThresholdDetector(ProbabilityAggregationMethod aggType, double aggregatedEventThreshold, double aggregatedTimeThreshold,
			double singleEventThreshold, double singleTimeThreshold) {
		super(aggType, aggregatedEventThreshold, aggregatedTimeThreshold);
		this.singleEventThreshold = singleEventThreshold;
		this.singleTimeThreshold = singleTimeThreshold;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		boolean aggregatedResult = super.decide(eventLikelihoods, timeLikelihoods);
		if (aggregatedResult) {
			return true;
		}
		double eventLikelihood;
		for (int i = 0; i < eventLikelihoods.size(); i++) {
			eventLikelihood = eventLikelihoods.get(i);
			if (eventLikelihood < singleEventThreshold) {
				return true;
			}
		}
		double timeLikelihood;
		for (int i = 0; i < timeLikelihoods.size(); i++) {
			timeLikelihood = timeLikelihoods.get(i);
			if (timeLikelihood < singleTimeThreshold) {
				return true;
			}
		}
		return false;
	}
}
