package jsat.distributions.empirical;

import java.util.LinkedList;
import java.util.List;

import jsat.distributions.Distribution;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.math.Function;
import jsat.math.optimization.GoldenSearch;

public class KernelDensityEstimatorDifferentiable extends Distribution {

	protected KernelDensityEstimator kernelDensity;
	protected Function kernelPdfFunction;
	protected Function kernelDerivationFunction;

	protected double accuracy = DEFAULT_MINSEARCH_ACCURACY;

	public static final double DEFAULT_BANDWIDTH = 0.5d;
	public static final double DEFAULT_MINSEARCH_ACCURACY = 0.0001d;

	public KernelDensityEstimatorDifferentiable(double[] dataPoints) {
		this(new DenseVector(dataPoints));
	}

	public KernelDensityEstimatorDifferentiable(Vec dataPoints) {
		this(dataPoints, DEFAULT_BANDWIDTH);
	}

	public KernelDensityEstimatorDifferentiable(double[] dataPoints, double bandwidth) {
		this(new DenseVector(dataPoints), bandwidth);
	}

	public KernelDensityEstimatorDifferentiable(Vec dataPoints, double bandwidth) {

		kernelDensity = new KernelDensityEstimator(dataPoints, GaussKF.getInstance(), bandwidth);
		kernelPdfFunction = Distribution.getFunctionPDF(kernelDensity);
		kernelDerivationFunction = Distribution.getFunctionPDF(new KernelDensityEstimator(dataPoints, GaussKFDerivation.getInstance(), bandwidth));
	}

	public double prime(double x) {

		return kernelDerivationFunction.f(x);
	}

	/**
	 * @param accuracy
	 *            The accuracy of min-search in getMinima function.
	 */
	public void setAccuracy(double accuracy) {

		if (Double.isNaN(accuracy)) {
			throw new IllegalArgumentException();
		}
		this.accuracy = accuracy;
	}

	public Double[] getMinima() {

		final List<Double> pointList = new LinkedList<>();

		final double bandwidth = kernelDensity.getBandwith();

		double lastX = kernelDensity.min() + bandwidth;
		double lastValue = kernelDerivationFunction.f(lastX);

		final double endX = kernelDensity.max() - bandwidth;

		for (double x = lastX + bandwidth; x < endX; x = x + bandwidth) {
			final double newValue = kernelDerivationFunction.f(x);

			if (lastValue < 0 && newValue > 0) {
				pointList.add(GoldenSearch.minimize(accuracy, 100, lastX, x, 0, kernelPdfFunction, new double[1]));
			}

			lastX = x;
			lastValue = newValue;
		}

		return pointList.toArray(new Double[0]);
	}

	@Override
	public double pdf(double x) {

		return kernelDensity.pdf(x);
	}

	@Override
	public double cdf(double x) {

		return kernelDensity.cdf(x);
	}

	@Override
	public double invCdf(double p) {

		return kernelDensity.invCdf(p);
	}

	@Override
	public double min() {

		return kernelDensity.min();
	}

	@Override
	public double max() {

		return kernelDensity.max();
	}

	@Override
	public String getDistributionName() {

		return kernelDensity.getDescriptiveName();
	}

	@Override
	public String[] getVariables() {

		return kernelDensity.getVariables();
	}

	@Override
	public double[] getCurrentVariableValues() {

		return kernelDensity.getCurrentVariableValues();
	}

	@Override
	public void setVariable(String var, double value) {

		kernelDensity.setVariable(var, value);
	}

	@Override
	public Distribution clone() {

		return kernelDensity.clone();
	}

	@Override
	public void setUsingData(Vec data) {

		kernelDensity.setUsingData(data);
	}

	@Override
	public double mean() {

		return kernelDensity.mean();
	}

	@Override
	public double mode() {

		return kernelDensity.mode();
	}

	@Override
	public double variance() {

		return kernelDensity.variance();
	}

	@Override
	public double skewness() {

		return kernelDensity.skewness();
	}
}
