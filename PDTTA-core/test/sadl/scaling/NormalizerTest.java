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
package sadl.scaling;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NormalizerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	List<double[]> data = new ArrayList<>();
	List<double[]> scaledData = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		data.add(new double[] { 1, 2, 4 });
		data.add(new double[] { 2, 2, 0 });
		data.add(new double[] { 3, 2, -4 });

		scaledData.add(new double[] { 0, 1, 1 });
		scaledData.add(new double[] { 0.5, 1, 0.5 });
		scaledData.add(new double[] { 1, 1, 0 });
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		final Normalizer n = new Normalizer();
		n.setFeatureCount(3);
		final List<double[]> trained = n.train(data);

		final List<double[]> scaled = n.scale(data);
		for (int i = 0; i < trained.size(); i++) {
			assertTrue(Arrays.equals(trained.get(i), scaled.get(i)));
			assertTrue(Arrays.equals(scaledData.get(i), scaled.get(i)));
		}
	}
}
