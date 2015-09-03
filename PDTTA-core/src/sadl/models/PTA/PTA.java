package sadl.models.PTA;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.pdrtaModified.PDRTAModified;
import sadl.models.pdrtaModified.PDRTAStateModified;

public class PTA {

	protected PTAState root;
	protected ArrayList<LinkedHashMap<Integer, PTAState>> tails = new ArrayList<>(50);
	protected HashMap<String, Event> events;
	protected boolean statesMerged = false;

	public PTA(HashMap<String, Event> events) {
		this.root = new PTAState(this);
		this.events = events;
	}

	public PTA(HashMap<String, Event> events, TimedInput timedSequences) throws Exception {
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

			final SubEvent subEvent = event.getSubEventByTime(time);

			if (subEvent == null) {
				throw new UnexpectedException("Subevent " + symbol + " with time " + time + " not exists in sequence: " + sequence.toString());
			}

			final PTATransition transition = currentState.getTransition(subEvent.getSymbol());

			if (transition == null){
				tails.get(i).remove(currentState.id); // TODO remove?
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

			final SubEvent subEvent = event.getSubEventByTime(time);

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

	public void mergeCompatibleStates() throws Exception {

		// printTails();

		final LinkedList<PTAState> mergedStates = new LinkedList<>();

		for (int i = tails.size() - 1; i > 0; i--) {
			final LinkedHashMap<Integer, PTAState> longestTails = tails.remove(i);

			for (final PTAState tailState : longestTails.values()) {

				boolean tailMerged = false;

				final PTAState father = tailState.inTransitions.values().iterator().next().values().iterator().next().getSource();
				tails.get(i - 1).put(father.getId(), father); // TODO vater

				final Iterator<PTAState> mergedStatesIterator = mergedStates.iterator();
				while (mergedStatesIterator.hasNext()) {
					final PTAState state = mergedStatesIterator.next();

					if (state.removed) {
						mergedStatesIterator.remove();
						continue;
					}

					// System.out.println("START compatible: " + tailState + " " + state);
					if (tailState.compatibleWith(state)) {
						// System.out.println("Merge: " + state + " " + tailState);
						// PTAState.merge(state, tailState); // TODO check continue for2
						PTAState.merge(tailState, state);
						// System.out.println("Merged: " + state);
						tailMerged = true;
						break;
					}
				}

				if (!tailMerged) {
					// System.out.println("Worked off: " + tailState);
					mergedStates.add(tailState); // TODO in loop?
				}

				// printWorkedOff(mergedStates);
				// printTails();
			}

			// printTails();

		}

		statesMerged = true;
	}

	public PDRTAModified toPDRTA() {

		final LinkedList<PTAState> nextPTAStatesList = new LinkedList<>();
		final LinkedList<PDRTAStateModified> nextPDRTAStatesList = new LinkedList<>();

		final HashMap<Integer, PDRTAStateModified> states = new HashMap<>();

		final PDRTAStateModified PDRTAroot = new PDRTAStateModified(root.getId(), root.getEndProbability());

		nextPTAStatesList.addLast(root);
		nextPDRTAStatesList.addLast(PDRTAroot);

		while (!nextPTAStatesList.isEmpty()) {
			final PTAState currentPTAState = nextPTAStatesList.pollFirst();
			final PDRTAStateModified currentPDRTAState = nextPDRTAStatesList.pollFirst();

			final int transitionsCount = currentPTAState.getOutTransitionsCount();

			for (final PTATransition transition : currentPTAState.outTransitions.values()) {
				final PTAState nextPTAState = transition.getTarget();
				final PDRTAStateModified nextPDRTAState;
				final SubEvent event = transition.getEvent();

				if (!nextPTAState.checked) {
					nextPDRTAState = new PDRTAStateModified(nextPTAState.getId(), nextPTAState.getEndProbability());

					nextPTAStatesList.addLast(nextPTAState);
					nextPDRTAStatesList.addLast(nextPDRTAState);
					states.put(nextPTAState.getId(), nextPDRTAState);
					nextPTAState.checked = true;
				} else {
					nextPDRTAState = states.get(nextPTAState.getId());
				}

				currentPDRTAState.addTransition(event, nextPDRTAState, event.getIntervallInState(currentPTAState), transition.getCount() / transitionsCount);
			}

		}

		return new PDRTAModified(PDRTAroot, events);
	}

	public void printTails() {

		for (int i = tails.size() - 1; i > 0; i--) {
			final LinkedHashMap<Integer, PTAState> longestTails = tails.get(i);

			System.out.print(i + ": ");

			for (final PTAState state : longestTails.values()) {
				System.out.println("\t" + state);
			}

			System.out.println();
		}

	}

	int i = 0;

	public void printWorkedOff(List<PTAState> list) {

		System.out.print(i++ + "List: ");

		for (final PTAState state : list) {
			// System.out.print(state + " ");
			break;
		}

		System.out.println("");
	}

	public boolean statesMerged() {

		return this.statesMerged;
	}

	public Map<String, Event> getEventsMap() {
		return events;
	}

}
