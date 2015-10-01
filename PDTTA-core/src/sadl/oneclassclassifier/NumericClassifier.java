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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ScalingMethod;
import sadl.utils.DatasetTransformationUtils;
import sadl.utils.IoUtils;
import sadl.utils.Settings;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * 
 * @author Timo Klerx
 *
 */
public abstract class NumericClassifier implements OneClassClassifier {
	private static Logger logger = LoggerFactory.getLogger(NumericClassifier.class);
	Path classificationTrainFile = Paths.get("classificationTrain.csv");
	Path classificationTestFile = Paths.get("classificationTest.csv");

	Filter filter = null;

	public NumericClassifier(ScalingMethod scalingMethod) {

		if (scalingMethod == ScalingMethod.NORMALIZE) {
			final Normalize n = new Normalize();
			setFilter(n);
		} else if (scalingMethod == ScalingMethod.STANDARDIZE) {
			final Standardize s = new Standardize();
			setFilter(s);
		} else if (scalingMethod == ScalingMethod.NONE) {
			setFilter(null);
		}
		if (Settings.isDebug()) {
			try {
				Files.deleteIfExists(classificationTestFile);
				Files.deleteIfExists(classificationTrainFile);
				Files.createFile(classificationTestFile);
				Files.createFile(classificationTrainFile);
			} catch (final IOException e) {
				logger.error("Unexpected exception occured.", e);
			}
		}

	}

	protected final List<double[]> scale(List<double[]> samples, boolean isTrainingData) {
		if (filter != null) {
			final Instances unscaledInstances = DatasetTransformationUtils.trainingSetToInstances(samples);
			try {
				if (isTrainingData) {
					filter.setInputFormat(unscaledInstances);
				}
				final Instances scaledInstances = Filter.useFilter(unscaledInstances, filter);
				final ArrayList<double[]> result = (ArrayList<double[]>) DatasetTransformationUtils.instancesToDoubles(scaledInstances, true);
				return result;
			} catch (final Exception e) {
				logger.error("Unexpected exception", e);
				throw new IllegalStateException(e);
			}
		} else {
			return samples;
		}
	}

	protected void setFilter(Filter f) {
		filter = f;
	}

	@Override
	public final boolean isOutlier(double[] testSample) {
		if (Settings.isDebug()) {
			try {
				IoUtils.writeToFile(testSample, classificationTestFile);
			} catch (final IOException e) {
				logger.error("Unexpected exception", e);
			}
		}
		return isOutlier(testSample, false);
	}

	public final boolean isOutlier(double[] testSample, boolean alreadyScaled) {
		double[] toEvaluate = testSample;
		if (!alreadyScaled && filter != null) {
			List<double[]> temp = new ArrayList<>();
			temp.add(testSample);
			temp = scale(temp, false);
			toEvaluate = temp.get(0);
		}
		return isOutlierScaled(toEvaluate);
	}

	protected abstract boolean isOutlierScaled(double[] scaledTestSample);

	protected abstract void trainModelScaled(List<double[]> scaledTrainSamples);

	@Override
	public final void train(List<double[]> trainingSamples) {
		if (Settings.isDebug()) {
			try {
				IoUtils.writeToFile(trainingSamples, classificationTrainFile);
			} catch (final IOException e) {
				logger.error("Unexpected exception", e);
			}
		}
		trainModelScaled(scale(trainingSamples, true));
	}

}
