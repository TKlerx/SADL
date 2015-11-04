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

	protected double minSearchAccuracy;
	protected double minSearchStep;

	public static final double DEFAULT_BANDWIDTH = 0.6d;
	public static final double DEFAULT_MIN_SEARCH_ACCURACY = 0.00001d;

	public KernelDensityEstimatorDifferentiable(double[] dataPoints) {
		this(new DenseVector(dataPoints), DEFAULT_BANDWIDTH, DEFAULT_BANDWIDTH / 4.0, DEFAULT_MIN_SEARCH_ACCURACY);
	}

	public KernelDensityEstimatorDifferentiable(Vec dataPoints) {
		this(dataPoints, DEFAULT_BANDWIDTH, DEFAULT_BANDWIDTH / 4.0, DEFAULT_MIN_SEARCH_ACCURACY);
	}

	public KernelDensityEstimatorDifferentiable(double[] dataPoints, double bandwidth) {
		this(new DenseVector(dataPoints), bandwidth, bandwidth / 4.0, DEFAULT_MIN_SEARCH_ACCURACY);
	}

	public KernelDensityEstimatorDifferentiable(Vec dataPoints, double bandwidth) {
		this(dataPoints, bandwidth, bandwidth / 4.0, DEFAULT_MIN_SEARCH_ACCURACY);
	}

	public KernelDensityEstimatorDifferentiable(double[] dataPoints, double bandwidth, double minSearchStep, double minSearchAccuracy) {
		this(new DenseVector(dataPoints), bandwidth, minSearchStep, minSearchAccuracy);
	}

	public KernelDensityEstimatorDifferentiable(Vec dataPoints, double bandwidth, double minSearchStep, double minSearchAccuracy) {

		kernelDensity = new KernelDensityEstimator(dataPoints, GaussKF.getInstance(), bandwidth);
		kernelPdfFunction = Distribution.getFunctionPDF(kernelDensity);
		kernelDerivationFunction = Distribution.getFunctionPDF(new KernelDensityEstimator(dataPoints, GaussKFDerivation.getInstance(), bandwidth));
		this.minSearchStep = minSearchStep;
		this.minSearchAccuracy = minSearchAccuracy;
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
		this.minSearchAccuracy = accuracy;
	}

	public Double[] getMinima() {

		final List<Double> pointList = new LinkedList<>();

		final double bandwidth = kernelDensity.getBandwith();

		double lastX = kernelDensity.min() + bandwidth;
		double lastValue = kernelDerivationFunction.f(lastX);

		final double endX = kernelDensity.max() - bandwidth;

		for (double x = lastX + minSearchStep; x < endX; x = x + minSearchStep) {
			final double newValue = kernelDerivationFunction.f(x);

			if (lastValue < 0 && newValue > 0) {
				pointList.add(GoldenSearch.minimize(minSearchAccuracy, 100, lastX, x, 0, kernelPdfFunction, new double[1]));
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
