package jsat.distributions.empirical;
import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.Random;

import jsat.distributions.Normal;
import jsat.distributions.empirical.kernelfunc.KernelFunction;

/**
 *
 * @author Edward Raff
 */
public class GaussKFInvertible implements KernelFunction {

	private static final long serialVersionUID = -6765390012694573184L;

	private GaussKFInvertible() {
	}

	private static class SingletonHolder {

		public static final GaussKFInvertible INSTANCE = new GaussKFInvertible();
	}

	public static double InvertedIntGaussKF(double p, double invertedKFAccuracy) {

		final GaussKFInvertible normalFunc = getInstance();
		double start = -normalFunc.cutOff();
		double end = normalFunc.cutOff();

		do {
			final double between = (start + end) / 2.0;
			if (normalFunc.intK(between) > p) {
				end = between;
			} else {
				start = between;
			}
		} while ((end - start) > invertedKFAccuracy);

		return -(end + start) / 2.0;
	}

	public double getRandom(double expectedValue, double variance, double invertedKFAccuracy) {
		final Random random = new Random();
		double point = 0.0d;

		do {
			point = random.nextDouble();
		} while (point == 0.0d);

		return (InvertedIntGaussKF(point, invertedKFAccuracy) * variance) + expectedValue;
	}

	/**
	 * Returns the singleton instance of this class
	 * 
	 * @return the instance of this class
	 */
	public static GaussKFInvertible getInstance() {
		return SingletonHolder.INSTANCE;
	}

	@Override
	public double k(double u) {
		return Normal.pdf(u, 0, 1);
	}

	@Override
	public double intK(double u) {
		return Normal.cdf(u, 0, 1);
	}

	@Override
	public double k2() {
		return 1;
	}

	@Override
	public double cutOff() {
		/*
		 * This is not techincaly correct, as this value of k(u) is still 7.998827757006813E-38 However, this is very close to zero, and is so small that k(u)+x
		 * = x, for most values of x. Unless this probability si going to be near zero, values past this point will have no effect on the result
		 */
		return 13;
	}

	@Override
	public double kPrime(double u) {
		return -exp(-pow(u, 2) / 2) * u / sqrt(2 * PI);
	}

	@Override
	public String toString() {
		return "Gaussian Kernel";
	}
}

