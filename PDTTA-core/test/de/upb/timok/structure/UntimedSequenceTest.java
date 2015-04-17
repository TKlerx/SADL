package de.upb.timok.structure;

import static org.junit.Assert.*;
import gnu.trove.list.array.TIntArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.upb.timok.constants.ClassLabel;

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
		UntimedSequence original = new UntimedSequence(new TIntArrayList(new int []{1,2,3}), ClassLabel.NORMAL);
		UntimedSequence clone = original.clone();
		clone.setLabel(ClassLabel.ANOMALY);
		clone.events.set(1, 5);
		assertNotEquals("Changes in the clone should not affect the original.",original.getEvent(1), clone.getEvent(1));
	}

}
