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
package de.upb.timok.utils;

import java.util.ArrayList;
import java.util.List;

public class Normalizer {
	double mins[];
	double maxs[];
	double scalingFactors[];

	public Normalizer(int featureCount) {
		mins = new double[featureCount];
		maxs = new double[featureCount];
		for (int i = 0; i < featureCount; i++) {
			mins[i] = Double.MAX_VALUE;
			maxs[i] = Double.MIN_VALUE;
		}
	}

	public List<double[]> train(List<double[]> input) {
		for (double[] ds : input) {
			for (int i = 0; i < ds.length; i++) {
				mins[i] = Math.min(mins[i], ds[i]);
				maxs[i] = Math.max(maxs[i], ds[i]);
			}
		}
		scalingFactors = new double[mins.length];
		for (int i = 0; i < mins.length; i++) {
			scalingFactors[i] = maxs[i] - mins[i];
		}
		return normalize(input);
	}

	public List<double[]> normalize(List<double[]> input) {
		List<double[]> result = new ArrayList<>(input.size());
		for (double[] ds : input) {
			double[] temp = new double[ds.length];
			for (int i = 0; i < ds.length; i++) {
				temp[i] = (ds[i] - mins[i]) / scalingFactors[i];
			}
			result.add(temp);
		}
		return result;
	}

}
