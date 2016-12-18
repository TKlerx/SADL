package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TDoubleList;
import sadl.modellearner.rtiplus.StatisticsUtil;

public class MADOutlierAnalysis extends OutlierDistanceAnalysis {

	public enum MADConservatism {
		VERY_CONSERVATIVE(3.0), MODERATELY_CONSERVATIVE(2.5), POORLY_CONSERVATIVE(2.0);

		private final double coeff;

		private MADConservatism(double coeff) {
			this.coeff = coeff;
		}

		public double getCoefficent() {
			return this.coeff;
		}
	}

	private final MADConservatism coeff;

	public MADOutlierAnalysis(double strength, MADConservatism coeff) {
		this(strength, coeff, null, -1);
	}

	public MADOutlierAnalysis(double strength, MADConservatism coeff, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(strength, fewElementsAnalysis, fewElementsLimit);
		this.coeff = coeff;
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {

		final double median = StatisticsUtil.calculateMedian(distValues, true);
		final double mad = StatisticsUtil.calculateMAD(distValues, median);
		return (int) Math.ceil(((median + coeff.getCoefficent() * mad) / 2.0));
	}

}
