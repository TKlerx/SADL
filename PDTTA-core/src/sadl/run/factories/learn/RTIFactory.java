/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.run.factories.learn;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.modellearner.rtiplus.PDRTALearnerBuilder;
import sadl.modellearner.rtiplus.SearchingPDRTALearner.SearchMeasure;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.modellearner.rtiplus.analysis.DistributionAnalysis;
import sadl.modellearner.rtiplus.analysis.FixedSplit;
import sadl.modellearner.rtiplus.analysis.FrequencyAnalysis;
import sadl.modellearner.rtiplus.analysis.IQROutlierAnalysis;
import sadl.modellearner.rtiplus.analysis.MADOutlierAnalysis;
import sadl.modellearner.rtiplus.analysis.MADOutlierAnalysis.MADConservatism;
import sadl.modellearner.rtiplus.analysis.QuantileAnalysis;
import sadl.modellearner.rtiplus.analysis.StrictAnalysis;
import sadl.modellearner.rtiplus.tester.FishersMethodTester;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;
import sadl.run.factories.LearnerFactory;

/**
 * 
 * @author Fabian Witter
 *
 */
@Parameters(commandDescription = "Run with RTI+ as a learner")
public class RTIFactory implements LearnerFactory {

	public enum OperationTesterType {

		LRT(new LikelihoodRatioTester(false)),
		LRT_ADV(new LikelihoodRatioTester(true)),
		NAIVE_LRT(new NaiveLikelihoodRatioTester()),
		FM(new FishersMethodTester(false)),
		FM_ADV(new FishersMethodTester(true));

		private final OperationTester tester;

		private OperationTesterType(OperationTester tester) {
			this.tester = tester;
		}

		public OperationTester getTester() {
			return this.tester;
		}
	}

	public enum DistributionAnalysisType {

		DISABLED(null),
		// FIXME Using strict analysis fails!
		// STRICT(new StrictAnalysis()),
		MAD(new MADOutlierAnalysis(1.0, MADConservatism.MODERATELY_CONSERVATIVE, new FrequencyAnalysis(10, 0.25), 2)),
		IQR(new IQROutlierAnalysis(1.0, false, new FrequencyAnalysis(10, 0.25), 2)),
		FREQUENCY(new FrequencyAnalysis(10, 0.25)),
		QUANTILE(new QuantileAnalysis(4)),
		FIXED(new FixedSplit(new TIntArrayList(new int[] { 1, 42, 1337 })));

		private final DistributionAnalysis ana;

		private DistributionAnalysisType(DistributionAnalysis ana) {
			this.ana = ana;
		}

		public DistributionAnalysis getAnalysis() {
			return this.ana;
		}
	}

	@Parameter(names = "-sig", required = true, arity = 1)
	double sig;

	@Parameter(names = "-hist", required = true, arity = 1)
	DistributionAnalysisType hist;
	@Parameter(names = "-fewHist", arity = 1)
	DistributionAnalysisType fewHist = DistributionAnalysisType.DISABLED;
	@Parameter(names = "-fewHistLimit", arity = 1)
	int fewHistLimit = -1;

	@Parameter(names = "-em", arity = 1)
	OperationTesterType tester = OperationTesterType.LRT;
	@Parameter(names = "-sp", arity = 1)
	SplitPosition splitPos = SplitPosition.MIDDLE;
	@Parameter(names = "-noMergeRoot", arity = 1)
	boolean doNotMergeWithRoot = false;
	@Parameter(names = "-bop", arity = 1)
	String boolOps = "AAA";
	@Parameter(names = "-testPara", arity = 1)
	boolean testParallel = false;

	@Parameter(names = "-ida", arity = 1)
	DistributionAnalysisType ida = DistributionAnalysisType.DISABLED;
	@Parameter(names = "-fewIda", arity = 1)
	DistributionAnalysisType fewIda = DistributionAnalysisType.DISABLED;
	@Parameter(names = "-fewIdaLimit", arity = 1)
	int fewIdaLimit = -1;
	@Parameter(names = "-remBorderOnly", arity = 1)
	boolean removeBorderGapsOnly = false;
	@Parameter(names = "-idaActively", arity = 1)
	boolean performIDAActively = true;
	@Parameter(names = "-intervalExpRate", arity = 1)
	double intervalExpRate = 0.1;

