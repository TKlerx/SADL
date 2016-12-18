package sadl.modellearner.rtiplus;

import java.nio.file.Path;

import sadl.modellearner.rtiplus.SearchingPDRTALearner.SearchMeasure;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.modellearner.rtiplus.analysis.DistributionAnalysis;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;

public class PDRTALearnerBuilder {

	private final double significance;
	private final DistributionAnalysis histoBinDistriAnalysis;

	private Path directory = null;

	// Operation Tester
	private String bOp = "AAA";
	private OperationTester tester = new LikelihoodRatioTester(false);
	private SplitPosition splitPos = SplitPosition.MIDDLE;
	private boolean noMergeWithRoot = false;
	private boolean testPara = false;

	// IDA
	private DistributionAnalysis intervalDistriAna = null;
	private boolean remBorderGapsOnly = false;
	private boolean perfIDAActively = true;
	private double intervalExpRate = 0.05;

	// Searching RTI+
	private boolean searching = false;
	private int maxOpsToSearch = 10;
	private boolean searchPara = false;
	private SearchMeasure measure = SearchMeasure.SIZE;

	public PDRTALearnerBuilder(double significance, DistributionAnalysis histoBinDistriAnalysis) {

		this.significance = significance;
		this.histoBinDistriAnalysis = histoBinDistriAnalysis;
	}

	public PDRTALearnerBuilder drawPDRTAStepwise(Path dir) {

		this.directory = dir;
		return this;
	}

	public PDRTALearnerBuilder setOperationTester(OperationTester opTester) {

		this.tester = opTester;
		return this;
	}

	public PDRTALearnerBuilder setOperationTester(OperationTester opTester, SplitPosition splitPosition, boolean doNotMergeWithRoot, String boolOperators,
			boolean testParallel) {

		this.splitPos = splitPosition;
		this.noMergeWithRoot = doNotMergeWithRoot;
		this.bOp = boolOperators;
		this.testPara = testParallel;
		return setOperationTester(opTester);
	}

	public PDRTALearnerBuilder activateIDA(DistributionAnalysis intervalDistriAnalysis) {

		this.intervalDistriAna = intervalDistriAnalysis;
		return this;
	}

	public PDRTALearnerBuilder activateIDA(DistributionAnalysis intervalDistriAnalysis, boolean removeBorderGapsOnly, boolean performIDAActively,
			double intervalExpansionRate) {

		this.remBorderGapsOnly = removeBorderGapsOnly;
		this.perfIDAActively = performIDAActively;
		this.intervalExpRate = intervalExpansionRate;
		return activateIDA(intervalDistriAnalysis);
	}

	public PDRTALearnerBuilder useSearchingWrapper() {

		this.searching = true;
		return this;
	}

	public PDRTALearnerBuilder useSearchingWrapper(int maxOperationsToSearch, SearchMeasure searchMeasure, boolean searchParallel) {

		this.maxOpsToSearch = maxOperationsToSearch;
		this.measure = searchMeasure;
		this.searchPara = searchParallel;
		return useSearchingWrapper();
	}

	public SimplePDRTALearner build() {

		if (searching) {
			return new SearchingPDRTALearner(significance, histoBinDistriAnalysis, tester, splitPos, noMergeWithRoot, testPara, intervalDistriAna,
					remBorderGapsOnly, perfIDAActively, intervalExpRate, maxOpsToSearch, measure, searchPara, bOp, directory);
		} else {
			return new SimplePDRTALearner(significance, histoBinDistriAnalysis, tester, splitPos, noMergeWithRoot, testPara, intervalDistriAna,
					remBorderGapsOnly, perfIDAActively, intervalExpRate, bOp, directory);
		}
	}

}
