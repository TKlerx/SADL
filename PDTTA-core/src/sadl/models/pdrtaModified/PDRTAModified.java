package sadl.models.pdrtaModified;

import gnu.trove.list.TDoubleList;

import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.models.PTA.Event;
import sadl.models.PTA.SubEvent;

public class PDRTAModified implements AutomatonModel {

	PDRTAStateModified root;
	HashMap<String, Event> events;

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {
		// TODO Auto-generated method stub
		return null;
	}

	public PDRTAModified(PDRTAStateModified root, HashMap<String, Event> events) {
		this.root = root;
		this.events = events;
	}

	public PDRTAStateModified getRoot() {

		return root;
	}

	public boolean hasAnomalie(TimedWord word) {

		final PDRTAStateModified currentState = root;

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDRTATransitionModified transition = currentState.getTransition(eventSymbol, time);

			if (transition == null) {
				System.out.println("ERROR: (" + currentState.getId() + ")");
				return true;
			}

			final SubEvent event = transition.getEvent();

			if (event.hasWarning(time)) {
				System.out.println("WARNING: time in warning arrea. (" + currentState.getId() + ")");
			}

			if (event.isInCriticalArea(time)) {
				System.out.println("WARNING: time in critical area. Wrong decision possible. (" + currentState.getId() + ")");
			}

		}

		if (!currentState.isFinalState()) {
			System.out.println("ERROR: ended not in final state. (" + currentState.getId() + ")");
			return true;
		}

		return false;
	}
}
