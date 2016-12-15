package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class MADOutlierAnalysis extends OutlierDistanceAnalysis {

	public MADOutlierAnalysis(double strength) {
		this(strength, null, -1);
	}

	public MADOutlierAnalysis(double strength, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		final double median = StatisticsUtil.calculateMedian(distValues, true);
		final double mad = StatisticsUtil.calculateMAD(distValues, median);
		return (int) Math.ceil(((median + 2.5 * mad) / 2.0));
	}

}
