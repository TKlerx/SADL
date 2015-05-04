package sadl.models;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import sadl.input.TimedInput;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTest {

	@Test
	public void testTauPTATimedInputNormal() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_normal.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputNormalSmall() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_normal_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

}
