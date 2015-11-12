package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
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
import sadl.modellearner.rtiplus.SimplePDRTALearner.DistributionCheckType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.OperationTesterType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.utils.IoUtils;

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

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.ALL, SplitPosition.MIDDLE, "AAO",
					null);
			final Model p1 = l1.train(ti1);

			final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.ALL, SplitPosition.MIDDLE, "AAO",
					null);
			final Model p2 = l2.train(ti2);

			assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
		}
	}

	// @Test
	public void testDeterminismNaive() throws URISyntaxException, IOException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));
			final TimedInput ti2 = SerializationUtils.clone(ti1);

			final SimplePDRTALearner l1 = new SimplePDRTALearner(0.05, "4", OperationTesterType.NAIVE_LRT, DistributionCheckType.ALL, SplitPosition.MIDDLE,
					"AOO",
					"/home/fabian/sadl_rti_test/" + i + "/");
			final Model p1 = l1.train(ti1);

			final SimplePDRTALearner l2 = new SimplePDRTALearner(0.05, "4", OperationTesterType.NAIVE_LRT, DistributionCheckType.ALL, SplitPosition.MIDDLE,
					"AOO", null);
			final Model p2 = l2.train(ti2);

			assertEquals("PDRTAs for files " + i + " are not equal", p2, p1);
		}
	}

	@Test
	public void testSerialization() throws URISyntaxException, IOException, ClassNotFoundException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.ALL,SplitPosition.MIDDLE, "AOO", null);
			final Model p = l.train(ti);

			final Path path = Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI());
			IoUtils.serialize(p, path);
			final Model cP = (Model) IoUtils.deserialize(path);

			assertEquals("PDRTAs for files " + i + " are not equal", p, cP);
		}
	}

	@Test
	public void testCorrectness() throws URISyntaxException, IOException, ClassNotFoundException {

		for (int i = 1; i <= 5; i++) {

			final TimedInput ti = TimedInput.parse(Paths.get(this.getClass().getResource("/pdrta/test_" + i + ".inp").toURI()));

			final SimplePDRTALearner l = new SimplePDRTALearner(0.05, "4", OperationTesterType.LRT, DistributionCheckType.ALL,SplitPosition.MIDDLE, "AOO", null);
			final Model pdrta = l.train(ti);

			// Deserialize
			final Model p = (Model) IoUtils.deserialize(Paths.get(this.getClass().getResource("/pdrta/pdrta_" + i + ".aut").toURI()));

			assertEquals("PDRTAs for files " + i + " are not equal", pdrta, p);
		}
	}

}
