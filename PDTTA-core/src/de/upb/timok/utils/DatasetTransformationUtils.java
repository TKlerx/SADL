/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.upb.timok.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.linear.DenseVector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class DatasetTransformationUtils {
	private static Logger logger = LoggerFactory.getLogger(DatasetTransformationUtils.class);

	public static Instances trainingSetToInstances(List<double[]> trainingSet) {
		final double[] sample = trainingSet.get(0);
		final ArrayList<Attribute> fvWekaAttributes = new ArrayList<>(sample.length + 1);
		for (int i = 0; i < sample.length; i++) {
			fvWekaAttributes.add(new Attribute(Integer.toString(i)));
		}

		final ArrayList<String> classStrings = new ArrayList<>();
		classStrings.add("normal");
		final Attribute ClassAttribute = new Attribute("class", classStrings);

		// Declare the feature vector
		fvWekaAttributes.add(ClassAttribute);
		final Instances result = new Instances("trainingSet", fvWekaAttributes, trainingSet.size());
		result.setClass(ClassAttribute);
		result.setClassIndex(fvWekaAttributes.size() - 1);
		for (final double[] instance : trainingSet) {
			final double[] newInstance = Arrays.copyOf(instance, instance.length + 1);
			newInstance[newInstance.length - 1] = 0;
			final Instance wekaInstance = new DenseInstance(1, newInstance);
			wekaInstance.setDataset(result);
			result.add(wekaInstance);
		}
		return result;
	}

	public static Instances testSetToInstances(List<double[]> testSet) {
		if (testSet.size() == 0) {
			logger.warn("TestSet has size 0");
		}
		final double[] sample = testSet.get(0);
		final ArrayList<Attribute> fvWekaAttributes = new ArrayList<>(sample.length);
		for (int i = 0; i < sample.length; i++) {
			fvWekaAttributes.add(new Attribute(Integer.toString(i)));
		}
		final ArrayList<String> classStrings = new ArrayList<>();
		classStrings.add("normal");
		final Attribute ClassAttribute = new Attribute("class", classStrings);
		fvWekaAttributes.add(ClassAttribute);

		// Declare the feature vector
		final Instances result = new Instances("testSet", fvWekaAttributes, testSet.size());
		result.setClassIndex(fvWekaAttributes.size() - 1);
		for (final double[] instance : testSet) {
			final Instance wekaInstance = new DenseInstance(1, instance);
			wekaInstance.setDataset(result);
			result.add(wekaInstance);
		}
		return result;
	}

	public static List<double[]> instancesToDoubles(Instances instances, boolean chopClassAttribute) {
		final List<double[]> result = new ArrayList<>();
		for (int i = 0; i < instances.size(); i++) {
			final Instance instance = instances.get(i);
			double[] temp = instance.toDoubleArray();
			if (chopClassAttribute) {
				temp = Arrays.copyOfRange(temp, 0, temp.length - 1);
			}
			result.add(temp);
		}
		return result;
	}

	public static DataSet doublesToDataSet(List<double[]> doubleVectors) {
		final List<DataPoint> dataPoints = new ArrayList<>(doubleVectors.size());
		for (final double[] sample : doubleVectors) {
			final DataPoint dp = new DataPoint(new DenseVector(sample));
			dataPoints.add(dp);
		}
		final DataSet result = new SimpleDataSet(dataPoints);
		return result;
	}

}
