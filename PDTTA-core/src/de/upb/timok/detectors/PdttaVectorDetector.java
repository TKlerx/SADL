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
package de.upb.timok.detectors;

import gnu.trove.list.TDoubleList;

import java.util.ArrayList;
import java.util.List;

import de.upb.timok.constants.ProbabilityAggregationMethod;
import de.upb.timok.detectors.featureCreators.FeatureCreator;
import de.upb.timok.interfaces.TrainableDetector;
import de.upb.timok.oneclassclassifier.OneClassClassifier;
import de.upb.timok.structure.TimedSequence;

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
	public void train(List<TimedSequence> trainingSequences) {
		final List<double[]> trainingSet = new ArrayList<>(trainingSequences.size());
		for (final TimedSequence s : trainingSequences) {
			final TDoubleList eventLikelihoods = computeEventLikelihoods(s);
			final TDoubleList timeLikelihoods = computeTimeLikelihoods(s);
			trainingSet.add(fc.createFeatures(eventLikelihoods, timeLikelihoods, aggType));
		}
		c.train(trainingSet);
	}


}
