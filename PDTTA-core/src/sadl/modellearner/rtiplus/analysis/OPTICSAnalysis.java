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

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.OPTICS;
import jsat.linear.distancemetrics.DistanceMetric;

public class OPTICSAnalysis extends JsatClusteringAnalysis {

	public OPTICSAnalysis(DistanceMetric dstMetric, double clusterExpansionRate) {
		this(dstMetric, clusterExpansionRate, null, -1);
	}

	public OPTICSAnalysis(DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(dstMetric, clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
	}

	@Override
	List<List<DataPoint>> computeJsatClusters(DataSet<SimpleDataSet> ds) {

		// TODO OPTICS was not completely implemented yet in JSAT. Missing param xi
		final OPTICS x = new OPTICS(dm, 1);
		return x.cluster(ds);
	}

}
