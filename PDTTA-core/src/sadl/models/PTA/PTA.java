package sadl.models.PTA;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.CompatibilityChecker;
import sadl.models.pdrtaModified.PDRTAModified;
import sadl.models.pdrtaModified.PDRTAStateModified;

public class PTA {

	protected PTAState root;
	protected LinkedHashMap<Integer, PTAState> tails = new LinkedHashMap<>();
	protected HashMap<String, Event> events;

	protected LinkedList<PTAState> states = new LinkedList<>();
	protected LinkedList<PTATransition> transitions = new LinkedList<>();

	protected boolean statesMerged = false;

	public PTA(HashMap<String, Event> events) {
		this.events = events;
		this.root = new PTAState("", null, this);
		states.add(root);
		tails.put(root.getId(), root);
	}

	public PTA(HashMap<String, Event> events, TimedInput timedSequences) {
		this(events);
		this.addSequences(timedSequences);
	}

	public Map<String, Event> getEventsMap() {
		return events;
	}

	public HashMap<String, Event> getEvents() {

		return events;
	}

	// TODO getTransitions

	public LinkedHashMap<Integer, PTAState> getTails() {

		return tails;
	}

	public LinkedList<PTAState> getStates() {

		return states;
	}

	public void addSequences(TimedInput timedSequences) {

		if (timedSequences == null) {
			return;
		}

		for (final TimedWord sequence : timedSequences) {
			this.addSequence(sequence);
		}

	}

	public void addSequence(TimedWord sequence) {

		PTAState currentState = root;
		boolean addTail = false;

		int i;
		for (i = 0; i < sequence.length(); i++) {
			final String eventSymbol = sequence.getSymbol(i);
			final double time = sequence.getTimeValue(i);
			final Event event = events.get(eventSymbol);

			if (event == null){
				throw new IllegalArgumentException("Event " + eventSymbol + " not exists: " + sequence.toString());
			}

			final SubEvent subEvent = event.getSubEventByTime(time);

			final PTATransition transition = currentState.getTransition(subEvent.getSymbol());

			if (transition == null){
				tails.remove(currentState.id);
				addTail = true;

				final PTAState nextState = new PTAState(currentState.getWord() + eventSymbol, currentState, this);

				final PTATransition newTransition = new PTATransition(currentState, nextState, subEvent, 1);
				newTransition.add();

				currentState = nextState;
				states.add(currentState);

				break;
			}

			transition.incrementCount(1);
			currentState = transition.getTarget();
		}

		for (i = i + 1; i < sequence.length(); i++) {
			final String eventSymbol = sequence.getSymbol(i);
			final double time = sequence.getTimeValue(i);
			final Event event = events.get(eventSymbol);

			if (event == null){
				throw new IllegalArgumentException("Event " + eventSymbol + " not exists: " + sequence.toString());
			}

			final SubEvent subEvent = event.getSubEventByTime(time);

			final PTAState nextState = new PTAState(currentState.getWord() + eventSymbol, currentState, this);

			final PTATransition newTransition = new PTATransition(currentState, nextState, subEvent, 1);
			// newTransition.addTimeValue(time);
			newTransition.add();

			currentState = nextState;
			states.add(currentState);
		}

		if (addTail) {
			tails.put(currentState.getId(), currentState);
		}

	}

	public void mergeStatesBottomUp(CompatibilityChecker checker) {

		final LinkedList<PTAState> mergedStates = new LinkedList<>();

		final int i = 0;

		while (!tails.isEmpty()) {
			// printTails();

			final LinkedHashMap<Integer, PTAState> nextTails = new LinkedHashMap<>(tails.size());

			for (final PTAState tailState : tails.values()) {

				if (tailState.isRemoved()) {
					continue;
				}

				final PTAState fatherState = tailState.getFatherState();
				if (fatherState != root && !fatherState.isMarked()) {
					nextTails.putIfAbsent(fatherState.getId(), fatherState);
				}

				for (final ListIterator<PTAState> mergedStatesIterator = mergedStates.listIterator(); mergedStatesIterator.hasNext();) {
					final PTAState state = mergedStatesIterator.next();

					if (state.isRemoved()) {
						mergedStatesIterator.remove();
						continue;
					}

					if (checker.compatible(state, tailState)) {
						PTAState.merge(state, tailState);
						break;
					}
				}

				if (!tailState.isRemoved()) {
					tailState.mark();
					mergedStates.add(tailState);
				}
			}

			tails = nextTails;

			/*
			 * try { this.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\output-merged" + i++ + ".gv")); } catch (final IOException e) {
			 * 
			 * e.printStackTrace(); }
			 */

		}

		states = mergedStates;
		states.add(root);
		statesMerged = true;
		cleanUp();
	}

	public PDRTAModified toPDRTA() {

		final HashMap<Integer, PDRTAStateModified> pdrtaStates = new HashMap<>();

		for (final PTAState ptaState : states) {
			final int stateId = ptaState.getId();
			pdrtaStates.put(stateId, new PDRTAStateModified(stateId, ptaState.getEndProbability()));
		}

		for (final PTAState ptaState : states) {
			final PDRTAStateModified pdrtaStateSource = pdrtaStates.get(ptaState.getId());
			final int outTransitionsCount = ptaState.getOutTransitionsCount();

			for (final PTATransition transition : ptaState.outTransitions.values()) {
				final PDRTAStateModified pdrtaStateTarget = pdrtaStates.get(transition.getTarget().getId());
				final SubEvent event = transition.getEvent();

				pdrtaStateSource.addTransition(event, pdrtaStateTarget, event.getIntervalInState(ptaState), transition.getCount() / outTransitionsCount);
			}
		}

		return new PDRTAModified(pdrtaStates.get(root.getId()), pdrtaStates, events);
	}

	public void toGraphvizFile(Path resultPath) throws IOException {
		final BufferedWriter writer = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8);
		writer.write("digraph G {\n");

		// write states
		for (final PTAState state : states) {
			if (!state.isRemoved()) {
				writer.write(Integer.toString(state.getId()));
				writer.write(" [shape=circle, label=\"" + Integer.toString(state.getId()) + "\"");

				if (tails.containsKey(state.getId())) {
					writer.write(", color=red");
				}

				writer.write("]\n");
			}
		}

		for (final PTATransition transition : transitions) {
			if (!transition.removed) {
				writer.write(Integer.toString(transition.getSource().getId()) + "->" + Integer.toString(transition.getTarget().getId()) + " [label=<"
						+ transition.getEvent().getSymbol() + "(" + transition.getCount() + ")>;];\n");
			}
		}
		writer.write("}");
		writer.close();
	}

	public void printTails() {

		for (final PTAState state : tails.values()) {
			System.out.println("\t" + state);
		}

	}

	public boolean statesMerged() {

		return this.statesMerged;
	}

	private void cleanUp(){

		for (final Iterator<PTAState> statesIterator = states.iterator(); statesIterator.hasNext();) {
			final PTAState state = statesIterator.next();

			if (state.isRemoved()) {
				statesIterator.remove();
			}
		}

		for (final Iterator<PTAState> statesIterator = tails.values().iterator(); statesIterator.hasNext();) {
			final PTAState state = statesIterator.next();

			if (state.isRemoved()) {
				statesIterator.remove();
			}
		}

		for (final Iterator<PTATransition> transitionsIterator = transitions.iterator(); transitionsIterator.hasNext();) {
			final PTATransition transition = transitionsIterator.next();

			if (transition.isRemoved()) {
				transitionsIterator.remove();
			}
		}

	}

}