	@Parameter(names = "-search", arity = 1)
	boolean searching = false;
	@Parameter(names = "-maxOpsToSearch", arity = 1)
	int maxOperationsToSearch = 10;
	@Parameter(names = "-searchMeasure", arity = 1)
	SearchMeasure measure = SearchMeasure.SIZE;
	@Parameter(names = "-searchPara", arity = 1)
	boolean searchParallel = false;

	@Parameter(names = "-steps", arity = 1)
	String stepsDir = null;

	@Override
	public ProbabilisticModelLearner create() {

		final DistributionAnalysis histoBinAna = createDistributionAnalysis(hist, false, false);
		final DistributionAnalysis intervalAna = createDistributionAnalysis(ida, false, true);

		final PDRTALearnerBuilder b = new PDRTALearnerBuilder(sig, histoBinAna);
		b.setOperationTester(tester != null ? tester.getTester() : null, splitPos, doNotMergeWithRoot, boolOps, testParallel);

		if (intervalAna != null) {
			b.activateIDA(intervalAna, removeBorderGapsOnly, performIDAActively, intervalExpRate);
		}
		if (searching) {
			b.useSearchingWrapper(maxOperationsToSearch, measure, searchParallel);
		}
		if (stepsDir != null) {
			b.drawPDRTAStepwise(Paths.get(stepsDir));
		}
		return b.build();
	}

	private DistributionAnalysis createDistributionAnalysis(DistributionAnalysisType type, boolean fewData, boolean isIda) {

		switch (type) {
			case DISABLED:
				return null;
				// FIXME @fwitter
				// Use again if strict bug fixed
				// case STRICT:
				// return fewData ? fewDataStrictAnalysisParams.createAnalysis() : strictAnalysisParams.createAnalysis(isIda);
			case FREQUENCY:
				return fewData ? fewDataFrequencyAnalysisParams.createAnalysis() : frequencyAnalysisParams.createAnalysis(isIda);
			case IQR:
				return fewData ? fewDataIqrAnalysisParams.createAnalysis() : iqrAnalysisParams.createAnalysis(isIda);
			case MAD:
				return fewData ? fewDataMadAnalysisParams.createAnalysis() : madAnalysisParams.createAnalysis(isIda);
			case FIXED:
				return fewData ? fewDataFixedSplitParams.createAnalysis() : fixedSplitParams.createAnalysis(isIda);
			case QUANTILE:
				return fewData ? fewDataQuantileAnalysisParams.createAnalysis() : quantileAnalysisParams.createAnalysis(isIda);
			default:
				throw new IllegalArgumentException("Nonexistent type used!");
		}
	}

	@ParametersDelegate
	private final StrictAnalysisParams strictAnalysisParams = new StrictAnalysisParams();
	@ParametersDelegate
	private final FewDataStrictAnalysisParams fewDataStrictAnalysisParams = new FewDataStrictAnalysisParams();

	class StrictAnalysisParams {

