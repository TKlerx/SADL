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

import jsat.distributions.empirical.kernelfunc.BiweightKF;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.TriweightKF;
import jsat.distributions.empirical.kernelfunc.UniformKF;
import sadl.constants.KdeKernelFunction;
import sadl.constants.MergeMethod;
import sadl.constants.MergeTest;
import sadl.constants.TauEstimation;
import sadl.interfaces.TauEstimator;
import sadl.modellearner.AlergiaRedBlue;
import sadl.modellearner.PdfaLearner;
import sadl.modellearner.PdttaLearner;
import sadl.run.factories.LearnerFactory;
import sadl.tau_estimation.IdentityEstimator;
import sadl.tau_estimation.MonteCarloEstimator;

@Parameters(commandDescription = "Run with PDTTALearner as a learner")
public class PdttaFactory implements LearnerFactory {

	@Parameter(names = "-mergeTest")
	MergeTest mergeTest = MergeTest.ALERGIA;

	@Parameter(names = "-mergeAlpha")
	private double mergeAlpha;

	@Parameter(names = "-recursiveMergeTest", arity = 1)
	private boolean recursiveMergeTest;

	@Parameter(names = "-smoothingPrior")
	double smoothingPrior = 0;

	@Parameter(names = "-mergeT0")
	int mergeT0 = 3;

	@Parameter(names = "-mergeMethod")
	MergeMethod mergeMethod = MergeMethod.ALERGIA_PAPER;

	@Parameter(names = "-kdeBandwidth")
	double kdeBandwidth;

	@Parameter(names = "-kdeBandwidthEstimate", arity = 1)
	boolean kdeBandwidthEstimate;

	@Parameter(names = "-kdeKernelFunction")
	KdeKernelFunction kdeKernelFunctionQualifier;
	KernelFunction kdeKernelFunction;

	@Parameter(names = "-tauEstimation")
	TauEstimation tauEstimation = TauEstimation.DENSITY;

	@Parameter(names = "-mcNumberOfSteps")
	int mcNumberOfSteps = 1000;

	@Parameter(names = "-mcPointsToStore")
	int mcPointsToStore = 10000;

	@Override
	public PdttaLearner create() {
		if (kdeKernelFunctionQualifier == KdeKernelFunction.BIWEIGHT) {
			kdeKernelFunction = BiweightKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.EPANECHNIKOV) {
			kdeKernelFunction = EpanechnikovKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.GAUSS) {
			kdeKernelFunction = GaussKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.TRIWEIGHT) {
			kdeKernelFunction = TriweightKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.UNIFORM) {
			kdeKernelFunction = UniformKF.getInstance();
		} else if (kdeKernelFunctionQualifier == KdeKernelFunction.ESTIMATE) {
			kdeKernelFunction = null;
		}
		TauEstimator tauEstimator;
		if (tauEstimation == TauEstimation.DENSITY) {
			tauEstimator = new IdentityEstimator();
		} else if (tauEstimation == TauEstimation.MONTE_CARLO) {
			tauEstimator = new MonteCarloEstimator(mcNumberOfSteps, mcPointsToStore);
		} else {
			tauEstimator = null;
		}
		if (kdeBandwidthEstimate) {
			kdeBandwidth = -1;
		}
		final PdfaLearner pdfaLearner = new AlergiaRedBlue(mergeAlpha, recursiveMergeTest, mergeMethod, mergeT0);
		final PdttaLearner learner = new PdttaLearner(pdfaLearner, kdeKernelFunction, kdeBandwidth, tauEstimator);
		return learner;
	}

}
