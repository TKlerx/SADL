/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package jsat.distributions.empirical;

import java.util.Arrays;

import org.apache.commons.math3.util.Precision;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.math.Function;
import jsat.math.optimization.GoldenSearch;
import sadl.constants.KDEFormelVariant;

public class KernelDensityEstimatorButla {
	protected Vec dataPoints;
	protected double[] X;
	protected Function kernelPdfFunction;
	protected Function kernelDerivationFunction;

	protected double minSearchAccuracy;
	protected double minSearchStep;

	protected double startX;
	protected double endX;

	protected KDEFormelVariant kdeFormelVariant;

	public static final double DEFAULT_BANDWIDTH = 50d;
	public static final double DEFAULT_MIN_SEARCH_ACCURACY = 0.25d;

	public KernelDensityEstimatorButla(double[] dataPoints, KDEFormelVariant formelVariant) {
		this(new DenseVector(dataPoints), formelVariant);
	}

	public KernelDensityEstimatorButla(Vec dataPoints, KDEFormelVariant formelVariant) {
		this(dataPoints, formelVariant, MyKernelDensityEstimator.BandwithGuassEstimate(dataPoints));
	}

	public KernelDensityEstimatorButla(double[] dataPoints, KDEFormelVariant formelVariant, double bandwidth) {
		this(new DenseVector(dataPoints), formelVariant, bandwidth);
	}

	public KernelDensityEstimatorButla(Vec dataPoints, KDEFormelVariant formelVariant, double bandwidth) {
		this(dataPoints, formelVariant, bandwidth, bandwidth / 4.0, DEFAULT_MIN_SEARCH_ACCURACY);
	}

	public KernelDensityEstimatorButla(double[] dataPoints, KDEFormelVariant formelVariant, double bandwidth, double minSearchStep,
			double minSearchAccuracy) {
		this(new DenseVector(dataPoints), formelVariant, bandwidth, minSearchStep, minSearchAccuracy);
	}

