/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.modellearner.rtiplus.analysis;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.linear.distancemetrics.DistanceMetric;
import sadl.utils.DatasetTransformationUtils;

public abstract class JsatClusteringAnalysis extends ClusteringAnalysis {

	final DistanceMetric dm;

	public JsatClusteringAnalysis(DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis,
			int fewElementsLimit) {
		super(clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
		this.dm = dstMetric;
	}

	@Override
	List<TDoubleList> computeClusters(List<double[]> data) {

		final DataSet<SimpleDataSet> ds = DatasetTransformationUtils.doublesToDataSet(data);

		final List<List<DataPoint>> c = computeJsatClusters(ds);

		final List<TDoubleList> clusters = new ArrayList<>(c.size());
		for (final List<DataPoint> clu : c) {
			if (clu.size() > 0) {
				final TDoubleList l = new TDoubleArrayList(clu.size());
				for (final DataPoint d : clu) {
					l.add(d.getNumericalValues().get(0));
				}
				clusters.add(l);
			}
		}

		return clusters;
	}

	abstract List<List<DataPoint>> computeJsatClusters(final DataSet<SimpleDataSet> ds);

}
