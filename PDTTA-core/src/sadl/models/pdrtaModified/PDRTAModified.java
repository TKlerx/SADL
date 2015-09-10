package sadl.models.pdrtaModified;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TIntLinkedList;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.models.PTA.Event;
import sadl.models.PTA.SubEvent;

public class PDRTAModified implements AutomatonModel {

	PDRTAStateModified root;
	HashMap<String, Event> events;

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {

		final TDoubleList propabilities1 = new TDoubleArrayList(s.length());
		final TDoubleList propabilities2 = new TDoubleArrayList(s.length());

		final PDRTAStateModified currentState = root;

		for (int i = 0; i < s.length(); i++) {
			final String eventSymbol = s.getSymbol(i);
			final double time = s.getTimeValue(i);

			final PDRTATransitionModified currentTransition = currentState.getTransition(eventSymbol, time); // TODO
		}

		return new Pair<>(propabilities1, propabilities2);
	}

	public PDRTAModified(PDRTAStateModified root, HashMap<String, Event> events) {
		this.root = root;
		this.events = events;
	}

	public PDRTAStateModified getRoot() {

		return root;
	}

	public TimedInput generateRandomSequences(boolean allowAnomaly, int count) throws Exception {

		final LinkedList<TimedWord> words = new LinkedList<>();

		for (int i = 0; i < count; i++) {

			final LinkedList<String> symbols = new LinkedList<>();
			final TIntLinkedList timeValues = new TIntLinkedList();

			PDRTAStateModified currentState = root;

			while (currentState != null) {

				final PDRTATransitionModified nextTransition = currentState.getRandomTransition();

				if (nextTransition != null){
					final SubEvent event = nextTransition.getEvent();
					final String eventSymbol = event.getEvent().getSymbol();
					final double time = event.generateRandomTime(allowAnomaly);
					symbols.add(eventSymbol);
					timeValues.add((int) time);

					currentState = nextTransition.getTarget();
				}
				else{
					currentState = null;
				}
			}

			words.add(new TimedWord(symbols, timeValues, ClassLabel.NORMAL));
		}

		return new TimedInput(words);

	}

	public boolean hasAnomalie(TimedWord word) {

		PDRTAStateModified currentState = root;

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDRTATransitionModified transition = currentState.getTransition(eventSymbol, time);

			if (transition == null) {
				// System.out.println("ERROR: (" + currentState.getId() + ")");
				return true;
			}

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
}
