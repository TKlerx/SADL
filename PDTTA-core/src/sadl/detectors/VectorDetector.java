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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import gnu.trove.list.TDoubleList;
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
public class VectorDetector extends AnomalyDetector implements TrainableDetector {

	OneClassClassifier c;
	FeatureCreator fc;
	boolean aggSublists;

	public VectorDetector(ProbabilityAggregationMethod aggType, FeatureCreator featureCreator, OneClassClassifier classifier) {
		this(aggType, featureCreator, classifier, false);
	}

	public VectorDetector(ProbabilityAggregationMethod aggType, FeatureCreator featureCreator, OneClassClassifier classifier, boolean aggregateSublists) {
		super(aggType);
		this.c = classifier;
		this.fc = featureCreator;
		this.aggSublists = aggregateSublists;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		// TODO also use aggregateSublists in VectorDetector
		// then also train with sublists!
		if (aggSublists) {
			// do sublists over timelikelihoods because the time list is shorter than the event list
			for (int i = 1; i <= timeLikelihoods.size(); i++) {
				final TDoubleList eventSubList = eventLikelihoods.subList(0, i);
				final TDoubleList timeSubList = timeLikelihoods.subList(0, i);
				final double[] vector = fc.createFeatures(eventSubList, timeSubList, aggType);
				if (c.isOutlier(vector)) {
					return true;
				}
			}
			return false;
		} else {
			final double[] vector = fc.createFeatures(eventLikelihoods, timeLikelihoods, aggType);
			return c.isOutlier(vector);
		}
	}

	@Override
	public void train(TimedInput trainingInput) {
		final List<double[]> trainingSet = new ArrayList<>(trainingInput.size());
		for (final TimedWord s : trainingInput) {
			final Pair<TDoubleList, TDoubleList> p = model.calculateProbabilities(s);
			if (aggSublists) {
				final TDoubleList eventLikelihoods = p.getKey();
				final TDoubleList timeLikelihoods = p.getValue();
				for (int i = 1; i <= timeLikelihoods.size(); i++) {
					final TDoubleList eventSubList = eventLikelihoods.subList(0, i);
					final TDoubleList timeSubList = timeLikelihoods.subList(0, i);
					final double[] vector = fc.createFeatures(eventSubList, timeSubList, aggType);
					trainingSet.add(vector);
				}
			} else {
				trainingSet.add(fc.createFeatures(p.getKey(), p.getValue(), aggType));
			}
		}
		c.train(trainingSet);
	}
}
