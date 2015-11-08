package sadl.detectors;

import gnu.trove.list.TDoubleList;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.input.TimedWord;
import sadl.models.PTA.SubEvent;
import sadl.models.pdrtaModified.PDRTAModified;
import sadl.models.pdrtaModified.PDRTAStateModified;
import sadl.models.pdrtaModified.PDRTATransitionModified;

public class AnodaDetector extends AnomalyDetector {

	protected PDRTAModified pdrta;

	public AnodaDetector(ProbabilityAggregationMethod aggType, PDRTAModified pdrta) {
		super(aggType);
		super.setModel(pdrta);
	}

	public boolean isAnomalie(TimedWord word) {

		PDRTAStateModified currentState = pdrta.getRoot();

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDRTATransitionModified transition = currentState.getTransition(eventSymbol, time);
			final SubEvent event = transition.getEvent();

			if (event.hasWarning(time)) {
				// System.out.println("WARNING: time in warning arrea. (" + currentState.getId() + ")");
			}

			if (event.isInCriticalArea(time)) {
				// System.out.println("WARNING: time in critical area. Wrong decision possible. (" + currentState.getId() + ")");
			}

			currentState = transition.getTarget();
		}

		if (!currentState.isFinalState()) {
			// System.out.println("ERROR: ended not in final state. (" + currentState.getId() + ")");
			return true;
		}

		return false;
	}

	@Override
	protected boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods) {

		throw new UnsupportedOperationException();
	}
}
