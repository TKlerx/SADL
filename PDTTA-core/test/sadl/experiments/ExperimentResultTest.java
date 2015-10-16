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
