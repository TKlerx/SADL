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
