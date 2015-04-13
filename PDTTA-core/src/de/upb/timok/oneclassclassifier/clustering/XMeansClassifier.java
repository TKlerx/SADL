/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.upb.timok.oneclassclassifier.clustering;

import java.util.List;

import de.upb.timok.constants.ScalingMethod;
import de.upb.timok.oneclassclassifier.NumericClassifier;

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
