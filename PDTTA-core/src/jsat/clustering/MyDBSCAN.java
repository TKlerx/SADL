/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package jsat.clustering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsat.DataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.DataPoint;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.VecPaired;
import jsat.linear.distancemetrics.DistanceMetric;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.linear.distancemetrics.TrainableDistanceMetric;
import jsat.linear.vectorcollection.KDTree;
import jsat.linear.vectorcollection.VectorCollection;
import jsat.linear.vectorcollection.VectorCollectionFactory;
import jsat.math.OnLineStatistics;
import jsat.utils.SystemInfo;

/**
 * A density-based algorithm for discovering clusters in large spatial databases with noise (1996) by Martin Ester , Hans-peter Kriegel , Jörg S , Xiaowei Xu
 * 
 * @author Edward Raff
 * @author Timo Klerx
 */
@SuppressWarnings("rawtypes")
public class MyDBSCAN extends ClustererBase implements Cloneable {

	private static final long serialVersionUID = -2682331793218766965L;
	/**
	 * Used by {@link #cluster(DataSet, double, int, VectorCollection,int[]) } to mark that a data point as not yet been visited. <br>
	 * Clusters that have been visited have a value >= 0, that indicates their cluster. Or have the value {@link #NOISE}
	 */
	private static final int UNCLASSIFIED = -1;
	/**
	 * Used by {@link #expandCluster(int[], DataSet, int, int, double, int, VectorCollection) } to mark that a data point has been visited, but was considered
	 * noise.
	 */
	@SuppressWarnings("javadoc")
	public static final int NOISE = -2;

	/**
	 * Factory used to create a vector space of the inputs. The paired Integer is the vector's index in the original dataset
	 */
	private final VectorCollectionFactory<VecPaired<Vec, Integer>> vecFactory;
	private final DistanceMetric dm;
	private double stndDevs = 2.0;
	private VectorCollection<VecPaired<Vec, Integer>> lastVectorCollection;

	public MyDBSCAN(DistanceMetric dm, VectorCollectionFactory<VecPaired<Vec, Integer>> vecFactory) {
		this.dm = dm;
		this.vecFactory = vecFactory;
	}

	public MyDBSCAN() {
		this(new EuclideanDistance(), new KDTree.KDTreeFactory<VecPaired<Vec, Integer>>());
	}

	public MyDBSCAN(DistanceMetric dm) {
		this(dm, new KDTree.KDTreeFactory<VecPaired<Vec, Integer>>());
	}

	/**
	 * Copy constructor
	 * 
	 * @param toCopy
	 *            the object to copy
	 */
	public MyDBSCAN(MyDBSCAN toCopy) {
		this.vecFactory = toCopy.vecFactory.clone();
		this.dm = toCopy.dm.clone();
		this.stndDevs = toCopy.stndDevs;
	}

	public List<List<DataPoint>> cluster(DataSet<?> dataSet, int minPts) {
		return createClusterListFromAssignmentArray(cluster(dataSet, minPts, (int[]) null), dataSet);
	}

	public int[] cluster(DataSet<?> dataSet, int minPts, int[] designations) {
		final OnLineStatistics stats = new OnLineStatistics();
		TrainableDistanceMetric.trainIfNeeded(dm, dataSet);
		final VectorCollection<VecPaired<Vec, Integer>> vc = vecFactory.getVectorCollection(getVecIndexPairs(dataSet), dm);

		final List<DataPoint> dps = dataSet.getDataPoints();
		for (final DataPoint dp : dps) {
			stats.add(vc.search(dp.getNumericalValues(), minPts + 1).get(minPts).getPair());
		}

		final double eps = stats.getMean() + stats.getStandardDeviation() * stndDevs;

		return cluster(dataSet, eps, minPts, vc, designations);
	}

	@Override
	public int[] cluster(DataSet dataSet, int[] designations) {
		return cluster(dataSet, 3, designations);
	}

	@Override
	public int[] cluster(DataSet dataSet, ExecutorService threadpool, int[] designations) {
		return cluster(dataSet, 3, threadpool, designations);
	}

	@Override
	public MyDBSCAN clone() {
		return new MyDBSCAN(this);
	}

