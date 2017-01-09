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
package sadl.modellearner.rtiplus.analysis;

import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.DBSCAN;
import jsat.linear.distancemetrics.DistanceMetric;
import jsat.linear.distancemetrics.EuclideanDistance;

public class DBSCANAnalysis extends JsatClusteringAnalysis {

	private final double eps;

	public DBSCANAnalysis(double eps, DistanceMetric dstMetric, double clusterExpansionRate) {
		this(eps, dstMetric, clusterExpansionRate, null, -1);
	}

	public DBSCANAnalysis(double eps, DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(dstMetric, clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
		this.eps = eps;
	}

	@Override
	List<List<DataPoint>> computeJsatClusters(final DataSet<SimpleDataSet> ds) {

		final DBSCAN d = new DBSCAN(dm);
		return d.cluster(ds, eps, 1);
	}

	public static void main(String[] args) {

		final DBSCANAnalysis a = new DBSCANAnalysis(0.05, new EuclideanDistance(), 0.0);

		final TIntList v = new TIntArrayList(new int[] { 1, 5, 6, 7, 8, 20, 21, 22, 23, 24, 50, 99 });
		final TIntList f = new TIntArrayList(new int[] { 1, 1, 1, 1, 1, 10, 10, 10, 10, 10, 10, 99 });

		// final List<TIntList> y = a.computeClusters(v, f);
		// System.out.println("Clusters: " + y.toString());

		final TIntList x = a.performAnalysis(v, f, 0, 100);
		System.out.println("Splits: " + x.toString());

	}

}
