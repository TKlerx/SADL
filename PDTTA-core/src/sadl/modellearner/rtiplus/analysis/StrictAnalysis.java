package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TDoubleList;

public class StrictAnalysis extends OutlierDistanceAnalysis {

	public StrictAnalysis() {
		this(null, -1);
	}

	public StrictAnalysis(DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(0, fewElementsAnalysis, fewElementsLimit);
	}

	@Override
	int getOutlierDistance(TDoubleList distValues) {
		return 0;
	}

}
