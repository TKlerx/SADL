/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.run.factories.learn;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sadl.constants.KdeKernelFunction;
import sadl.constants.MergeTest;
import sadl.constants.TauEstimation;
import sadl.modellearner.PdfaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.modellearner.TrebaPdfaLearner;

@Parameters(commandDescription = "Run with PDTTALearner as a learner")
public class PdttaFactory implements TptaDefaultFactory {
	@Parameter(names = "-kdeBandwidth")
	double kdeBandwidth = 10000;

	@Parameter(names = "-kdeBandwidthEstimate", arity = 1)
	boolean kdeBandwidthEstimate = true;

	@Parameter(names = "-kdeKernelFunction")
	KdeKernelFunction kdeKernelFunctionQualifier = KdeKernelFunction.ESTIMATE;

	@Parameter(names = "-mcNumberOfSteps")
	int mcNumberOfSteps = 1000;

	@Parameter(names = "-mcPointsToStore")
	int mcPointsToStore = 10000;

	@Parameter(names = "-tauEstimation")
	TauEstimation tauEstimation = TauEstimation.DENSITY;

	@Parameter(names = "-mergeAlpha")
	double mergeAlpha = 0.05;

	@Parameter(names = "-mergeTest")
	MergeTest mergeTest = MergeTest.MDI;



	@Override
	public boolean getKdeBandwidthEstimateValue() {
		return kdeBandwidthEstimate;
	}

	@Override
	public double getKdeBandwidthValue() {
		return kdeBandwidth;
	}

	@Override
	public KdeKernelFunction getKdeFunctionQualifier() {
		return kdeKernelFunctionQualifier;
	}

	@Override
	public int getMcNumberOfSteps() {
		return mcNumberOfSteps;
	}

	@Override
	public int getMcPointsToStore() {
		return mcPointsToStore;
	}

	@Override
	public TauEstimation getTauEstimation() {
		return tauEstimation;
	}

	public double getMergeAlpha() {
		return mergeAlpha;
	}

	public MergeTest getMergeTest() {
		return mergeTest;
	}




	@Override
	public PdttaLearner create() {
		final PdfaLearner pdfaLearner = new TrebaPdfaLearner(getMergeAlpha(), false, getMergeTest());
		final PdttaLearner learner = new PdttaLearner(pdfaLearner, getKernelFunction(), getBandwidth(), getTauEstimator());
		return learner;
	}
}
