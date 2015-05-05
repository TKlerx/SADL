package sadl.models;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import sadl.constants.AnomalyInsertionType;
import sadl.input.TimedInput;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTestV2 {

	@BeforeClass
	public static void resetSeed() {
		MasterSeed.reset();
	}

	@Test
	public void testTauPTATimedInputAbnormal() throws IOException, URISyntaxException {
		Path p = Paths.get(TauPtaTestV2.class.getResource("/medium/rti_medium.txt").toURI());
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		final TauPTA pta = new TauPTA(trainingTimedSequences);
		for (final AnomalyInsertionType type : AnomalyInsertionType.values()) {
			if (type != AnomalyInsertionType.NONE && type != AnomalyInsertionType.ALL) {
				final TauPTA anomaly1 = SerializationUtils.clone(pta);
				anomaly1.makeAbnormal(type);
				p = Paths.get(this.getClass().getResource("/medium/pta_abnormal_" + type.getTypeIndex() + ".xml").toURI());
				final TauPTA des = (TauPTA) IoUtils.xmlDeserialize(p);
				assertEquals("Test failed for anomaly type " + type.getTypeIndex(), anomaly1, des);
			}
		}
	}

}
