package sadl.oneclassclassifier.clustering;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.classifiers.DataPoint;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.linear.distancemetrics.ManhattanDistance;
import jsat.utils.Pair;
import sadl.constants.DistanceMethod;
import sadl.constants.ScalingMethod;
import sadl.oneclassclassifier.NumericClassifier;

public abstract class KMeansBaseClustering extends NumericClassifier {
	private static Logger logger = LoggerFactory.getLogger(KMeansBaseClustering.class);

	final double distanceThreshold;
	private DistanceMetric dm;
	private final List<Vec> storedMeans = new ArrayList<>();
	private final int minClusterPoints;

	public KMeansBaseClustering(ScalingMethod scalingMethod, DistanceMethod distanceMethod, double distanceThreshold, int minClusterPoints) {
		super(scalingMethod);
		this.distanceThreshold = distanceThreshold;
		if (distanceMethod == DistanceMethod.EUCLIDIAN) {
			dm = new EuclideanDistance();
		} else if (distanceMethod == DistanceMethod.MANHATTAN) {
			dm = new ManhattanDistance();
		}
		this.minClusterPoints = minClusterPoints;
	}

	@Override
	public boolean isOutlierScaled(double[] toEvaluate) {
		// check distance to the next center
		// does not make sense and is thus not done to check for the next point because all points belong to a cluster (not like in DBScan where noise points
		// exist)
		final Vec sample = new DenseVector(toEvaluate);
		for (final Vec v : storedMeans) {
			if (dm.dist(v, sample) < distanceThreshold) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void trainModelScaled(List<double[]> trainSamples) {
		final Pair<List<Vec>, List<List<DataPoint>>> result = cluster(trainSamples);
		final List<Vec> means = result.getFirstItem();

		final List<List<DataPoint>> clusterList = result.getSecondItem();
		final int clusterCount = clusterList.size();
		logger.info("There are {} many clusters.", clusterCount);
		int count = 0;
		storedMeans.clear();
		for (int i = 0; i < clusterCount; i++) {
			final int clusterSize = clusterList.get(i).size();
			if (clusterSize >= minClusterPoints) {
				storedMeans.add(means.get(i));
			}
			logger.info("Cluster {} has {} many points.", i, clusterSize);
			count += clusterList.get(i).size();
		}
		logger.info("Original dataset size={}", trainSamples.size());
		logger.info("There are {} clustered instances", count);
	}

	abstract protected Pair<List<Vec>, List<List<DataPoint>>> cluster(List<double[]> trainSamples);

	public DistanceMetric getDistanceMetric() {
		return dm;
	}

}
