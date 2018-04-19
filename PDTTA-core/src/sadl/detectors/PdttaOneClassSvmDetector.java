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
package sadl.detectors;

import sadl.constants.ProbabilityAggregationMethod;
import sadl.constants.ScalingMethod;
import sadl.detectors.featureCreators.FeatureCreator;
import sadl.interfaces.TrainableDetector;
import sadl.oneclassclassifier.LibSvmClassifier;

/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public class PdttaOneClassSvmDetector extends VectorDetector implements TrainableDetector {
	/**
	 * Use the PdttaVectorDetector constructor directly instead
	 * 
	 * @param aggType
	 * @param fc
	 * @param useProbability
	 * @param gamma
	 * @param nu
	 * @param kernelType
	 * @param eps
	 * @param degree
	 * @param scalingMethod
	 */
	public PdttaOneClassSvmDetector(ProbabilityAggregationMethod aggType, FeatureCreator fc, int useProbability, double gamma, double nu,
			int kernelType, double eps, int degree, ScalingMethod scalingMethod) {
		super(aggType, fc, new LibSvmClassifier(useProbability, gamma, nu, kernelType, eps, degree, scalingMethod));
	}

}
