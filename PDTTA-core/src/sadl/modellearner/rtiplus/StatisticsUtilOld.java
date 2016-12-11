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
package sadl.modellearner.rtiplus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A utility class that provides some common statistical functions.
 * 
 * @author Fabian Witter
 */
@SuppressWarnings("all")
public abstract class StatisticsUtilOld {

	/**
	 * Returns the mean of a collection of {@link Number} objects.
	 * 
	 * @param values
	 *            {@link Collection} of values; elements may not be {@code null}; {@code NaN} and infinite values will be ignored
	 * 
	 * @return The mean of the given {@link Collection}
	 */
	public static double calculateMean(Collection<? extends Number> values) {

		int count = 0;
		double total = 0.0;
		final Iterator<? extends Number> iterator = values.iterator();
		while (iterator.hasNext()) {
			final double value = iterator.next().doubleValue();
			if (!Double.isNaN(value) && !Double.isInfinite(value)) {
				total += value;
				count++;
			}
		}
		return total / count;
	}

	/**
	 * 0: Minimum regular value<br> 1: Q1<br> 2: Median<br> 3: Q3<br> 4: Maximum regular value<br> 5: Mean<br>
	 * 
	 * @param values @param copyAndSort @return bla
	 */
	public static double[] calculateBox(List<Double> values, boolean copyAndSort) {

		final double[] box = new double[6];
		if (values != null && copyAndSort) {
			final List<Double> copy = new ArrayList<Double>(values);
			Collections.sort(copy);
			values = copy;
		}
		box[5] = calculateMean(values);
		box[2] = calculateMedian(values, false);
		box[1] = calculateQ1(values, false);
		box[3] = calculateQ3(values, false);
		final double iqr = box[3] - box[1];
		box[0] = box[1] - (1.5 * iqr);
		box[4] = box[3] + (1.5 * iqr);
		return box;
	}

	public static double calculateQ1(List<Double> values, boolean copyAndSort) {

		double result = Double.NaN;
		if (values != null) {
			if (copyAndSort) {
				final List<Double> copy = new ArrayList<Double>(values);
				Collections.sort(copy);
				values = copy;
			}
			final int count = values.size();
			if (count > 0) {
				if (count % 2 == 1) {
					if (count > 1) {
						result = calcMedian(values, 0, (count - 3) / 2);
					} else {
						result = calcMedian(values, 0, 0);
					}
				} else {
					result = calcMedian(values, 0, (count / 2) - 1);
				}

			}
		}
		return result;
	}

	public static double calculateQ3(List<Double> values, boolean copyAndSort) {

		double result = Double.NaN;
		if (values != null) {
			if (copyAndSort) {
				final List<Double> copy = new ArrayList<Double>(values);
				Collections.sort(copy);
				values = copy;
			}
			final int count = values.size();
			if (count > 0) {
				if (count % 2 == 1) {
					if (count > 1) {
						result = calcMedian(values, (count + 1) / 2, count - 1);
					} else {
						result = calcMedian(values, 0, 0);
					}
				} else {
					result = calcMedian(values, count / 2, count - 1);
				}
			}
		}
		return result;
	}

	/**
	 * Calculates the median for a list of values (<code>Number</code> objects). If <code>copyAndSort</code> is <code>false</code>, the list is assumed to be
	 * presorted in ascending order by value.
	 * 
	 * @param values
	 *            the values (<code>null</code> permitted).
	 * @param copyAndSort
	 *            a flag that controls whether the list of values is copied and sorted.
	 * 
	 * @return The median.
	 */
	public static double calculateMedian(List<Double> values, boolean copyAndSort) {

		double result = Double.NaN;
		if (values != null) {
			if (copyAndSort) {
				final List<Double> copy = new ArrayList<Double>(values);
				Collections.sort(copy);
				values = copy;
			}
			result = calcMedian(values, 0, values.size() - 1);
		}
		return result;
	}

	private static double calcMedian(List<Double> values, int start, int end) {

		double result = Double.NaN;
		final int count = end - start + 1;
		if (count > 0) {
			if (count % 2 == 1) {
				if (count > 1) {
					result = values.get(start + ((count - 1) / 2)).doubleValue();
				} else {
					result = values.get(start).doubleValue();
				}
			} else {
				final Number value1 = values.get(start + ((count / 2) - 1));
				final Number value2 = values.get(start + (count / 2));
				result = (value1.doubleValue() + value2.doubleValue()) / 2.0;
			}
		}
		return result;
	}

	public static double calculateMAD(List<? extends Number> values, Number median) {

		double result = Double.NaN;
		if (values != null && median != null) {
			final List<Double> diffs = new ArrayList<Double>(values.size());
			for (int i = 0; i < values.size(); i++) {
				diffs.add(Math.abs(values.get(i).doubleValue() - median.doubleValue()));
			}
			Collections.sort(diffs);
			final int count = diffs.size();
			if (count > 0) {
				if (count % 2 == 1) {
					if (count > 1) {
						result = diffs.get((count - 1) / 2).doubleValue();
					} else {
						result = diffs.get(0).doubleValue();
					}
				} else {
					final double value1 = diffs.get(count / 2 - 1);
					final double value2 = diffs.get(count / 2);
					result = (value1 + value2) / 2.0;
				}
			}
		}
		return result;
	}

	public static double calculateMAD(List<Double> values, boolean copyAndSort) {

		double result = Double.NaN;
		if (values != null) {
			final double median = calculateMedian(values, copyAndSort);
			result = calculateMAD(values, median);
		}
		return result;
	}

	/**
	 * Returns the standard deviation of a set of numbers.
	 * 
	 * @param data
	 *            the data (<code>null</code> or zero length array not permitted).
	 * 
	 * @return The standard deviation of a set of numbers.
	 */
	public static double getVariance(Collection<? extends Number> data, Number mean) {

		double result = Double.NaN;
		if (data != null && data.size() > 0 && mean != null) {
			double sum = 0.0;
			for (final Number n : data) {
				final double diff = n.doubleValue() - mean.doubleValue();
				sum = sum + (diff * diff);
			}
			result = sum / (data.size());
		}
		return result;
	}

	public static double getVariance(Collection<? extends Number> data) {

		final double avg = calculateMean(data);
		return getVariance(data, avg);
	}

	public static double getStdDev(Collection<? extends Number> data) {

		final double avg = calculateMean(data);
		return getStdDev(data, avg);
	}

	public static double getStdDev(Collection<? extends Number> data, Number mean) {

		return Math.sqrt(getVariance(data, mean));
	}

}
