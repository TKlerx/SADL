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
package sadl.experiments;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExperimentResultTest {

	static ExperimentResult result;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		result = new ExperimentResult(305, 4046, 459, 190);
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

	@Test
	public void testGetPrecision() {
		assertEquals(0.399214659685864, result.getPrecision(), 0.01);
	}

	@Test
	public void testGetRecall() {
		assertEquals(0.616161616161616, result.getRecall(), 0.01);
	}

	@Test
	public void testGetFMeasure() {
		assertEquals(0.484511517077045, result.getFMeasure(), 0.01);
	}

	@Test
	public void testGetPhiCoefficient() {
		assertEquals(0.4268945458, result.getPhiCoefficient(), 0.01);
	}

	@Test
	public void testGetAccuracy() {
		assertEquals(0.8702, result.getAccuracy(), 0.01);
	}

}
