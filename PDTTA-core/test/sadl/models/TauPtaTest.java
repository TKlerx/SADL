package sadl.models;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTest {

	@Test
	public void testTauPTATimedInputNormal() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_normal_medium.xml").toURI());
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

	@Test
	public void testTauPTATimedInputAbnormal1() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_1_medium.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_ONE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal1Small() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_1_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_ONE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal2() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_2_medium.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal2Small() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_2_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_TWO);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal3() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_3_medium.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_THREE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal3Small() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_3_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_THREE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal4() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_4_medium.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FOUR);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal4Small() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_4_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FOUR);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal5() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_medium.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_5_medium.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FIVE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

	@Test
	public void testTauPTATimedInputAbnormal5Small() throws IOException, URISyntaxException {
		Path p = Paths.get(this.getClass().getResource("/rti_small.txt").toURI());
		MasterSeed.reset();
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		p = Paths.get(this.getClass().getResource("/pta_abnormal_5_small.xml").toURI());
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		pta.makeAbnormal(AnomalyInsertionType.TYPE_FIVE);
		final TauPTA saved = (TauPTA) IoUtils.xmlDeserialize(p);
		assertEquals(pta, saved);
	}

}
