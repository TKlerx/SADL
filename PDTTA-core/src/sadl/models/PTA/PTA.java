package sadl.models.PTA;

import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sadl.input.TimedWord;

public class PTA {

	protected PTAState root;
	protected List<PTAState> leafs = new LinkedList<>();
	protected Map<String, Event> events = new HashMap<>();

	public void addWord(TimedWord word) throws UnexpectedException {

		final PTAState currentState = root;

		for (int i = 0; i < word.length(); i++) {

			final String symbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			//TODO no trans

			final Event event = events.get(symbol);

			if (event != null) {
				final SplittedEvent subEvent = event.getSplittedEventFromTime(time);

				if (subEvent != null) {

					final PTATransition transition = currentState.getTransition(subEvent.getSymbol());

				} else {
					throw new UnexpectedException("Can not find subevent " + symbol + " with time " + time);
				}

			} else {

			}

		}
	}

}
