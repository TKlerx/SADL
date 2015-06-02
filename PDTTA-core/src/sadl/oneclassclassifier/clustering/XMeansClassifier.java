/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.oneclassclassifier.clustering;

import java.util.List;

import sadl.constants.ScalingMethod;
import sadl.oneclassclassifier.NumericClassifier;

/**
 * 
 * @author Timo Klerx
 *
 */
public class XMeansClassifier extends NumericClassifier {

	public XMeansClassifier(ScalingMethod scalingMethod) {
		super(scalingMethod);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isOutlierScaled(double[] toEvaluate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void trainModelScaled(List<double[]> trainSamples) {
		// TODO Auto-generated method stub

	}


}
