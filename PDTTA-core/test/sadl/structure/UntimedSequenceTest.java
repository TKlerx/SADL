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
