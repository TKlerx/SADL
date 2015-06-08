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
import sadl.modellearner.TauPtaLearner;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;

public class TauPtaTestSmallV2 {
	@BeforeClass
	public static void resetSeed() {
		MasterSeed.reset();
	}
	@Test
	public void testTauPTATimedInputAbnormal() throws IOException, URISyntaxException, ClassNotFoundException {
		Path p = Paths.get(TauPtaTestSmallV2.class.getResource("/taupta/small/rti_small.txt").toURI());
		final TimedInput trainingTimedSequences = TimedInput.parseAlt(p, 1);
		final TauPtaLearner learner = new TauPtaLearner();
		final TauPTA pta = learner.train(trainingTimedSequences);
		for (final AnomalyInsertionType type : AnomalyInsertionType.values()) {
			if (type != AnomalyInsertionType.NONE && type != AnomalyInsertionType.ALL) {
				final TauPTA anomaly1 = SerializationUtils.clone(pta);
				anomaly1.makeAbnormal(type);
				p = Paths.get(this.getClass().getResource("/taupta/small/pta_abnormal_" + type.getTypeIndex() + ".ser").toURI());
				final TauPTA des = (TauPTA) IoUtils.deserialize(p);
				assertEquals("Test failed for anomaly type " + type.getTypeIndex(), anomaly1, des);
				System.out.println("Test " + type + " passed.");
			}
		}
	}

}
