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


package jsat.distributions.empirical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import jsat.distributions.ContinuousDistribution;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.UniformKF;
import jsat.linear.Vec;
import jsat.math.Function;
import jsat.math.OnLineStatistics;
import jsat.utils.ProbailityMatch;
/**
 * Kernel Density Estimator, KDE, uses the data set itself to approximate the underlying probability
 * distribution using {@link KernelFunction Kernel Functions}.
 * 
 * @author Edward Raff
 */
public class MyKernelDensityEstimator extends ContinuousDistribution
{
	private static final long serialVersionUID = -3928228439875019515L;
	/*
	 * README Implementation note: The values are stored in sorted order, which allows for fast evaluations. Instead of doing the full loop on each function
	 * call, O(n) time, we know the bounds on the values that will effect results, so we can do 2 binary searches and then a loop. Though this is still
	 * technically, O(n), its more accurately described as O(n * epsilon * log(n)) , where n * epsilon << n
	 */

	/**
	 * The various values
	 */
	private double[] X;
	/**
	 * Weights corresponding to each value. If all the same, weights should have a length of 0
	 */
	private double[] weights;
	/**
	 * For unweighted data, this is equal to X.length
	 */
	private double sumOFWeights;
	/**
	 * The bandwidth
	 */
	private double h;
	private double Xmean, Xvar, Xskew;

	private final KernelFunction k;

	public static double BandwithGuassEstimate(Vec X) {
		if (X.length() == 1) {
			return 1;
		} else if (X.standardDeviation() == 0) {
			return 1.06 * Math.pow(X.length(), -1.0 / 5.0);
		}

		return 1.06 * X.standardDeviation() * Math.pow(X.length(), -1.0 / 5.0);
	}

	/**
	 * Automatically selects a good Kernel function for the data set that balances Execution time and accuracy
	 * 
	 * @param dataPoints
	 * @return a kernel that will work well for the given distribution
	 */
	public static KernelFunction autoKernel(Vec dataPoints) {
		if (dataPoints.length() < 30) {
			return GaussKF.getInstance();
		} else if (dataPoints.length() < 1000) {
			return EpanechnikovKF.getInstance();
		} else {
			return UniformKF.getInstance();
		}
	}

	public MyKernelDensityEstimator(Vec dataPoints) {
		this(dataPoints, autoKernel(dataPoints));
	}

	public MyKernelDensityEstimator(Vec dataPoints, KernelFunction k) {
		this(dataPoints, k, BandwithGuassEstimate(dataPoints));
	}

	public MyKernelDensityEstimator(Vec dataPoints, KernelFunction k, double[] weights) {
		this(dataPoints, k, BandwithGuassEstimate(dataPoints), weights);
	}

	public MyKernelDensityEstimator(Vec dataPoints, KernelFunction k, double h) {
		setUpX(dataPoints);
		this.k = k;
		this.h = h;
	}

	public MyKernelDensityEstimator(Vec dataPoints, KernelFunction k, double h, double[] weights) {
		setUpX(dataPoints, weights);
		this.k = k;
		this.h = h;
	}

	/**
	 * Copy constructor
	 */
	private MyKernelDensityEstimator(double[] X, double h, double Xmean, double Xvar, double Xskew, KernelFunction k, double sumOfWeights, double[] weights) {
		this.X = Arrays.copyOf(X, X.length);
		this.h = h;
		this.Xmean = Xmean;
		this.Xvar = Xvar;
		this.Xskew = Xskew;
		this.k = k;
		this.sumOFWeights = sumOfWeights;
		this.weights = Arrays.copyOf(weights, weights.length);
	}

	private void setUpX(Vec S) {
		Xmean = S.mean();
		Xvar = S.variance();
		Xskew = S.skewness();
		X = S.arrayCopy();
		Arrays.parallelSort(X);
		sumOFWeights = X.length;
		weights = new double[0];
	}

