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

import jsat.distributions.empirical.kernelfunc.BiweightKF;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.TriweightKF;
import jsat.distributions.empirical.kernelfunc.UniformKF;
import sadl.constants.KdeKernelFunction;
import sadl.constants.TauEstimation;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.interfaces.TauEstimator;
import sadl.modellearner.TauPtaLearner;
import sadl.run.factories.LearnerFactory;
import sadl.tau_estimation.IdentityEstimator;
import sadl.tau_estimation.MonteCarloEstimator;

public interface TptaDefaultFactory extends LearnerFactory {


	default KernelFunction getKernelFunction() {
		KernelFunction kdeKernelFunction = null;
		if (getKdeFunctionQualifier() == KdeKernelFunction.BIWEIGHT) {
			kdeKernelFunction = BiweightKF.getInstance();
		} else if (getKdeFunctionQualifier() == KdeKernelFunction.EPANECHNIKOV) {
			kdeKernelFunction = EpanechnikovKF.getInstance();
		} else if (getKdeFunctionQualifier() == KdeKernelFunction.GAUSS) {
			kdeKernelFunction = GaussKF.getInstance();
		} else if (getKdeFunctionQualifier() == KdeKernelFunction.TRIWEIGHT) {
			kdeKernelFunction = TriweightKF.getInstance();
		} else if (getKdeFunctionQualifier() == KdeKernelFunction.UNIFORM) {
			kdeKernelFunction = UniformKF.getInstance();
		} else if (getKdeFunctionQualifier() == KdeKernelFunction.ESTIMATE) {
		}
		return kdeKernelFunction;
	}

	KdeKernelFunction getKdeFunctionQualifier();

	double getKdeBandwidthValue();

	boolean getKdeBandwidthEstimateValue();

	int getMcNumberOfSteps();

	int getMcPointsToStore();

	TauEstimation getTauEstimation();
	default double getBandwidth() {
		double newKdeBandwidth = getKdeBandwidthValue();
		if (getKdeBandwidthEstimateValue()) {
			newKdeBandwidth = -1;
		}
		return newKdeBandwidth;
	}

	default TauEstimator getTauEstimator() {
		TauEstimator tauEstimator;
		if (getTauEstimation() == TauEstimation.DENSITY) {
			tauEstimator = new IdentityEstimator();
		} else if (getTauEstimation() == TauEstimation.MONTE_CARLO) {
			tauEstimator = new MonteCarloEstimator(getMcNumberOfSteps(), getMcPointsToStore());
		} else {
			tauEstimator = null;
		}
		return tauEstimator;
	}

	@Override
	default public ProbabilisticModelLearner create() {
		return new TauPtaLearner(getKernelFunction(), getBandwidth(), getTauEstimator());
	}

}
