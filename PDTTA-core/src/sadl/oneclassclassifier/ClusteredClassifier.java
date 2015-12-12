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

import jsat.classifiers.DataPoint;
import jsat.clustering.kmeans.KMeans;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;
import sadl.constants.ScalingMethod;
import sadl.utils.DatasetTransformationUtils;

/**
 * First cluster the instances and then use a one class classifier on each cluster
 * 
 * @author Timo Klerx
 *
 */
public class ClusteredClassifier extends NumericClassifier {
	NumericClassifier[] classifiers;
	private final KMeans clustering;

	public ClusteredClassifier(ScalingMethod scalingMethod, KMeans clustering) {
		super(scalingMethod);
		this.clustering = clustering;
		this.clustering.setStoreMeans(true);
	}

	@Override
	protected boolean isOutlierScaled(double[] scaledTestSample) {
		final DistanceMetric dm = clustering.getDistanceMetric();
		final Vec sample = new DenseVector(scaledTestSample);
		int closestCluster = -1;
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < clustering.getMeans().size(); i++) {
			final Vec clusterVector = clustering.getMeans().get(i);
			final double dist = dm.dist(sample, clusterVector);
			if (dist < minDistance) {
				if (classifiers[i] != null) {
					closestCluster = i;
					minDistance = dist;
				}
			}
		}
		if (closestCluster == -1) {
			System.out.println();
			System.out.println();
		}
		return classifiers[closestCluster].isOutlierScaled(scaledTestSample);
	}

	@Override
	protected void trainModelScaled(List<double[]> scaledTrainSamples) {
		final List<List<DataPoint>> clusters = clustering.cluster(DatasetTransformationUtils.doublesToDataSet(scaledTrainSamples));
		classifiers = new NumericClassifier[clusters.size()];
		for (int i = 0; i < classifiers.length; i++) {
			// classifiers[i] = new SomClassifier(getScalingMethod(), 10, 10, 0.1);
			if (clusters.get(i).isEmpty()) {
				classifiers[i] = null;
			} else {
				classifiers[i] = new LibSvmClassifier(1, 0.2, 0.1, 1, 0.001, 3, getScalingMethod());
				classifiers[i].train(DatasetTransformationUtils.dataPointsToArray(clusters.get(i)));
			}
		}
	}

}
