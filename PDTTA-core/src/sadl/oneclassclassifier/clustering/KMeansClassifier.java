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

package sadl.oneclassclassifier.clustering;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.classifiers.DataPoint;
import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.linear.distancemetrics.ManhattanDistance;
import sadl.constants.DistanceMethod;
import sadl.constants.ScalingMethod;
import sadl.oneclassclassifier.NumericClassifier;
import sadl.utils.DatasetTransformationUtils;
import sadl.utils.MasterSeed;

/**
 * 
 * @author Timo Klerx
 *
 */
public class KMeansClassifier extends NumericClassifier {
	private static Logger logger = LoggerFactory.getLogger(KMeansClassifier.class);

	private final double distanceThreshold;
	private DistanceMetric dm;

	private final int k;

	public KMeansClassifier(ScalingMethod scalingMethod, int k, double distanceThreshold, DistanceMethod distanceMethod) {
		super(scalingMethod);
		this.distanceThreshold = distanceThreshold;
		if (distanceMethod == DistanceMethod.EUCLIDIAN) {
			dm = new EuclideanDistance();
		} else if (distanceMethod == DistanceMethod.MANHATTAN) {
			dm = new ManhattanDistance();
		}
		this.k = k;
	}

	@Override
	public boolean isOutlierScaled(double[] toEvaluate) {
		// check distance to the next center
		// does not make sense to check for the next point because all points belong to a cluster (not like in DBScan where noise points exist)

		final Vec sample = new DenseVector(toEvaluate);
		for (final Vec v : means) {
			if (dm.dist(v, sample) < distanceThreshold) {
				return false;
			}
		}

		return true;
	}

	private List<Vec> means = new ArrayList<>();


	@Override
	public void trainModelScaled(List<double[]> trainSamples) {
		final SeedSelection ss = SeedSelection.KPP;
		// TODO create parent class for kMeans clustering, that is shared by kMeans, gMeans, xMeans
		final HamerlyKMeans kMeans = new HamerlyKMeans(dm, ss, MasterSeed.nextRandom());
		kMeans.setStoreMeans(true);
		final List<List<DataPoint>> clusterResult = kMeans.cluster(DatasetTransformationUtils.doublesToDataSet(trainSamples), k);
		means = kMeans.getMeans();
		final int clusterCount = clusterResult.size();
		logger.info("There are {} many clusters.", clusterCount);
		int count = 0;
		for (int i = 0; i < clusterCount; i++) {
			logger.info("Cluster {} has {} many points.", i, clusterResult.get(i).size());
			count += clusterResult.get(i).size();
		}
		logger.info("Original dataset size={}", trainSamples.size());
		logger.info("There are {} clustered instances", count);
	}


}
