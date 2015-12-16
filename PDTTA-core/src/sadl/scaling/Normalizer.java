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

package sadl.scaling;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import sadl.interfaces.Scaling;

/**
 * 
 * @author Timo Klerx
 *
 */
public class Normalizer implements Scaling {
	double mins[];
	double maxs[];
	double scalingFactors[];

	public Normalizer() {

	}

	@Override
	public void setFeatureCount(int featureCount) {
		mins = new double[featureCount];
		maxs = new double[featureCount];
		for (int i = 0; i < featureCount; i++) {
			mins[i] = Double.MAX_VALUE;
			maxs[i] = Double.MIN_VALUE;
		}
	}

	public Normalizer(int featureCount) {
		setFeatureCount(featureCount);
	}

	boolean trained = false;

	@Override
	public List<double[]> train(List<double[]> input) {
		for (final double[] ds : input) {
			for (int i = 0; i < ds.length; i++) {
				if (Double.isNaN(ds[i])) {
					throw new IllegalStateException(
							"NaN is not allowed as an input value for scaling at with list index=" + input.indexOf(ds) + " and array index=" + i);
				}
				mins[i] = Math.min(mins[i], ds[i]);
				maxs[i] = Math.max(maxs[i], ds[i]);
			}
		}
		scalingFactors = new double[mins.length];
		for (int i = 0; i < mins.length; i++) {
			scalingFactors[i] = maxs[i] - mins[i];
		}
		trained = true;
		return scale(input);
	}

	@Override
	public List<double[]> scale(List<double[]> input) {
		if (!trained) {
			throw new IllegalStateException("Scaler must be trained first before scaling");
		}
		final List<double[]> result = new ArrayList<>(input.size());
		for (final double[] ds : input) {
			final double[] temp = new double[ds.length];
			for (int i = 0; i < ds.length; i++) {
				if (Precision.equals(scalingFactors[i], 0)) {
					temp[i] = 1;
				} else {
					temp[i] = (ds[i] - mins[i]) / scalingFactors[i];
				}
			}
			result.add(temp);
		}
		return result;
	}

}
