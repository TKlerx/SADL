package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.input.TimedInput;
import sadl.interfaces.Model;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner.RunMode;
import sadl.models.pdrta.PDRTA;

public class RtiPlusTest {

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
	public void testDeterminism() throws URISyntaxException, IOException {

		for (int i = 2; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05f, "4", 0, 1, RunMode.DEBUG_STEPS, "/home/fabian/sadl_rti_test/" + i + "/");
			final Model p1 = l1.train(ti1);

			final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05f, "4", 0, 1, RunMode.SILENT, null);
			final Model p2 = l2.train(ti2);

			// ******
			final PDRTA a = (PDRTA) p1;
			System.out.println("######### " + i + " ********");
			System.out.println(a.toString());
			// System.out.println(Base64.getEncoder().encodeToString(SerializationUtils.serialize(a)));
			// ******

			assertEquals("PDTTAs for files " + i + " are not equal", p2, p1);
		}
	}

	// @Test
	public void testCorrectness() throws URISyntaxException, IOException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05f, "4", 0, 1, RunMode.SILENT, null);
			final Model pdrta = l.train(ti);

			// Deserialize;
			final Model p = null;

			assertEquals("PDTTAs for files " + i + " are not equal", pdrta, p);
		}
	}

}
