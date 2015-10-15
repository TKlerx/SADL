package sadl.detectors.featureCreators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.math.OnLineStatistics;
import sadl.constants.ProbabilityAggregationMethod;

public class UberFeatureCreator extends FullFeatureCreator {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(FullFeatureCreator.class);

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods, ProbabilityAggregationMethod aggType) {
		final TDoubleList result = new TDoubleArrayList(super.createFeatures(eventLikelihoods, timeLikelihoods, aggType));

		final OnLineStatistics eventStat = getStatistics(eventLikelihoods);
		final OnLineStatistics timeStat = getStatistics(timeLikelihoods);
		final TDoubleList eventDiffs = calcDiffs(eventLikelihoods);
		final TDoubleList timeDiffs = calcDiffs(timeLikelihoods);

		result.add(eventStat.getStandardDeviation());
		result.add(eventLikelihoods.size());
		result.add(eventDiffs.min());
		result.add(eventDiffs.max());

		result.add(timeStat.getStandardDeviation());
		result.add(timeLikelihoods.size());
		result.add(timeDiffs.min());
		result.add(timeDiffs.max());

		return result.toArray();
	}

	private TDoubleList calcDiffs(TDoubleList likelihoods) {
		if (likelihoods.size() <= 1) {
			return new TDoubleArrayList(new double[] { Double.POSITIVE_INFINITY });
		} else {
			final TDoubleList result = new TDoubleArrayList(likelihoods.size());
			double last = likelihoods.get(0);
			for (int i = 1; i < likelihoods.size(); i++) {
				final double temp = likelihoods.get(i);
				result.add(Math.abs(temp - last));
				last = temp;
			}
			return result;
		}
	}

	private OnLineStatistics getStatistics(TDoubleList timeLikelihoods) {
		final OnLineStatistics stat = new OnLineStatistics();
		for (int i = 0; i < timeLikelihoods.size(); i++) {
			final double d = timeLikelihoods.get(i);
			stat.add(d);
		}
		return stat;
	}
}
