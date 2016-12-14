package sadl.modellearner.rtiplus.analysis;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TIntLinkedList;

public abstract class OutlierDistanceAnalysis implements DistributionAnalysis {

	private final double factor;

	public OutlierDistanceAnalysis(double strength) {

		if (strength <= 0.0) {
			throw new IllegalArgumentException("The strength must be positive!");
		}
		this.factor = 1.0 / strength;
	}

	@Override
	public TIntList analyzeDistribution(TIntList values, TIntList frequencies) {

		final TDoubleList distValues = new TDoubleArrayList(values.size() - 1);
		final TIntIterator it = values.iterator();
		if (it.hasNext()) {
			int prev = it.next();
			while (it.hasNext()) {
				final int curr = it.next();
				distValues.add(curr - prev - 1);
				prev = curr;
			}
		}

		final int outlierDist = getOutlierDistance(distValues);
		final int tolerance = (int) Math.rint(factor * outlierDist);

		final TIntList splits = new TIntLinkedList();

		final TIntIterator it2 = values.iterator();
		if (it2.hasNext()) {
			int t = it2.next();
			splits.add(t - tolerance - 1);
			while (it2.hasNext()) {
				final int t2 = it2.next();
				final int diff = t2 - t - 1;
				if (diff > 2 * tolerance) {
					splits.add(t + tolerance);
					splits.add(t2 - tolerance - 1);
				}
				t = t2;
			}
			splits.add(t + tolerance);
		}
		return splits;
	}

	abstract int getOutlierDistance(TDoubleList distValues);

}
