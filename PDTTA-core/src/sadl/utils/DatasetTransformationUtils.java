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
package sadl.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.linear.DenseVector;

/**
 * 
 * @author Timo Klerx
 *
 */
public class DatasetTransformationUtils {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(DatasetTransformationUtils.class);

	public static DataSet<SimpleDataSet> doublesToDataSet(List<double[]> doubleVectors) {
		final List<DataPoint> dataPoints = new ArrayList<>(doubleVectors.size());
		for (final double[] sample : doubleVectors) {
			final DataPoint dp = new DataPoint(new DenseVector(sample));
			dataPoints.add(dp);
		}
		final DataSet<SimpleDataSet> result = new SimpleDataSet(dataPoints);
		return result;
	}

	public static ClassificationDataSet doublesToClassificationDataSet(List<double[]> doubleVectors) {
		// We create a new data set. This data set will have 2 dimensions so we can visualize it, and 4 target class values
		final ClassificationDataSet dataSet = new ClassificationDataSet(doubleVectors.get(0).length, new CategoricalData[0], new CategoricalData(1));
		for (final double[] sample : doubleVectors) {
			dataSet.addDataPoint(new DenseVector(sample), new int[0], 0);
		}
		return dataSet;
	}

	public static List<double[]> dataPointsToArray(List<DataPoint> dataPoints) {
		final List<double[]> result = new ArrayList<>(dataPoints.size());
		for (final DataPoint sample : dataPoints) {
			result.add(sample.getNumericalValues().arrayCopy());
		}
		return result;
	}

}
