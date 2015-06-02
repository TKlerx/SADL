package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.util.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.input.TimedInput;
import sadl.interfaces.Model;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import sadl.utils.Settings;

public class PdttaLearnerTest {

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
		// TODO Change to train file (TimedInput.parse())
		for (int i = 1; i <= 5; i++) {
			final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(
					Paths.get(this.getClass().getResource("/pdtta/smac_mix_type" + i + ".txt").toURI()), (reader) -> {
						try {
							return TimedInput.parse(reader);
						} catch (final Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					});
			Settings.setDebug(true);
			final TimedInput ti1 = trainTest.getKey();
			final TimedInput ti2 = SerializationUtils.clone(ti1);
			final PdttaLearner l1 = new PdttaLearner(0.05, false);
			final Model p1 = l1.train(ti1);
			MasterSeed.reset();
			final PdttaLeanerOld l2 = new PdttaLeanerOld(0.05, false);
			final Model p2 = l2.train(ti2);
			assertEquals("PDTTAs for files " + i + " are not equal", p2, p1);
			// TODO Also compare with loaded model from file
		}
	}

}
