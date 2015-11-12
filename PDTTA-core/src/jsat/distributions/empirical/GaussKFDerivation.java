package jsat.distributions.empirical;

import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;

public class GaussKFDerivation implements KernelFunction {

	private static final long serialVersionUID = -1862640002601190922L;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public double k2() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double cutOff() {

		return gaussKF.cutOff();
	}

}
