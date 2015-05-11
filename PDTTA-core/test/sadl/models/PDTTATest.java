package sadl.models;

import static org.junit.Assert.fail;

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
import sadl.modellearner.PdttaLeaner;

public class PDTTATest {

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
		final TimedInput ti1 = TimedInput.parse(Paths.get(this.getClass().getResource("/pdtta/input_data.txt").toURI()));
		final TimedInput ti2 = SerializationUtils.clone(ti1);
		final PdttaLeaner l1 = new PdttaLeaner(0.05, false);
		final PdttaLeaner l2 = new PdttaLeaner(0.05, false);
		final Model p1 = l1.train(ti1);
		fail("Not yet implemented");
	}

}
