package sadl.modellearner.rtiplus.analysis;

import java.util.List;

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

		final List<TIntList> clusters = computeClusters(values, frequencies);

		int sizeSum = 0;
		// Sort clusters ascending
		for (final TIntList cluster : clusters) {
			cluster.sort();
			sizeSum += cluster.size();
		}
		clusters.sort((c1, c2) -> Integer.compare(c1.get(0), c2.get(0)));

		if (sizeSum != values.size()) {
			throw new RuntimeException("Numer of element has be altered. Expected " + values.size() + ", but got " + sizeSum);
		}

		final TIntList splits = new TIntArrayList(2 * clusters.size());

		TIntList clu = clusters.get(0);
		int prevMin = clu.get(0);
		int prevMax = clu.get(clu.size() - 1);
		int prevTol = (int) Math.rint((prevMax - prevMin + 1) * clusterExpRate);
		splits.add(prevMin - prevTol - 1);
		for (int i = 1; i < clusters.size(); i++) {
			clu = clusters.get(i);
			final int min = clu.get(0);
			final int max = clu.get(clu.size() - 1);
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

	abstract List<TIntList> computeClusters(TIntList values, TIntList frequencies);

}
