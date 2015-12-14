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

package sadl.oneclassclassifier;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.classifiers.CategoricalResults;
import jsat.classifiers.DataPoint;
import jsat.classifiers.neuralnetwork.SOM;
import jsat.linear.DenseVector;
import sadl.constants.ScalingMethod;
import sadl.utils.DatasetTransformationUtils;

/**
 * Uses a SOM to classify instances. Does not work at all.
 * @author timo
 *
 */
public class SomClassifier extends NumericClassifier {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(SomClassifier.class);

	SOM som;
	double threshold;

	public SomClassifier(ScalingMethod scalingMethod, int height, int width, double probThreshold) {
		super(scalingMethod);
		som = new SOM(height, width);
		this.threshold = probThreshold;
	}

	@Override
	protected boolean isOutlierScaled(double[] scaledTestSample) {
		final CategoricalResults result = som.classify(new DataPoint(new DenseVector(scaledTestSample)));
		final double sampleProb = result.getProb(0);
		return sampleProb < threshold;
	}

	@Override
	protected void trainModelScaled(List<double[]> scaledTrainSamples) {
		som.trainC(DatasetTransformationUtils.doublesToClassificationDataSet(scaledTrainSamples));
	}

}
