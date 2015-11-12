package jsat.distributions.empirical;

import java.util.Random;

import jsat.distributions.Normal;

public class NormalRandomized extends Normal {

	private static final long serialVersionUID = 2926027478092427009L;

	public NormalRandomized() { // TODO remove?
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

}
