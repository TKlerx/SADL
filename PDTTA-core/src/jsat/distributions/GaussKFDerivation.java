package jsat.distributions;

import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;

import org.apache.commons.lang3.NotImplementedException;

public class GaussKFDerivation implements KernelFunction {

	private final GaussKF gaussKF = GaussKF.getInstance();

	private GaussKFDerivation() {
	}

	private static class SingletonHolder {

		public static final GaussKFDerivation INSTANCE = new GaussKFDerivation();
	}

	/**
	 * Returns the singleton instance of this class
	 * 
	 * @return the instance of this class
	 */
	public static GaussKFDerivation getInstance() {
		return SingletonHolder.INSTANCE;
	}

	@Override
	public double k(double u) {

		return gaussKF.kPrime(u);
	}

	@Override
	public double intK(double u) {

		return gaussKF.k(u);
	}

	@Override
	public double kPrime(double u) {
		throw new NotImplementedException("");
	}

	@Override
	public double k2() {
		throw new NotImplementedException("");
	}

	@Override
	public double cutOff() {

		return gaussKF.cutOff();
	}

}
