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

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.PdttaDetector;

/**
 * 
 * @author Timo Klerx
 *
 */
public class PdttaAggregatedThresholdDetector extends PdttaDetector {
	private static Logger logger = LoggerFactory.getLogger(PdttaAggregatedThresholdDetector.class);

	double aggregatedEventThreshold;
	double aggregatedTimeThreshold;
	boolean aggregateSublists;

	public PdttaAggregatedThresholdDetector(ProbabilityAggregationMethod aggType, double aggregatedEventThreshold, double aggregatedTimeThreshold,
			boolean aggregateSublists) {
		super(aggType);
		this.aggregatedEventThreshold = aggregatedEventThreshold;
		this.aggregatedTimeThreshold = aggregatedTimeThreshold;
		this.aggregateSublists = aggregateSublists;
	}

	public PdttaAggregatedThresholdDetector(double aggregatedEventThreshold, double aggregatedTimeThreshold) {
		this(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, aggregatedEventThreshold, aggregatedTimeThreshold, false);
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		if(aggregateSublists){
			final Pair<TDoubleList, TDoubleList> anomalyTrend = computeAggregatedTrendLikelihood(eventLikelihoods, timeLikelihoods);
			final TDoubleList eventLHs = anomalyTrend.getKey();
			final TDoubleList timeLHs = anomalyTrend.getValue();
			double eventLh;
			double timeLh;
			for (int i = 0; i < eventLHs.size(); i++) {
				eventLh = eventLHs.get(i);
				if (eventLh <= aggregatedEventThreshold) {
					return true;
				}
			}
			for (int i = 0; i < timeLHs.size(); i++) {
				timeLh = timeLHs.get(i);
				if (timeLh <= aggregatedTimeThreshold) {
					return true;
				}
			}
			return false;
		}else{
			return myDecide(eventLikelihoods, timeLikelihoods);
		}
	}

	private  boolean myDecide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		final double normalizedEventThreshold = aggregatedEventThreshold;
		final double normalizedTimeThreshold = aggregatedTimeThreshold;
		// What happens if aggregate returns NaN?!
		// Should only return -infty
		final double aggregatedEventScore = aggregate(eventLikelihoods, aggType);
		final double aggregatedTimeScore = aggregate(timeLikelihoods, aggType);
		logger.debug("aggEventScore={}\taggTimeScore={}", aggregatedEventScore, aggregatedTimeScore);

		if (aggregatedEventScore <= normalizedEventThreshold || aggregatedTimeScore <= normalizedTimeThreshold) {
			// one of the scores is so low, that this indicates an anomaly
			return true;
		} else {
			return false;
		}

	}
}
