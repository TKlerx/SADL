package sadl.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import sadl.input.TimedInput;
import sadl.structure.Transition;

public class PdfaTest {

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
	public void testIsConnected() {
		final TimedInput alphabet = new TimedInput(new String[] { "a", "b" });
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(0, 0);
		finalStateProbabilities.put(1, 0);
		finalStateProbabilities.put(2, 0);
		finalStateProbabilities.put(3, 1);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(0, 1, "a", 0.5));
		transitions.add(new Transition(0, 2, "b", 0.5));
		transitions.add(new Transition(1, 3, "a", 1));
		transitions.add(new Transition(2, 3, "b", 1));
		final PDFA connected = new PDFA(alphabet, transitions, finalStateProbabilities);
		assertTrue(connected.isConnected());

		transitions = new HashSet<>();
		transitions.add(new Transition(0, 1, "a", 1));
		transitions.add(new Transition(1, 3, "a", 1));
		transitions.add(new Transition(2, 3, "b", 1));
		final PDFA notConnected1 = new PDFA(alphabet, transitions, finalStateProbabilities);
		assertFalse(notConnected1.isConnected());

		transitions = new HashSet<>();
		transitions.add(new Transition(0, 1, "a", 1));
		transitions.add(new Transition(1, 3, "a", 1));
		final PDFA notConnected2 = new PDFA(alphabet, transitions, finalStateProbabilities);
		assertFalse(notConnected2.isConnected());

	}

}