		DistributionAnalysis createAnalysis(boolean isIda) {
			return new StrictAnalysis(createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda), isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataStrictAnalysisParams {

		DistributionAnalysis createAnalysis() {
			return new StrictAnalysis();

		}
	}

	@ParametersDelegate
	private final FrequencyAnalysisParams frequencyAnalysisParams = new FrequencyAnalysisParams();
	@ParametersDelegate
	private final FewDataFrequencyAnalysisParams fewDataFrequencyAnalysisParams = new FewDataFrequencyAnalysisParams();

	class FrequencyAnalysisParams {

		@Parameter(names = "-trustedFreq")
		int trustedFrequency = 10;

		@Parameter(names = "-rangeRatio")
		double rangeRatio = 0.2;

		DistributionAnalysis createAnalysis(boolean isIda) {
			return new FrequencyAnalysis(trustedFrequency, rangeRatio, createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda),
					isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataFrequencyAnalysisParams {

		@Parameter(names = "-fewTrustedFreq")
		int fewTrustedFrequency = 10;

		@Parameter(names = "-fewRangeRatio")
		double fewRangeRatio = 0.2;

		DistributionAnalysis createAnalysis() {
			return new FrequencyAnalysis(fewTrustedFrequency, fewRangeRatio);

		}
	}

	@ParametersDelegate
	private final IQRAnalysisParams iqrAnalysisParams = new IQRAnalysisParams();
	@ParametersDelegate
	private final FewDataIQRAnalysisParams fewDataIqrAnalysisParams = new FewDataIQRAnalysisParams();

	class IQRAnalysisParams {

		@Parameter(names = "-iqrStrength")
		double strength = 1.0;

		@Parameter(names = "-iqrOnlyFarOuts")
		boolean onlyFarOuts = false;

		DistributionAnalysis createAnalysis(boolean isIda) {
			return new IQROutlierAnalysis(strength, onlyFarOuts, createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda),
					isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataIQRAnalysisParams {

		@Parameter(names = "-fewIqrStrength")
		double fewStrength = 1.0;

		@Parameter(names = "-fewIqrOnlyFarOuts")
		boolean onlyFarOuts = false;

		DistributionAnalysis createAnalysis() {
			return new IQROutlierAnalysis(fewStrength, onlyFarOuts);

		}
	}

	@ParametersDelegate
	private final MADAnalysisParams madAnalysisParams = new MADAnalysisParams();
	@ParametersDelegate
	private final FewDataMADAnalysisParams fewDataMadAnalysisParams = new FewDataMADAnalysisParams();

	class MADAnalysisParams {

		@Parameter(names = "-madStrength")
		double strength = 1.0;

		@Parameter(names = "-madConservatism")
		MADConservatism conservatism = MADConservatism.MODERATELY_CONSERVATIVE;

		DistributionAnalysis createAnalysis(boolean isIda) {
			return new MADOutlierAnalysis(strength, conservatism, createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda),
					isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataMADAnalysisParams {

		@Parameter(names = "-fewMadStrength")
		double fewStrength = 1.0;

		@Parameter(names = "-fewMadConservatism")
		MADConservatism conservatism = MADConservatism.MODERATELY_CONSERVATIVE;

		DistributionAnalysis createAnalysis() {
			return new MADOutlierAnalysis(fewStrength, conservatism);

		}
	}

	@ParametersDelegate
	private final FixedSplitParams fixedSplitParams = new FixedSplitParams();
	@ParametersDelegate
	private final FewDataFixedSplitParams fewDataFixedSplitParams = new FewDataFixedSplitParams();

	class FixedSplitParams {

		@Parameter(names = "-splitVals", variableArity = true)
		List<String> splits = new LinkedList<>();

		DistributionAnalysis createAnalysis(boolean isIda) {
			final TIntList l = new TIntArrayList(splits.size());
			splits.stream().map(s -> new Integer(Integer.parseInt(s))).sorted().forEachOrdered(v -> l.add(v.intValue()));
			return new FixedSplit(l, createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda), isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataFixedSplitParams {

		@Parameter(names = "-fewSplitVals", variableArity = true)
		List<String> splits = new LinkedList<>();

		DistributionAnalysis createAnalysis() {
			final TIntList l = new TIntArrayList(splits.size());
			splits.stream().map(s -> new Integer(Integer.parseInt(s))).sorted().forEachOrdered(v -> l.add(v.intValue()));
			return new FixedSplit(l);

		}
	}

	@ParametersDelegate
	private final QuantileAnalysisParams quantileAnalysisParams = new QuantileAnalysisParams();
	@ParametersDelegate
	private final FewDataQuantileAnalysisParams fewDataQuantileAnalysisParams = new FewDataQuantileAnalysisParams();

	class QuantileAnalysisParams {

		@Parameter(names = "-numQuantiles")
		int numQuantiles = 4;

		DistributionAnalysis createAnalysis(boolean isIda) {
			return new QuantileAnalysis(numQuantiles, createDistributionAnalysis(isIda ? fewIda : fewHist, true, isIda), isIda ? fewIdaLimit : fewHistLimit);
		}
	}

	class FewDataQuantileAnalysisParams {

		@Parameter(names = "-fewNumQuantiles")
		int numQuantiles = 4;

		DistributionAnalysis createAnalysis() {
			return new QuantileAnalysis(numQuantiles);

		}
	}

}
