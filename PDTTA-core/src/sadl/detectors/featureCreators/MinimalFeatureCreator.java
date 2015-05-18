package sadl.detectors.featureCreators;

import gnu.trove.list.TDoubleList;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.PdttaDetector;

public class MinimalFeatureCreator implements FeatureCreator {


	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {
		return createFeatures(eventLikelihoods, timeLikelihoods, ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);
	}

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods, ProbabilityAggregationMethod aggType) {
		final double timeAgg = PdttaDetector.aggregate(timeLikelihoods, aggType);
		final double eventAgg = PdttaDetector.aggregate(eventLikelihoods, aggType);
		return new double[] { eventAgg, timeAgg };

	}

}
