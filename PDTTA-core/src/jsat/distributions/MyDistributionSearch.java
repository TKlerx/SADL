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

import java.util.Arrays;

import jsat.distributions.empirical.MyKernelDensityEstimator;
import jsat.linear.Vec;
import jsat.testing.goodnessoffit.KSTest;
import jsat.utils.Pair;

import org.apache.commons.math3.util.Precision;

/**
 * Provides methods for selecting the distribution that best fits a given data set.
 * 
 * @author Edward Raff
 * @author Timo Klerx
 */
public class MyDistributionSearch extends DistributionSearch
{
	private static ContinuousDistribution[] possibleDistributionSet = new ContinuousDistribution[]
			{
		new Normal(),
		new LogNormal(), new Exponential(),
		new Gamma(2, 1), new FisherSendor(10, 10), new Weibull(2, 1),
		new Uniform(0, 1), new Logistic(3, 2), new MaxwellBoltzmann(),
		new Pareto(), new Rayleigh(2)
			};

	/**
	 * Searches the distributions that are known for a possible fit, and returns
	 * what appears to be the best fit.
	 * 
	 * @param v all the values from a sample
	 * @return the distribution that provides the best fit to the data that this method could find.
	 */
	public static ContinuousDistribution getBestDistribution(Vec v)
	{
		return getBestDistribution(v, possibleDistributionSet);
	}

	/**
	 * Searches the distributions that are known for a possible fit, and returns
	 * what appears to be the best fit. If no suitable fit can be found, a
	 * {@link MyKernelDensityEstimator} is fit to the data.
	 * 
	 * @param v all the values from a sample
	 * @param KDECutOff the cut off value used for using the KDE. Should be in
	 * the range (0, 1). Values less than zero means the KDE will never be used,
	 * and greater then 1 means the KDE will always be used.
	 * @return the distribution that provides the best fit to the data that this method could find.
	 */
	public static ContinuousDistribution getBestDistribution(Vec v, double KDECutOff)
	{
		return getBestDistribution(v, KDECutOff, possibleDistributionSet);
	}

	/**
	 * Searches the distributions that are given for a possible fit, and returns
	 * what appears to be the best fit.
	 * 
	 * @param v all the values from a sample
	 * @param possibleDistributions the array of distribution to try and fit to the data
	 * @return the distribution that provides the best fit to the data that this method could find.
	 */
	public static ContinuousDistribution getBestDistribution(Vec v, ContinuousDistribution... possibleDistributions)
	{
		return getBestDistribution(v, 0.0, possibleDistributions);
	}

	/**
	 * Searches the distributions that are given for a possible fit, and returns
	 * what appears to be the best fit. If no suitable fit can be found, a
	 * {@link MyKernelDensityEstimator} is fit to the data.
	 * 
	 * @param v all the values from a sample
	 * @param KDECutOff the cut off value used for using the KDE. Should be in
	 * the range (0, 1). Values less than zero means the KDE will never be used,
	 * and greater then 1 means the KDE will always be used.
	 * @param possibleDistributions the array of distribution to try and fit to the data
	 * @return  the distribution that provides the best fit to the data that this method could find.
	 */
	public static ContinuousDistribution getBestDistribution(Vec v, double KDECutOff, ContinuousDistribution... possibleDistributions)
	{
		if(v.length() == 0) {
			throw new ArithmeticException("Can not fit a distribution to an empty set");
		}
		final Pair<Boolean, Double> result = checkForDifferentValues(v);
		if(result.getFirstItem()){
			return new SingleValueDistribution(result.getSecondItem());
		}
		//Thread Safety, clone the possible distributions

		final ContinuousDistribution[] possDistCopy = new ContinuousDistribution[possibleDistributions.length];

		for(int i = 0; i < possibleDistributions.length; i++) {
			possDistCopy[i] = possibleDistributions[i].clone();
		}


		final KSTest ksTest = new KSTest(v);

		ContinuousDistribution bestDist = null;
		double bestProb = 0;

		for (final ContinuousDistribution cd : possDistCopy)
		{
			try
			{
				cd.setUsingData(v);
				final double prob = ksTest.testDist(cd);

				if(prob > bestProb)
				{
					bestDist = cd;
					bestProb = prob;
				}

			}
			catch(final Exception ex)
			{

			}
		}

		///Return the best distribution, or if somehow everythign went wrong, a normal distribution
		try
		{
			if(bestProb >= KDECutOff) {
				return bestDist == null ? new Normal(v.mean(), v.standardDeviation()) : bestDist.clone();
			} else {
				return new MyKernelDensityEstimator(v);
			}
		}
		catch (final RuntimeException ex)//Mostly likely occurs if all values are all zero
		{
			if(v.standardDeviation() == 0) {
				return null;
			}
			throw new ArithmeticException("Catistrophic faulure getting a distribution");
		}
	}
	/**
	 * True iff there are only identical values in the vector
	 * @param v
	 */
	public static Pair<Boolean, Double> checkForDifferentValues(Vec v) {
		final double value = v.get(0);
		for(int i = 1;i<v.length();i++){
			if (!Precision.equals(v.get(i), value)) {
				return new Pair<>(false, -1.0);
			}
		}
		return new Pair<>(true, value);
	}

	/**
	 * search for all possible distributions and maybe also for a KDE. Does not compare bestProb to cutoff
	 * @param v
	 * @param includeKDE
	 */
	public static Distribution getBestDistribution(Vec v, boolean includeKDE) {
		if(!includeKDE){
			return getBestDistribution(v);
		}else{
			final ContinuousDistribution[] possibleDists = Arrays.copyOf(possibleDistributionSet, possibleDistributionSet.length + 1);
			possibleDists[possibleDists.length-1] = new MyKernelDensityEstimator(v);
			return getBestDistribution(v,possibleDists);
		}
	}
}
