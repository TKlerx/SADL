package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class IQROutlierAnalysis extends OutlierDistanceAnalysis {

	private final boolean onlyFarOuts;

	public IQROutlierAnalysis(double strength, boolean onlyFarOuts) {
		this(strength, onlyFarOuts, null, -1);
	}

	public IQROutlierAnalysis(double strength, boolean onlyFarOuts, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
		this.onlyFarOuts = onlyFarOuts;
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		distValues.sort();
		final double q1 = StatisticsUtil.calculateQ1(distValues, false);
		final double q3 = StatisticsUtil.calculateQ3(distValues, false);
		final double coeff = onlyFarOuts ? 3.0 : 1.5;
		return (int) Math.ceil(((q3 + (q3 - q1) * coeff) / 2.0));
	}

}
