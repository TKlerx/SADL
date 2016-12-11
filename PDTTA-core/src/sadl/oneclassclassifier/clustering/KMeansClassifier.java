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
package sadl.oneclassclassifier.clustering;

import java.util.List;

import jsat.classifiers.DataPoint;
import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.linear.Vec;
import jsat.utils.Pair;
import sadl.constants.DistanceMethod;
import sadl.constants.ScalingMethod;
import sadl.utils.DatasetTransformationUtils;
import sadl.utils.MasterSeed;

/**
 * 
 * @author Timo Klerx
 *
 */
public class KMeansClassifier extends KMeansBaseClustering {
	private final int k;

	public KMeansClassifier(ScalingMethod scalingMethod, int k, double distanceThreshold, int minClusterPoints, DistanceMethod distanceMethod) {
		super(scalingMethod, distanceMethod, distanceThreshold, minClusterPoints);
		this.k = k;
	}

	@Override
	protected Pair<List<Vec>, List<List<DataPoint>>> cluster(List<double[]> trainSamples) {
		final SeedSelection ss = SeedSelection.KPP;
		final HamerlyKMeans kMeans = new HamerlyKMeans(getDistanceMetric(), ss, MasterSeed.nextRandom());
		kMeans.setStoreMeans(true);
		final List<List<DataPoint>> clusterResult = kMeans.cluster(DatasetTransformationUtils.doublesToDataSet(trainSamples), k);
		return new Pair<>(kMeans.getMeans(), clusterResult);
	}

}
