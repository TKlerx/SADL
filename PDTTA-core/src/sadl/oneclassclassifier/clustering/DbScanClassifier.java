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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.DataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.MyDBSCAN;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.VecPaired;
import jsat.linear.distancemetrics.DistanceMetric;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.linear.distancemetrics.ManhattanDistance;
import sadl.constants.DistanceMethod;
import sadl.constants.ScalingMethod;
import sadl.oneclassclassifier.NumericClassifier;
import sadl.utils.DatasetTransformationUtils;

/**
 * 
 * @author Timo Klerx
 *
 */
public class DbScanClassifier extends NumericClassifier {
	// TODO write unit test for this
	private static Logger logger = LoggerFactory.getLogger(DbScanClassifier.class);
	DistanceMetric dm;
	MyDBSCAN dbscan;
	double eps;
	int n;
	private final double threshold;

	private List<List<DataPoint>> clusterResult;
	int[] pointCats;

	public DbScanClassifier(double dbscan_eps, int dbscan_n, DistanceMethod distanceMethod, ScalingMethod scalingMethod) {
		this(dbscan_eps, dbscan_n, dbscan_eps, distanceMethod, scalingMethod);
	}

	public DbScanClassifier(double dbscan_eps, int dbscan_n, double dbscan_threshold, DistanceMethod distanceMethod, ScalingMethod scalingMethod) {
		super(scalingMethod);
		eps = dbscan_eps;
		n = dbscan_n;
		if (distanceMethod == DistanceMethod.EUCLIDIAN) {
			dm = new EuclideanDistance();
		} else if (distanceMethod == DistanceMethod.MANHATTAN) {
			dm = new ManhattanDistance();
		}
		dbscan = new MyDBSCAN(dm);
		this.threshold = dbscan_threshold;
	}

	private void cluster(List<double[]> data) {
		// try {
		// final Path p = Paths.get("toCluster.csv");
		// Files.deleteIfExists(p);
		// Files.createFile(p);
		// IoUtils.writeToFile(data, p);
		// } catch (final IOException e) {
		// logger.error("Unexpected exception", e);
		// }

		pointCats = new int[data.size()];
		final DataSet<?> dataSet = DatasetTransformationUtils.doublesToDataSet(data);
		clusterResult = MyDBSCAN.createClusterListFromAssignmentArray(dbscan.cluster(dataSet, eps, n, pointCats), dataSet);
		final int clusterCount = clusterResult.size();
		logger.info("DBSCAN found {} many clusters.", clusterCount);
		int count = 0;
		for (int i = 0; i < clusterCount; i++) {
			logger.info("Cluster {} has {} many points.", i, clusterResult.get(i).size());
			count += clusterResult.get(i).size();
		}
		logger.info("Original dataset size={}", data.size());
		logger.info("There are {} clustered instances", count);
	}



	@Override
	protected boolean isOutlierScaled(double[] testSample) {
		// TODO do not use eps here, because it we must use a real threshold for testing and not the eps boundary that we used for training
		final List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> neighbours = dbscan.getLastVectorCollection().search(new DenseVector(testSample),
				threshold);
		// check whether one of the points is a core point!
		for (final VecPaired<VecPaired<Vec, Integer>, Double> vecPaired : neighbours) {
			final int dataSetIndex = vecPaired.getVector().getPair();
			final Vec v = vecPaired.getVector().getVector();
			if (pointCats[dataSetIndex] != MyDBSCAN.NOISE) {
				if (isCorePoint(v)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isCorePoint(Vec v) {
		final List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> neighbours = dbscan.getLastVectorCollection().search(new DenseVector(v), eps);
		int nonNoisePoints = 0;
		if (neighbours.size() < n) {
			return false;
		}
		for (final VecPaired<VecPaired<Vec, Integer>, Double> vecPaired : neighbours) {
			final int dataSetIndex = vecPaired.getVector().getPair();
			if (pointCats[dataSetIndex] != MyDBSCAN.NOISE) {
				nonNoisePoints++;
				if (nonNoisePoints >= n) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void trainModelScaled(List<double[]> scaledTrainSamples) {
		cluster(scaledTrainSamples);

	}

}
