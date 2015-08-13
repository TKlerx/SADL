package sadl.models.PTA;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.input.TimedWord;

public class PTA implements Cloneable {

	protected PTAState root;
	protected ArrayList<LinkedHashMap<Integer, PTAState>> tails = new ArrayList<>(50);
	protected Map<String, Event> events;
	protected boolean statesMerged = false;

	public PTA(Map<String, Event> events) {
		this.root = new PTAState(this);
		this.events = events;
	}

	public PTA(Map<String, Event> events, TimedInput timedSequences) throws Exception {
		this(events);
		this.addSequences(timedSequences);
	}

	public void addSequences(TimedInput timedSequences) throws Exception {

		if (timedSequences == null) {
			return;
		}

		for (final TimedWord sequence : timedSequences) {
			this.addSequence(sequence);
		}

	}

	public void addSequence(TimedWord sequence) throws Exception {

		PTAState currentState = root;
		boolean tailRemoved = false;

		int i;

		for (i = tails.size(); i < sequence.length(); i++) {
			tails.add(new LinkedHashMap<Integer, PTAState>());
		}

		for (i = 0; i < sequence.length(); i++) {
			final String symbol = sequence.getSymbol(i);
			final double time = sequence.getTimeValue(i);
			final Event event = events.get(symbol);

			if (event == null){
				throw new UnexpectedException("Event " + symbol + " not exists in sequence: " + sequence.toString());
			}

			final SplittedEvent subEvent = event.getSplittedEventFromTime(time);

			if (subEvent == null) {
				throw new UnexpectedException("Subevent " + symbol + " with time " + time + " not exists in sequence: " + sequence.toString());
			}

			final PTATransition transition = currentState.getTransition(subEvent.getSymbol());

			if (transition == null){
				// tails.get(i).remove(currentState.id); // TODO remove?
				tailRemoved = true;

				final PTAState nextState = new PTAState(this);
				new PTATransition(currentState, nextState, subEvent, 1).add();
				currentState = nextState;

				break;
			}

			transition.incrementCount(1);
			currentState = transition.getTarget();
		}

		for (i = i + 1; i < sequence.length(); i++) {
			final String symbol = sequence.getSymbol(i);
			final double time = sequence.getTimeValue(i);
			final Event event = events.get(symbol);

			if (event == null){
				throw new UnexpectedException("Event " + symbol + " not exists. Sequence: " + sequence.toString());
			}

			final SplittedEvent subEvent = event.getSplittedEventFromTime(time);

			if (subEvent == null) {
				throw new UnexpectedException("Subevent " + symbol + " with time " + time + " not exists. Sequence: " + sequence.toString());
			}

			final PTAState nextState = new PTAState(this);
			new PTATransition(currentState, nextState, subEvent, 1).add();

			currentState = nextState;
		}

		if (tailRemoved) {
			tails.get(sequence.length() - 1).put(currentState.getId(), currentState);
		}

	}

	public void mergeCompatibleStates() {

		final LinkedList<PTAState> finishedStates = new LinkedList<>();

		for (int i = tails.size() - 1; i > 0; i--) {
			final LinkedHashMap<Integer, PTAState> longestTails = tails.remove(i);

			for (final PTAState tailState : longestTails.values()) {

				boolean tailMerged = false;

				for (final PTAState state : finishedStates) {
					if (tailState.compatibleWith(state)) {
						PTAState.merge(state, tailState);
						tailMerged = true;
					}
				}

				if (!tailMerged) {
					finishedStates.add(tailState);
				}
				tails.get(i - 1).put(tailState.getId(), tailState);
			}

		}

		statesMerged = true;
	}

	public boolean statesMerged() {

		return this.statesMerged;
	}

	public Map<String, Event> getEventsMap() {
		return events;
	}

	@Override
	public PTA clone() { // TODO

		final PTA ptaClone = new PTA(null);
		return ptaClone;
	}

}
