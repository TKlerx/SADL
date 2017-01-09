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

import java.util.ArrayList;
import java.util.List;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public abstract class ClusteringAnalysis extends DistributionAnalysis {

	private final double clusterExpRate;

	public ClusteringAnalysis(double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(fewElementsAnalysis, fewElementsLimit);

		if (clusterExpansionRate < 0.0) {
			throw new IllegalArgumentException("Expansion rate for clusters must be 0 at least");
		}

		this.clusterExpRate = clusterExpansionRate;
	}

	@Override
	TIntList analyzeDistribution(TIntList values, TIntList frequencies, int begin, int end) {

		int valMin = Integer.MAX_VALUE;
		int valMax = Integer.MIN_VALUE;
		final TIntIterator it = values.iterator();
		while (it.hasNext()) {
			final int v = it.next();
			if (v < valMin) {
				valMin = v;
			}
			if (v > valMax) {
				valMax = v;
			}
		}

		int freqMin = Integer.MAX_VALUE;
		int freqMax = Integer.MIN_VALUE;
		final TIntIterator it2 = values.iterator();
		while (it2.hasNext()) {
			final int v = it2.next();
			if (v < freqMin) {
				freqMin = v;
			}
			if (v > freqMax) {
				freqMax = v;
			}
		}

		// To normalized array
		final List<double[]> data = new ArrayList<>(values.size());
		for (int i = 0; i < values.size(); i++) {
			final double normVal = normalize(values.get(i), valMin, valMax);
			final double normFreq = normalize(frequencies.get(i), freqMin, freqMax);
			data.add(new double[] { normVal, normFreq });
		}

		// Cluster data
		final List<TDoubleList> clusters = computeClusters(data);

		int sizeSum = 0;
		// Sort clusters ascending
		for (final TDoubleList cluster : clusters) {
			cluster.sort();
			sizeSum += cluster.size();
		}
		clusters.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		if (sizeSum != values.size()) {
			throw new RuntimeException("Numer of elements has been altered. Expected " + values.size() + ", but got " + sizeSum);
		}

		// Denormalize
		final List<int[]> orgClusters = new ArrayList<>(clusters.size());
		for (final TDoubleList clu : clusters) {
			final double first = clu.get(0);
			final double last = clu.get(clu.size() - 1);
			orgClusters.add(new int[] { denormalize(first, valMin, valMax), denormalize(last, valMin, valMax) });
		}

		final TIntList splits = new TIntArrayList(2 * orgClusters.size());

		int[] clu = orgClusters.get(0);
		int prevMin = clu[0];
		int prevMax = clu[1];
		int prevTol = (int) Math.rint((prevMax - prevMin + 1) * clusterExpRate);
		splits.add(prevMin - prevTol - 1);
		for (int i = 1; i < orgClusters.size(); i++) {
			clu = orgClusters.get(i);
			final int min = clu[0];
			final int max = clu[1];
			final int tol = (int) Math.rint((max - min + 1) * clusterExpRate);
			if ((prevTol + tol) < (min - prevMax - 1)) {
				splits.add(prevMax + prevTol);
				splits.add(min - tol - 1);
			}
			prevMin = min;
			prevMax = max;
			prevTol = tol;
		}
		splits.add(prevMax + prevTol);

		return splits;
	}

	private static double normalize(int v, int min, int max) {
		return (v - min) / (double) (max - min);
	}

	private static int denormalize(double v, int min, int max) {
		return (int) Math.rint(v * (max - min) + min);
	}

	abstract List<TDoubleList> computeClusters(List<double[]> data);

}
