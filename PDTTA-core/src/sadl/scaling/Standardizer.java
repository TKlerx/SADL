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
package sadl.scaling;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import jsat.math.OnLineStatistics;
import sadl.interfaces.Scaling;

public class Standardizer implements Scaling {
	private static final int ABNORMAL_STANDARD_DEVIATION = 0;
	double mus[];
	double sigmas[];
	@Override
	public void setFeatureCount(int length) {
		mus = new double[length];
		sigmas = new double[length];
	}

	boolean trained = false;
	@Override
	public List<double[]> train(List<double[]> input) {
		final OnLineStatistics[] os = new OnLineStatistics[mus.length];
		for (int i = 0; i < os.length; i++) {
			os[i] = new OnLineStatistics();
		}
		for (final double[] ds : input) {
			for (int i = 0; i < ds.length; i++) {
				os[i].add(ds[i]);
			}
		}
		for (int i = 0; i < os.length; i++) {
			mus[i] = os[i].getMean();
			sigmas[i] = os[i].getStandardDeviation();
			if (Double.isNaN(sigmas[i])) {
				sigmas[i] = ABNORMAL_STANDARD_DEVIATION;
			}
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
				if (Precision.equals(ABNORMAL_STANDARD_DEVIATION, sigmas[i])) {
					temp[i] = mus[i];
				} else {
					temp[i] = (ds[i] - mus[i]) / sigmas[i];
				}
			}
			result.add(temp);
		}
		return result;
	}

}