	private void setUpX(Vec S, double[] weights) {
		if (S.length() != weights.length) {
			throw new RuntimeException("Weights and variables do not have the same length");
		}

		final OnLineStatistics stats = new OnLineStatistics();

		X = new double[S.length()];
		this.weights = Arrays.copyOf(weights, S.length());

		// Probability is the X value, match is the weights - so that they can be sorted together.
		final List<ProbailityMatch<Double>> sorter = new ArrayList<>(S.length());
		for (int i = 0; i < S.length(); i++) {
			sorter.add(new ProbailityMatch<>(S.get(i), weights[i]));
		}

		Collections.sort(sorter);
		for (int i = 0; i < sorter.size(); i++) {
			this.X[i] = sorter.get(i).getProbability();
			this.weights[i] = sorter.get(i).getMatch();
			stats.add(this.X[i], this.weights[i]);
		}
		// Now do some helpful preprocessing on weights. We will make index i store the sum for [0, i].
		// Each individual weight can still be retrieved in O(1) by accessing a 2nd index and a subtraction
		// Methods that need the sum can now access it in O(1) time from the weights array instead of doing an O(n) summations
		for (int i = 1; i < this.weights.length; i++) {
			this.weights[i] += this.weights[i - 1];
		}

		sumOFWeights = this.weights[this.weights.length - 1];
		this.Xmean = stats.getMean();
		this.Xvar = stats.getVarance();
		this.Xskew = stats.getSkewness();
	}

	private double getWeight(int i) {
		if (weights.length == 0) {
			return 1.0;
		} else if (i == 0) {
			return weights[i];
		} else {
			return weights[i] - weights[i - 1];
		}
	}

	@Override
	public double pdf(double x) {
		return pdf(x, -1);
	}

	/**
	 * Computes the Leave One Out PDF of the estimator
	 * 
	 * @param x
	 *            the value to get the pdf of
	 * @param j
	 *            the sorted index of the value to leave. If a negative value is given, the PDF with all values is returned
	 * @return the pdf with the given index left out
	 */
	private double pdf(double x, int j) {
		/*
		 * n ===== /x - x \ 1 \ | i| f(x) = --- > K|------| n h / \ h / ===== i = 1
		 */

		// Only values within a certain range will have an effect on the result, so we will skip to that range!
		int from = Arrays.binarySearch(X, x - h * k.cutOff());
		int to = Arrays.binarySearch(X, x + h * k.cutOff());
		// Mostly likely the exact value of x is not in the list, so it returns the inseration points
		from = from < 0 ? -from - 1 : from;
		to = to < 0 ? -to - 1 : to;

		// Univariate opt, if uniform weights, the sum is just the number of elements divide by half
		if (weights.length == 0 && k instanceof UniformKF) {
			return (to - from) * 0.5 / (sumOFWeights * h);
		}

		double sum = 0;
		for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
			if (i != j) {
				sum += k.k((x - X[i]) / h) * getWeight(i);
			}
		}