	private class StatsWorker implements Callable<OnLineStatistics> {
		final BlockingQueue<DataPoint> queue;
		final VectorCollection<VecPaired<Vec, Integer>> vc;
		final int minPts;

		public StatsWorker(BlockingQueue<DataPoint> queue, VectorCollection<VecPaired<Vec, Integer>> vc, int minPts) {
			this.queue = queue;
			this.vc = vc;
			this.minPts = minPts;
		}

		@Override
		public OnLineStatistics call() throws Exception {
			final OnLineStatistics stats = new OnLineStatistics();
			while (true) {
				final DataPoint dp = queue.take();

				if (dp.numNumericalValues() == 0 && dp.numCategoricalValues() == 0) {
					break;// Posion value, nonsense data point used to signal end
				}
				stats.add(vc.search(dp.getNumericalValues(), minPts + 1).get(minPts).getPair());
			}
			return stats;
		}

	}

	public List<List<DataPoint>> cluster(DataSet dataSet, int minPts, ExecutorService threadpool) {
		return createClusterListFromAssignmentArray(cluster(dataSet, minPts, threadpool, null), dataSet);
	}

	public int[] cluster(DataSet dataSet, int minPts, ExecutorService threadpool, int[] designations) {
		OnLineStatistics stats = null;
		TrainableDistanceMetric.trainIfNeeded(dm, dataSet, threadpool);
		final VectorCollection<VecPaired<Vec, Integer>> vc = vecFactory.getVectorCollection(getVecIndexPairs(dataSet), dm);

		final BlockingQueue<DataPoint> queue = new ArrayBlockingQueue<>(SystemInfo.L2CacheSize * 2);
		final List<Future<OnLineStatistics>> futures = new ArrayList<>(SystemInfo.LogicalCores);

		// Setup
		for (int i = 0; i < SystemInfo.LogicalCores; i++) {
			futures.add(threadpool.submit(new StatsWorker(queue, vc, minPts)));
		}
		// Feed data
		for (int i = 0; i < dataSet.getSampleSize(); i++) {
			queue.add(dataSet.getDataPoint(i));
		}
		// Posion stop
		for (int i = 0; i < SystemInfo.LogicalCores; i++) {
			queue.add(new DataPoint(new DenseVector(0), new int[0], new CategoricalData[0]));
		}

		for (final Future<OnLineStatistics> future : futures) {
			try {
				if (stats == null) {
					stats = future.get();
				} else {
					stats = OnLineStatistics.add(stats, future.get());
				}
			} catch (final InterruptedException ex) {
				Logger.getLogger(MyDBSCAN.class.getName()).log(Level.SEVERE, null, ex);
			} catch (final ExecutionException ex) {
				Logger.getLogger(MyDBSCAN.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@SuppressWarnings("null")
		final double eps = stats.getMean() + stats.getStandardDeviation() * stndDevs;

		return cluster(dataSet, eps, minPts, vc, threadpool, designations);
	}

	private List<VecPaired<Vec, Integer>> getVecIndexPairs(DataSet dataSet) {
		final List<VecPaired<Vec, Integer>> vecs = new ArrayList<>(dataSet.getSampleSize());
		for (int i = 0; i < dataSet.getSampleSize(); i++) {
			vecs.add(new VecPaired<>(dataSet.getDataPoint(i).getNumericalValues(), i));
		}
		return vecs;
	}

	public List<List<DataPoint>> cluster(DataSet dataSet, double eps, int minPts) {
		return createClusterListFromAssignmentArray(cluster(dataSet, eps, minPts, (int[]) null), dataSet);
	}

	public int[] cluster(DataSet dataSet, double eps, int minPts, int[] designations) {
		TrainableDistanceMetric.trainIfNeeded(dm, dataSet);
		return cluster(dataSet, eps, minPts, vecFactory.getVectorCollection(getVecIndexPairs(dataSet), dm), designations);
	}

	public List<List<DataPoint>> cluster(DataSet dataSet, double eps, int minPts, ExecutorService threadpool) {
		return createClusterListFromAssignmentArray(cluster(dataSet, eps, minPts, threadpool, null), dataSet);
	}

	public int[] cluster(DataSet dataSet, double eps, int minPts, ExecutorService threadpool, int[] designations) {
		TrainableDistanceMetric.trainIfNeeded(dm, dataSet, threadpool);
		return cluster(dataSet, eps, minPts, vecFactory.getVectorCollection(getVecIndexPairs(dataSet), dm), threadpool, designations);
	}

	public int[] cluster(DataSet dataSet, double eps, int minPts, VectorCollection<VecPaired<Vec, Integer>> vc, int[] pointCats) {
		if (pointCats == null) {
			pointCats = new int[dataSet.getSampleSize()];
		}
		Arrays.fill(pointCats, UNCLASSIFIED);

		int curClusterID = 0;
		for (int i = 0; i < pointCats.length; i++) {
			if (pointCats[i] == UNCLASSIFIED) {
				// All assignments are done by expandCluster
				if (expandCluster(pointCats, dataSet, i, curClusterID, eps, minPts, vc)) {
					curClusterID++;
				}
			}
		}
		lastVectorCollection = vc;
		return pointCats;
	}

	private int[] cluster(DataSet dataSet, double eps, int minPts, VectorCollection<VecPaired<Vec, Integer>> vc, ExecutorService threadpool, int[] pointCats) {
		if (pointCats == null) {
			pointCats = new int[dataSet.getSampleSize()];
		}
		Arrays.fill(pointCats, UNCLASSIFIED);

		final BlockingQueue<List<? extends VecPaired<VecPaired<Vec, Integer>, Double>>> resultQ = new SynchronousQueue<>();
		final BlockingQueue<Vec> sourceQ = new LinkedBlockingQueue<>();

		// Set up workers
		for (int i = 0; i < SystemInfo.LogicalCores; i++) {
			threadpool.submit(new ClusterWorker(vc, eps, resultQ, sourceQ));
		}

		int curClusterID = 0;
		for (int i = 0; i < pointCats.length; i++) {
			if (pointCats[i] == UNCLASSIFIED) {
				// All assignments are done by expandCluster
				if (expandCluster(pointCats, dataSet, i, curClusterID, eps, minPts, vc, resultQ, sourceQ)) {
					curClusterID++;
				}
			}
		}

		// Kill workers
		try {
			for (int i = 0; i < SystemInfo.LogicalCores; i++) {
				sourceQ.put(new DenseVector(0));
			}
		} catch (final InterruptedException interruptedException) {
		}
		lastVectorCollection = vc;
		return pointCats;
	}

	/**
	 * 
	 * @param pointCats
	 *            the array to store the cluster assignments in
	 * @param dataSet
	 *            the data set
	 * @param point
	 *            the current data point we are working on
	 * @param clId
	 *            the current cluster we are working on
	 * @param eps
	 *            the search radius
	 * @param minPts
	 *            the minimum number of points to create a new cluster
	 * @param vc
	 *            the collection to use to search with
	 * @return true if a cluster was expanded, false if the point was marked as noise
	 */
	private boolean expandCluster(int[] pointCats, DataSet dataSet, int point, int clId, double eps, int minPts, VectorCollection<VecPaired<Vec, Integer>> vc) {
		final Vec queryPoint = dataSet.getDataPoint(point).getNumericalValues();
		final List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> seeds = vc.search(queryPoint, eps);

		if (seeds.size() < minPts)// no core point
		{
			pointCats[point] = NOISE;
			return false;
		}
		// Else, all points in seeds are density-reachable from Point

		List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> results;

		pointCats[point] = clId;
		final Queue<VecPaired<VecPaired<Vec, Integer>, Double>> workQue = new ArrayDeque<>(seeds);
		while (!workQue.isEmpty()) {
			final VecPaired<VecPaired<Vec, Integer>, Double> currentP = workQue.poll();
			results = vc.search(currentP, eps);

			if (results.size() >= minPts) {
				for (final VecPaired<VecPaired<Vec, Integer>, Double> resultP : results) {
					final int resultPIndx = resultP.getVector().getPair();
					if (pointCats[resultPIndx] < 0)// is UNCLASSIFIED or NOISE
					{
						if (pointCats[resultPIndx] == UNCLASSIFIED) {
							workQue.add(resultP);
						}
						pointCats[resultPIndx] = clId;
					}
				}
			}
		}

		return true;
	}

	private class ClusterWorker implements Runnable {
		private final VectorCollection<VecPaired<Vec, Integer>> vc;
		private volatile List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> results;
		private final double range;
		private final BlockingQueue<List<? extends VecPaired<VecPaired<Vec, Integer>, Double>>> resultQ;
		private final BlockingQueue<Vec> sourceQ;

		/**
		 * 
		 * @param vc
		 *            the accelerate structure to search for data points. Must support concurent method calls
		 * @param range
		 *            the range to search <tt>tt</tt> with
		 * @param resultQ
		 *            The que to place this worker object into on completion
		 */
		public ClusterWorker(VectorCollection<VecPaired<Vec, Integer>> vc, double range,
				BlockingQueue<List<? extends VecPaired<VecPaired<Vec, Integer>, Double>>> resultQ, BlockingQueue<Vec> sourceQ) {
			this.vc = vc;
			this.range = range;
			this.resultQ = resultQ;
			this.sourceQ = sourceQ;
		}

		@SuppressWarnings("unused")
		public List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> getResults() {
			return results;
		}

		@Override
		public void run() {
			Vec searchPoint;
			try {
				while (true) {
					searchPoint = sourceQ.take();
					if (searchPoint.length() == 0) {
						break;
					}
					results = vc.search(searchPoint, range);
					resultQ.put(results);
				}
			} catch (final InterruptedException ex) {
				Logger.getLogger(MyDBSCAN.class.getName()).log(Level.SEVERE, null, ex);
			}

		}

	}

	/**
	 * 
	 * @param pointCats
	 *            the array to store the cluster assignments in
	 * @param dataSet
	 *            the data set
	 * @param point
	 *            the current data point we are working on
	 * @param clId
	 *            the current cluster we are working on
	 * @param eps
	 *            the search radius
	 * @param minPts
	 *            the minimum number of points to create a new cluster
	 * @param vc
	 *            the collection to use to search with
	 * @param threadpool
	 *            source of threads for computation
	 * @param resultQ
	 *            blocking queue used to get results from
	 * @param sourceQ
	 *            blocking queue used to store points that need to be processed
	 * @return true if a cluster was expanded, false if the point was marked as noise
	 */
	private boolean expandCluster(int[] pointCats, DataSet dataSet, int point, int clId, double eps, int minPts, VectorCollection<VecPaired<Vec, Integer>> vc,
			BlockingQueue<List<? extends VecPaired<VecPaired<Vec, Integer>, Double>>> resultQ, BlockingQueue<Vec> sourceQ) {
		final Vec queryPoint = dataSet.getDataPoint(point).getNumericalValues();
		final List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> seeds = vc.search(queryPoint, eps);

		if (seeds.size() < minPts)// no core point
		{
			pointCats[point] = NOISE;
			return false;
		}
		// Else, all points in seeds are density-reachable from Point

		List<? extends VecPaired<VecPaired<Vec, Integer>, Double>> results;

		try {
			pointCats[point] = clId;
			int out = seeds.size();
			for (final VecPaired<VecPaired<Vec, Integer>, Double> v : seeds) {
				sourceQ.put(v.getVector().getVector());
			}

			while (out > 0) {
				results = resultQ.take();
				out--;

				if (results.size() >= minPts) {
					for (final VecPaired<VecPaired<Vec, Integer>, Double> resultP : results) {
						final int resultPIndx = resultP.getVector().getPair();
						if (pointCats[resultPIndx] < 0)// is UNCLASSIFIED or NOISE
						{
							if (pointCats[resultPIndx] == UNCLASSIFIED) {
								sourceQ.put(resultP.getVector().getVector());
								out++;
							}
							pointCats[resultPIndx] = clId;
						}
					}
				}
			}
		} catch (final InterruptedException interruptedException) {
		}

		return true;
	}

	/**
	 * Gets the last(final) vector collection for beeing able to search neighbors for a point See {@link VectorCollection#search(Vec query, double range)},
	 * {@link VectorCollection#search(Vec query, int neighbors)}
	 * 
	 * @return the last vector collection
	 */
	public VectorCollection<VecPaired<Vec, Integer>> getLastVectorCollection() {
		return lastVectorCollection;
	}

}
