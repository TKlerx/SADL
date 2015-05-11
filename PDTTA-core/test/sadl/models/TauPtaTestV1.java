package sadl.models;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTestV1 {
	// XXX if this test fails and V2 passes then this may be an issue with the reset method in this class. As long as V2 passes, everything should be OK

	static TimedInput trainingTimedSequences;

	@BeforeClass
	public static void setup() throws URISyntaxException, IOException {
		final Path p = Paths.get(TauPtaTestV1.class.getResource("/taupta/medium/rti_medium.txt").toURI());
		trainingTimedSequences = TimedInput.parseAlt(p, 1);
	}

	@Before
	public void reset() {
		MasterSeed.reset();
	}
	@Test
	public void testTauPTATimedInputNormal() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_normal.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal1() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_1.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_ONE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal2() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_2.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal3() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_3.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_THREE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal4() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_4.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FOUR);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}
	@Test
	public void testTauPTATimedInputAbnormal5() throws IOException, URISyntaxException {
		final Path p = Paths.get(this.getClass().getResource("/taupta/medium/pta_abnormal_5.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FIVE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

}