	public KernelDensityEstimatorButla(Vec dataPoints, KDEFormelVariant formelVariant, double bandwidth, double minSearchStep, double minSearchAccuracy) {

		// TODO check

		this.dataPoints = dataPoints.sortedCopy();
		this.X = dataPoints.arrayCopy();
		this.minSearchStep = minSearchStep;
		this.minSearchAccuracy = minSearchAccuracy;
		this.kdeFormelVariant = formelVariant;

		if (Precision.equals(bandwidth, 0)) {
			bandwidth = MyKernelDensityEstimator.BandwithGuassEstimate(dataPoints);
			this.minSearchStep = bandwidth / 4.0;
		}

		if (this.minSearchStep < 0.0001) {
			this.minSearchStep = 0.25d;
		}

		if (formelVariant == KDEFormelVariant.OriginalKDE) {

			final MyKernelDensityEstimator kernelDensity = new MyKernelDensityEstimator(dataPoints, GaussKF.getInstance(), bandwidth);
			kernelPdfFunction = ContinuousDistribution.getFunctionPDF(kernelDensity);
			kernelDerivationFunction = ContinuousDistribution
					.getFunctionPDF(new MyKernelDensityEstimator(dataPoints, GaussKFDerivation.getInstance(),
							bandwidth));

			startX = kernelDensity.min() + bandwidth;
			endX = kernelDensity.max() - bandwidth;

		} else if (formelVariant == KDEFormelVariant.OriginalButlaVariableBandwidth) {

			kernelPdfFunction = new Function() {

				private static final long serialVersionUID = 337703545623146489L;
				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}
				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = Math.pow(X[X.length - 1] * 0.05, 2);
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = dataPoints.get(i);
						if (!Precision.equals(ti, 0)) {
							sum += Math.exp(-Math.pow(t - ti, 2) / (2 * 0.05 * ti)) / (Math.sqrt(2.0 * Math.PI) * 0.05 * ti);
						}
					}

					return sum / dataPoints.length();
				}
			};

			kernelDerivationFunction = new Function() {
				private static final long serialVersionUID = 1896912471233540595L;

				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}

				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = Math.pow(X[X.length - 1] * 0.05, 2);
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = dataPoints.get(i);
						if (!Precision.equals(ti, 0)) {
							sum += (-79.7885 * Math.exp(-10 * Math.pow(t - ti, 2) / ti) * (Math.pow(ti, 2) + 0.1 * ti - Math.pow(t, 2))) / Math.pow(ti, 3);
						}
					}

					return sum / dataPoints.length();
				}
			};

			startX = Math.max(dataPoints.get(0), 1.0);
			endX = dataPoints.get(dataPoints.length() - 1);

		} else if (formelVariant == KDEFormelVariant.ButlaBandwidthNotSquared) {

			kernelPdfFunction = new Function() {
				private static final long serialVersionUID = -8200289641116502672L;

				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}

				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = Math.pow(X[X.length - 1] * 0.05, 2);
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = X[i];
						if (!Precision.equals(ti, 0)) {
							sum += Math.exp(-Math.pow(t - ti, 2) / (2 * Math.pow(0.05 * ti, 2))) / (Math.sqrt(2.0 * Math.PI) * 0.05 * ti);
						}
					}

					return sum / dataPoints.length();
				}
			};

			kernelDerivationFunction = new Function() {
				private static final long serialVersionUID = -2561020473687438986L;

				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}

				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = Math.pow(X[X.length - 1] * 0.05, 2);
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = dataPoints.get(i);
						if (!Precision.equals(ti, 0)) {
							sum += ((-7.97885 * Math.pow(ti, 2) - 3191.54 * ti * t + 3191.54 * Math.pow(t, 2)) * Math.exp(-200 * Math.pow(t - ti, 2)
									/ Math.pow(ti, 2)))
									/ Math.pow(ti, 4);
						}
					}

					return sum / dataPoints.length();
				}
			};

			startX = Math.max(dataPoints.get(0), 1.0);
			endX = dataPoints.get(dataPoints.length() - 1);

		} else if (formelVariant == KDEFormelVariant.ButlaBandwidthSquared) {

			kernelPdfFunction = new Function() {
				private static final long serialVersionUID = 6749547413109881687L;

				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}

				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = X[X.length - 1] * 0.05;
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = dataPoints.get(i);
						if (!Precision.equals(ti, 0)) {
							sum += Math.exp(-Math.pow(t - ti, 2) / (2 * 0.05 * ti)) / (Math.sqrt(2.0 * Math.PI * 0.05 * ti));
						}
					}

					return sum / dataPoints.length();
				}
			};

			kernelDerivationFunction = new Function() {
				private static final long serialVersionUID = 3612595828189571262L;

				@Override
				public double f(Vec x) {
					return f(new double[] { x.get(0) });
				}

				@Override
				public double f(double... x) {

					final double t = x[0];
					double sum = 0.0d;

					final double maxH = X[X.length - 1] * 0.05;
					int from = Arrays.binarySearch(X, t - maxH * 13);
					int to = Arrays.binarySearch(X, t + maxH * 13);
					from = from < 0 ? -from - 1 : from;
					to = to < 0 ? -to - 1 : to;

					for (int i = Math.max(0, from); i < Math.min(X.length, to + 1); i++) {
						final double ti = dataPoints.get(i);
						if (!Precision.equals(ti, 0)) {
							sum += (Math.exp(-10 * Math.pow(t - ti, 2) / ti) * (-17.8412 * Math.pow(ti, 2) - 0.892062 * ti + 17.8412 * Math.pow(t, 2)))
									/ Math.sqrt(Math.pow(ti, 5));
						}
					}

					return sum / dataPoints.length();
				}
			};

			startX = Math.max(dataPoints.get(0), 1.0);
			endX = dataPoints.get(dataPoints.length() - 1);
		}
	}

	public double pdf(double x) {

		return kernelPdfFunction.f(x);
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

	public double[] getMinima() {

		final TDoubleList pointList = new TDoubleArrayList();

		double lastX = startX;
		double lastValue = kernelDerivationFunction.f(lastX);

		double step = minSearchStep;

		if (kdeFormelVariant != KDEFormelVariant.OriginalKDE && Precision.equals(X[0], 0.0)) {
			pointList.add(0.5);
		}

		for (double x = lastX + step; x < endX; x = x + step) {
			final double newValue = kernelDerivationFunction.f(x);

			if (lastValue < 0 && newValue > 0) {
				pointList.add(GoldenSearch.minimize(minSearchAccuracy, 100, lastX, x, 0, kernelPdfFunction, new double[1]));
			}

			lastX = x;
			lastValue = newValue;

			if (kdeFormelVariant == KDEFormelVariant.ButlaBandwidthNotSquared || kdeFormelVariant == KDEFormelVariant.OriginalButlaVariableBandwidth) {
				step = x * 0.05d / 4.0;
			} else if (kdeFormelVariant == KDEFormelVariant.ButlaBandwidthSquared) {
				step = Math.pow(x * 0.05d, 2) / 4.0;
			}
		}

		return pointList.toArray();
	}

}