		return sum / (sumOFWeights * h);
	}

	@Override
	public double cdf(double x) {
		// Only values within a certain range will have an effect on the result, so we will skip to that range!
		int from = Arrays.binarySearch(X, x - h * k.cutOff());
		int to = Arrays.binarySearch(X, x + h * k.cutOff());
		// Mostly likely the exact value of x is not in the list, so it returns the inseration points
		from = from < 0 ? -from - 1 : from;
		to = to < 0 ? -to - 1 : to;

		double sum = 0;

		for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
			sum += k.intK((x - X[i]) / h) * getWeight(i);
		}

		/*
		 * Slightly different, all things below the from value for the cdf would be adding 1 to the value, as the value of x would be the integration over the
		 * entire range, which by definition, is equal to 1.
		 */
		// We perform the addition after the summation to reduce the difference size
		if (weights.length == 0) {
			sum += Math.max(0, from);
		} else {
			sum += weights[from];
		}

		return sum / (X.length);
	}

	@SuppressWarnings("unused")
	private final Function cdfFunc = new Function() {

		/**
		 * 
		 */
		private static final long serialVersionUID = -4100975560125048798L;

		@Override
		public double f(double... x) {
			return cdf(x[0]);
		}

		@Override
		public double f(Vec x) {
			return f(x.get(0));
		}
	};

	@Override
	public double invCdf(double p) {
		int index;
		double kd0;

		if (weights.length == 0) {
			final double r = p * X.length;
			index = (int) r;
			final double pd0 = r - index, pd1 = 1 - pd0;
			kd0 = k.intK(pd1);
		} else// CDF can be found from the weights summings
		{
			final double XEstimate = p * sumOFWeights;
			index = Arrays.binarySearch(weights, XEstimate);
			index = index < 0 ? -index - 1 : index;
			if (X[index] != 0) {
				kd0 = 1.0;// -Math.abs((XEstimate-X[index])/X[index]);
			} else {
				kd0 = 1.0;
			}
		}

		if (index == X.length - 1) {
			return X[index] * kd0;
		}

		final double x = X[index] * kd0 + X[index + 1] * (1 - kd0);

		return x;
	}

	@Override
	public double min() {
		return X[0] - h;
	}

	@Override
	public double max() {
		return X[X.length - 1] + h;
	}

	@Override
	public String getDistributionName() {
		return "Kernel Density Estimate";
	}

	@Override
	public String[] getVariables() {
		return new String[] { "h" };
	}

	@Override
	public double[] getCurrentVariableValues() {
		return new double[] { h };
	}

	/**
	 * Sets the bandwidth used for smoothing. Higher values make the pdf smoother, but can obscure features. Too small a bandwidth will causes spikes at only
	 * the data points.
	 * 
	 * @param val
	 *            new bandwidth
	 */
	public void setBandwith(double val) {
		if (val <= 0 || Double.isInfinite(val)) {
			throw new ArithmeticException("Bandwith parameter h must be greater than zero, not " + 0);
		}

		this.h = val;
	}

	/**
	 * 
	 * @return the bandwidth parameter
	 */
	public double getBandwith() {
		return h;
	}

	@Override
	public void setVariable(String var, double value) {
		if (var.equals("h")) {
			setBandwith(value);
		}
	}

	@Override
	public MyKernelDensityEstimator clone() {
		return new MyKernelDensityEstimator(X, h, Xmean, Xvar, Xskew, k, sumOFWeights, weights);
	}

	@Override
	public void setUsingData(Vec data) {
		setUpX(data);
		this.h = BandwithGuassEstimate(data);
	}

	@Override
	public double mean() {
		return Xmean;
	}

	@Override
	public double mode() {
		double maxP = 0, pTmp;
		double maxV = Double.NaN;
		for (int i = 0; i < X.length; i++) {
			if ((pTmp = pdf(X[i])) > maxP) {
				maxP = pTmp;
				maxV = X[i];
			}
		}

		return maxV;
	}

	@Override
	public double variance() {
		return Xvar + h * h * k.k2();
	}

	@Override
	public double skewness() {
		// TODO cant find anything about what this should really be...
		return Xskew;
	}

	@Override
	public String toString() {
		return "KernelDensityEstimator [size=" + X.length + ", sumOFWeights=" + sumOFWeights
				+ ", h=" + h + ", Xmean=" + Xmean + ", Xvar=" + Xvar
				+ ", Xskew=" + Xskew + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(X);
		long temp;
		temp = Double.doubleToLongBits(mean());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(getBandwith());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((k == null) ? 0 : k.hashCode());
		temp = Double.doubleToLongBits(sumOFWeights);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(weights);
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MyKernelDensityEstimator)) {
			return false;
		}
		final MyKernelDensityEstimator other = (MyKernelDensityEstimator) obj;
		if (!Precision.equals(Xmean, other.Xmean, 10000)) {
			return false;
		}
		if (!Precision.equals(Xskew, other.Xskew, 10000)) {
			return false;
		}
		if (!Precision.equals(Xvar, other.Xvar, 10000)) {
			return false;
		}
		if (!Precision.equals(h, other.h, 10000)) {
			return false;
		}
		if (!Precision.equals(sumOFWeights, other.sumOFWeights, 10000)) {
			return false;
		}
		if (k == null) {
			if (other.k != null) {
				return false;
			}
		} else if (k.getClass()!=other.k.getClass()) {
			return false;
		}
		if (!Arrays.equals(X, other.X)) {
			return false;
		}
		if (!Arrays.equals(weights, other.weights)) {
			return false;
		}
		return true;
	}


}
