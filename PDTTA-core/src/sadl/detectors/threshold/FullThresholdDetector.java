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

package sadl.detectors.threshold;

import gnu.trove.list.TDoubleList;
import sadl.constants.ProbabilityAggregationMethod;

/**
 * 
 * @author Timo Klerx
 *
 */
public class FullThresholdDetector extends AggregatedThresholdDetector {
	double singleEventThreshold;
	double singleTimeThreshold;

	public FullThresholdDetector(ProbabilityAggregationMethod aggType, double aggregatedEventThreshold, double aggregatedTimeThreshold,
			boolean aggregateSublists, double singleEventThreshold, double singleTimeThreshold) {
		super(aggType, aggregatedEventThreshold, aggregatedTimeThreshold, aggregateSublists);
		this.singleEventThreshold = singleEventThreshold;
		this.singleTimeThreshold = singleTimeThreshold;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		final boolean aggregatedResult = super.decide(eventLikelihoods, timeLikelihoods);
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
