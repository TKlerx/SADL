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
package sadl.detectors.featureCreators;

import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.PdttaDetector;
import gnu.trove.list.TDoubleList;

public class SmallFeatureCreator implements FeatureCreator {

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods, ProbabilityAggregationMethod aggType) {
		double eventMax = eventLikelihoods.max();
		double eventMin = eventLikelihoods.min();
		double eventAgg = PdttaDetector.aggregate(eventLikelihoods, aggType);

		double timeMax = timeLikelihoods.max();
		double timeMin = timeLikelihoods.min();
		double timeAgg = PdttaDetector.aggregate(timeLikelihoods, aggType);
		return new double[] { eventMax, eventMin, eventAgg, timeMax, timeMin, timeAgg };
	}

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		return createFeatures(eventLikelihoods, timeLikelihoods, ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);
	}

}
