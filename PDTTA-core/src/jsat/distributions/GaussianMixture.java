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
package jsat.distributions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.util.Pair;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.linear.Vec;

public class GaussianMixture extends ContinuousDistribution {

	private static final long serialVersionUID = -6518732005795117973L;
	MixtureMultivariateNormalDistribution mixture;

	public GaussianMixture(double[] means, double[] stdevs) {
		final List<Pair<Double, MultivariateNormalDistribution>> normals = new ArrayList<>(means.length);
		final Double weight = new Double(1d / means.length);
		for (int i = 0; i < means.length; i++) {
			final double[][] covMatrix = new double[][] { { stdevs[i] } };
			normals.add(Pair.create(weight, new MultivariateNormalDistribution(new double[] { means[i] }, covMatrix)));
		}
		mixture = new MixtureMultivariateNormalDistribution(normals);

	}

	@Override
	public double cdf(double x) {
		return super.cdf(x);
	}

	@Override
	public double[] sample(int numSamples, Random rand) {
		mixture.reseedRandomGenerator(rand.nextLong());
		final TDoubleList result = new TDoubleArrayList(numSamples);
		final double[][] samples = mixture.sample(numSamples);
		for (final double[] inner : samples) {
			for (final double d : inner) {
				result.add(d);
			}
		}
		return result.toArray();
	}

	@Override
	public double pdf(double x) {
		return mixture.density(new double[] { x });
	}

	@Override
	public String getDistributionName() {
		return "GaussianMixture";
	}

	@Override
	public String[] getVariables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getCurrentVariableValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariable(String var, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public ContinuousDistribution clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUsingData(Vec data) {
		// TODO Auto-generated method stub

	}

	@Override
	public double min() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double max() {
		return Double.POSITIVE_INFINITY;
	}

}
