package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.input.TimedInput;
import sadl.models.TauPTA;
import sadl.models.TauPtaTestV1;

public class TauPtaLearnerTest {

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
	public void test() throws IOException, URISyntaxException {
		final TimedInput train = TimedInput.parseAlt(Paths.get(TauPtaTestV1.class.getResource("/taupta/medium/rti_medium.txt").toURI()), 1);
		@SuppressWarnings("deprecation")
		final TauPTA oldPta = new TauPTA(train);
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA newPta = learner.train(train);
		assertEquals(newPta, oldPta);

	}

}
