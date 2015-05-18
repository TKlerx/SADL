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

package sadl.detectors.featureCreators;

import gnu.trove.list.TDoubleList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ProbabilityAggregationMethod;

/**
 * 
 * @author Timo Klerx
 *
 */
public class FullFeatureCreator extends SmallFeatureCreator {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(FullFeatureCreator.class);

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods, ProbabilityAggregationMethod aggType) {
		final double[] superCall = super.createFeatures(eventLikelihoods, timeLikelihoods, aggType);
		final double eventMean = eventLikelihoods.sum() / eventLikelihoods.size();
		final double timeMean;
		if (timeLikelihoods.size() == 0) {
			// happens if the first state is the final state
			timeMean = Double.NEGATIVE_INFINITY;
		} else {
			timeMean = timeLikelihoods.sum() / timeLikelihoods.size();
		}
		return new double[] { superCall[0], superCall[1], superCall[2], eventMean, superCall[3], superCall[4], superCall[5], timeMean };
	}
}
