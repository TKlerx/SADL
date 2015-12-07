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

import jsat.classifiers.DataPoint;
import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.GMeans;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.clustering.kmeans.KMeans;
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
public class GMeansClassifier extends KMeansBaseClustering {


	public GMeansClassifier(ScalingMethod scalingMethod, double distanceThreshold, int minClusterPoints, DistanceMethod distanceMethod) {
		super(scalingMethod, distanceMethod, distanceThreshold, minClusterPoints);
	}

	@Override
	protected Pair<List<Vec>, List<List<DataPoint>>> cluster(List<double[]> trainSamples) {
		final KMeans init = new HamerlyKMeans(getDistanceMetric(), SeedSelection.KPP, MasterSeed.nextRandom());
		final GMeans gMeans = new GMeans(init);
		gMeans.setStoreMeans(true);
		final List<List<DataPoint>> clusterResult = gMeans.cluster(DatasetTransformationUtils.doublesToDataSet(trainSamples));
		return new Pair<>(gMeans.getMeans(), clusterResult);
	}
}
