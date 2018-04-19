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
package sadl.oneclassclassifier;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import jsat.classifiers.CategoricalData;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.classifiers.neuralnetwork.SOM;
import jsat.distributions.multivariate.NormalM;
import jsat.linear.DenseMatrix;
import jsat.linear.DenseVector;
import jsat.linear.Matrix;
import jsat.linear.Vec;

public class SomClassifierTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	// @Test
	public void test() {
		final ClassificationDataSet dataSet1 = new ClassificationDataSet(2, new CategoricalData[0], new CategoricalData(1));
		for (int i = 0; i < 1000; i++) {
			dataSet1.addDataPoint(DenseVector.toDenseVec(4, 4), new int[0], 0);
		}
		for (int i = 0; i < 1000; i++) {
			dataSet1.addDataPoint(DenseVector.toDenseVec(5, 5), new int[0], 0);
		}
		for (int i = 0; i < 1000; i++) {
			dataSet1.addDataPoint(DenseVector.toDenseVec(6, 6), new int[0], 0);
		}
		final SOM som = new SOM(10, 10);
		som.trainC(dataSet1);
		CategoricalResults result = som.classify(new DataPoint(DenseVector.toDenseVec(10, 10)));
		System.out.println(result);
		result = som.classify(new DataPoint(DenseVector.toDenseVec(5.5, 5.5)));
		System.out.println(result);
		System.out.println("Sample belongs to class: " + result.mostLikely());

		final SOM som2 = new SOM(10, 10);
		final ClassificationDataSet dataSet2 = new ClassificationDataSet(2, new CategoricalData[0], new CategoricalData(4));

		// We can generate data from a multivarete normal distribution. The 'M' at the end stands for Multivariate
		NormalM normal;

		// The normal is specifed by a mean and covariance matrix. The covariance matrix must be symmetric.
		// We use a simple covariance matrix for each data point for simplicity
		final Matrix covariance = new DenseMatrix(new double[][] { { 1.0, 0.0 }, // Try altering these values to see the change!
			{ 0.0, 1.0 } // Just make sure its still symetric!
		});

		// And we create 4 different means
		final Vec mean0 = DenseVector.toDenseVec(0.0, 0.0);
		final Vec mean1 = DenseVector.toDenseVec(0.0, 4.0);
		final Vec mean2 = DenseVector.toDenseVec(4.0, 0.0);
		final Vec mean3 = DenseVector.toDenseVec(4.0, 4.0);

		final Vec[] means = new Vec[] { mean0, mean1, mean2, mean3 };

		// We now generate out data
		for (int i = 0; i < means.length; i++) {
			normal = new NormalM(means[i], covariance);
			for (final Vec sample : normal.sample(300, new Random())) {
				dataSet2.addDataPoint(sample, new int[0], i);
			}
		}
		som2.trainC(dataSet2);
		final CategoricalResults result1 = som2.classify(new DataPoint(DenseVector.toDenseVec(0.0, 0.0)));
		assertEquals("peng", 0, result1.mostLikely());
		final CategoricalResults result2 = som2.classify(new DataPoint(DenseVector.toDenseVec(0.0, 4.0)));
		assertEquals("peng", 1, result2.mostLikely());
		final CategoricalResults result3 = som2.classify(new DataPoint(DenseVector.toDenseVec(4.0, 0.0)));
		assertEquals("peng", 2, result3.mostLikely());
		final CategoricalResults result4 = som2.classify(new DataPoint(DenseVector.toDenseVec(4.0, 4.0)));
		assertEquals("peng", 3, result4.mostLikely());

	}

}
