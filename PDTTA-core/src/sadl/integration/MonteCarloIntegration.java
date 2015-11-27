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

package sadl.integration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.distributions.ContinuousDistribution;
import jsat.distributions.SingleValueDistribution;
import sadl.integration.MonteCarloPoint.MonteCarloPointComparator;
import sadl.utils.MasterSeed;

public class MonteCarloIntegration implements Serializable {
	private static final long serialVersionUID = -7798039596284260123L;
	private static Logger logger = LoggerFactory.getLogger(MonteCarloIntegration.class);
	int pointsToStore;
	MonteCarloPoint[] integral;
	boolean preprocessed = false;
	private boolean singleValueDis = false;
	final Random xRandom;
	final Random yRandom;
	public MonteCarloIntegration(int pointsToStore) {
		this.pointsToStore = pointsToStore;
		xRandom = MasterSeed.nextRandom();
		yRandom = MasterSeed.nextRandom();
	}

	public void preprocess(ContinuousDistribution d, double stepSize, double xMin, double xMax) {
		if (d instanceof SingleValueDistribution) {
			singleValueDis = true;
			preprocessed = true;
			return;
		}
		if (Double.isInfinite(xMin)) {
			xMin = Double.MIN_VALUE;
		}
		if (Double.isInfinite(xMax)) {
			xMax = Double.MAX_VALUE;
		}
		final Pair<Double, Double> minMax = findExtreme(d, xMin, xMax, stepSize);
		final double yMin = minMax.getLeft();
		final double yMax = minMax.getRight();
		final double xDiff = xMax - xMin;
		final double yDiff = yMax - yMin;

		int pointsFound = 0;
		int pointsRejected = 0;
		integral = new MonteCarloPoint[pointsToStore];
		while (pointsFound < pointsToStore) {
			final double xSampled = xMin + (xDiff * xRandom.nextDouble());
			final double ySampled = yMin + (yDiff * yRandom.nextDouble());
			final double pdfValue = d.pdf(xSampled);
			if (pdfValue > 0 && ySampled <= pdfValue) {
				// store the point because the sampled y value is smaller than the pdf value at the x value
				integral[pointsFound] = new MonteCarloPoint(xSampled, pdfValue);
				pointsFound++;
			} else {
				pointsRejected++;
			}
		}
		logger.debug("Rejected {} points", pointsRejected);
		logger.debug("Accepted {} points", pointsFound);
		Arrays.parallelSort(integral, new MonteCarloPointComparator());
		// Collections.sort(integral2);
		// integral2.sort((m1, m2) -> Double.compare(m1.getX(), m2.getX()));
		preprocessed = true;
	}

	public boolean isPreprocessed() {
		return preprocessed;
	}

	public void preprocess(ContinuousDistribution d, int numberOfSteps, double xMin, double xMax) {
		preprocess(d, (xMax - xMin) / numberOfSteps, xMin, xMax);
	}

	public void preprocess(ContinuousDistribution d, int numberOfSteps) {
		final double xMin = d.min();
		final double xMax = d.max();
		preprocess(d, (xMax - xMin) / numberOfSteps, xMin, xMax);
	}

	public void preprocess(ContinuousDistribution d, double stepSize) {
		preprocess(d, stepSize, d.min(), d.max());
	}

	/**
	 * Computes the proportion of the pdf where the function's density values are smaller than the given value
	 * 
	 * @param pdfValue the density value
	 * 
	 * @return the proportion of the area with smaller pdf values
	 */
	public double integrate(double pdfValue) {
		if (!isPreprocessed()) {
			throw new IllegalStateException("Preprocess before integrating!");
		}
		if (singleValueDis) {
			// There is only zero and one possible if the distribution is single value
			if (Precision.equals(pdfValue, 1)) {
				return 1;
			} else {
				return 0;
			}
		}
		// x value of monte carlo point does not matter, we search for the pdf value
		int foundIndex = Arrays.binarySearch(integral, new MonteCarloPoint(0, pdfValue));
		if (foundIndex > 0) {
			// Check whether there are the same pdf values right to the found one (is just done because of binary search)
			while (foundIndex + 1 < integral.length && Precision.equals(pdfValue, integral[foundIndex + 1].getPdfValue())) {
				foundIndex++;
			}
		} else if (foundIndex < 0) {
			foundIndex++;
			foundIndex *= -1;
			if (foundIndex > 0) {
				foundIndex--;
			}
		}
		logger.debug("FoundIndex={}", foundIndex);
		if (foundIndex - 1 >= 0) {
			logger.debug("Pdf value one index before={}", integral[foundIndex - 1].getPdfValue());
		}
		logger.debug("Pdf value to look for={}", pdfValue);
		logger.debug("Pdf value at index={}", integral[foundIndex].getPdfValue());
		if (foundIndex + 1 < integral.length) {
			logger.debug("Pdf value one index after={}", integral[foundIndex + 1].getPdfValue());
		}
		final int numberOfPoints = foundIndex;

		logger.debug("number of Points found={}", numberOfPoints);
		return numberOfPoints / (double) pointsToStore;
	}

	private Pair<Double, Double> findExtreme(ContinuousDistribution d, double xMin, double xMax, double stepResolution) {
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		for (double i = xMin; i <= xMax; i += stepResolution) {
			final double pdfValue = d.pdf(i);
			yMin = Math.min(yMin, pdfValue);
			yMax = Math.max(yMax, pdfValue);
		}
		return Pair.of(yMin, yMax);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(integral);
		result = prime * result + pointsToStore;
		result = prime * result + (preprocessed ? 1231 : 1237);
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MonteCarloIntegration other = (MonteCarloIntegration) obj;
		if (!Arrays.equals(integral, other.integral)) {
			return false;
		}
		if (pointsToStore != other.pointsToStore) {
			return false;
		}
		if (preprocessed != other.preprocessed) {
			return false;
		}
		return true;
	}

}