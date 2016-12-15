package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class IQROutlierAnalysis extends OutlierDistanceAnalysis {

	public IQROutlierAnalysis(double strength) {
		this(strength, null, -1);
	}

	public IQROutlierAnalysis(double strength, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		distValues.sort();
		final double q1 = StatisticsUtil.calculateQ1(distValues, false);
		final double q3 = StatisticsUtil.calculateQ3(distValues, false);
		return (int) Math.ceil(((q3 + (q3 - q1) * 1.5) / 2.0));
	}

}
