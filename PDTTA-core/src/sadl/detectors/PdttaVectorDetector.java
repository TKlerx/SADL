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

import java.util.ArrayList;
import java.util.List;

import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.TrainableDetector;
import sadl.oneclassclassifier.OneClassClassifier;

/**
 * 
 * @author Timo Klerx
 *
 */
public class PdttaVectorDetector extends PdttaDetector implements TrainableDetector {

	OneClassClassifier c;
	FeatureCreator fc;

	public PdttaVectorDetector(ProbabilityAggregationMethod aggType, FeatureCreator fc, OneClassClassifier c) {
		super(aggType);
		this.c = c;
		this.fc = fc;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		final double[] vector = fc.createFeatures(eventLikelihoods, timeLikelihoods, aggType);
		return c.isOutlier(vector);
	}

	@Override
	public void train(TimedInput trainingInput) {
		final List<double[]> trainingSet = new ArrayList<>(trainingInput.size());
		for (final TimedWord s : trainingInput) {
			final TDoubleList eventLikelihoods = computeEventLikelihoods(s);
			final TDoubleList timeLikelihoods = computeTimeLikelihoods(s);
			trainingSet.add(fc.createFeatures(eventLikelihoods, timeLikelihoods, aggType));
		}
		c.train(trainingSet);
	}


}
