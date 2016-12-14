package sadl.modellearner.rtiplus.analysis;

import gnu.trove.list.TIntList;

public class FixedSplit implements DistributionAnalysis {

	private final TIntList fixedSplit;

	public FixedSplit(TIntList fixedSplit) {

		if (fixedSplit == null) {
			throw new IllegalArgumentException("The list of splits must not be null!");
		}
		this.fixedSplit = fixedSplit;
	}

	@Override
	public TIntList analyzeDistribution(TIntList values, TIntList frequencies) {
		return fixedSplit;
	}

}
