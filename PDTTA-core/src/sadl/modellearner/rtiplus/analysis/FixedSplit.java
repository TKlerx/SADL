package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TIntList;

public class FixedSplit extends DistributionAnalysis {

	private final TIntList fixedSplit;

	public FixedSplit(TIntList fixedSplit) {
		this(fixedSplit, null, -1);
	}

	public FixedSplit(TIntList fixedSplit, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(fewElementsAnalysis, fewElementsLimit);

		if (fixedSplit == null) {
			throw new IllegalArgumentException("The list of splits must not be null!");
		}
		this.fixedSplit = fixedSplit;
	}

	@Override
	TIntList analyzeDistribution(TIntList values, TIntList frequencies, int begin, int end) {
		return fixedSplit;
	}

}
