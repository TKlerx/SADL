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

import java.util.Random;

import jsat.distributions.Normal;

public class NormalRandomized extends Normal {

	private static final long serialVersionUID = 2926027478092427009L;

	public NormalRandomized() {
		super();
	}

	public NormalRandomized(double mean, double stndDev) {
		super(mean, stndDev);
	}

	public double getRandomPoint() {
		final Random random = new Random();
		double point = 0.0d;

		do {
			point = random.nextDouble();
		} while (point == 0.0d);

		return this.invCdf(point);
	}

	@Override
	public double cdf(double x) {

		if (Double.isInfinite(x)) {
			if (x > 0.0) {
				return 1.0;
			} else {
				return 0.0;
			}
		}

		double result = cdf(x, mean(), standardDeviation());

		if (result > 1.0) {
			result = 1.0;
		} else if (result < 0.0) {
			result = 0.0;
		}

		return result;
	}
}
