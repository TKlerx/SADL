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
package sadl.structure;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.constants.ClassLabel;

public class UntimedSequenceTest {

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

	@Test
	public void cloneTest() throws CloneNotSupportedException {
		final UntimedSequence original = new UntimedSequence(Arrays.asList(new String[] { "1", "2", "3" }), ClassLabel.NORMAL);
		final UntimedSequence clone = original.clone();
		clone.setLabel(ClassLabel.ANOMALY);
		clone.events.set(1, "5");
		assertNotEquals("Changes in the clone should not affect the original.",original.getEvent(1), clone.getEvent(1));
		assertNotEquals("Changes in the clone should not affect the original.",original.getLabel(), clone.getLabel());
	}

}
