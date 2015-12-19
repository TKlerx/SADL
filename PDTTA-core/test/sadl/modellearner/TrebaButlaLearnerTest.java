package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.TransitionsType;
import sadl.input.TimedInput;
import sadl.interfaces.AutomatonModel;
import sadl.utils.IoUtils;

public class TrebaButlaLearnerTest {

	@Test
	public void test() throws URISyntaxException {
		final ButlaPdtaLearner butla = new ButlaPdtaLearner(100000000, 0.05, TransitionsType.Outgoing, 0.05, 0.05, PTAOrdering.TopDown,
				EventsCreationStrategy.DontSplitEvents, KDEFormelVariant.OriginalKDE);
		final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, true);
		final Path p = Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI());
		Pair<TimedInput, TimedInput> input = IoUtils.readTrainTestFile(p);

		final AutomatonModel pdta = butla.train(input.getKey());

		input = IoUtils.readTrainTestFile(p);
		final AutomatonModel pdfa = treba.train(input.getKey());

		assertEquals(pdta.getStateCount(), pdfa.getStateCount());
		assertEquals(pdta.getTransitionCount(), pdfa.getTransitionCount());

	}

}
