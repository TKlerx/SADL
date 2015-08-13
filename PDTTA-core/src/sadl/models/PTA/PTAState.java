package sadl.models.PTA;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import jsat.utils.Pair;

public class PTAState implements Cloneable {

	protected int id;
	protected PTA pta;
	protected LinkedHashMap<String, LinkedHashMap<Integer, PTATransition>> inTransitions = new LinkedHashMap<>();
	protected LinkedHashMap<String, PTATransition> outTransitions = new LinkedHashMap<>();
	protected static float a = 0.8f;

	private static int idCounter = 0;

	public PTAState(PTA pta) {
		this.id = idCounter++;
		this.pta = pta;
	}

	public PTATransition getTransition(String symbol) {

		return outTransitions.get(symbol);
	}

	public int getInTransitionCount() {

		int sum = 0;
		for (final LinkedHashMap<Integer, PTATransition> transitionsByEvent : inTransitions.values()) {
			for (final PTATransition transition : transitionsByEvent.values()) {
				sum += transition.getCount();
			}
		}

		return sum;
	}

	public int getInTransitionCount(String eventSymbol) {

		final LinkedHashMap<Integer, PTATransition> transitions = inTransitions.get(eventSymbol);

		if (transitions == null) {
			return 0;
		}

		int sum = 0;
		for (final PTATransition transition : transitions.values()) {
			sum += transition.getCount();
		}

		return sum;
	}

	public int getOutTransitionCount() {

		int sum = 0;
		for (final PTATransition transition : outTransitions.values()) {
			sum += transition.getCount();
		}

		return sum;
	}

	public int getOutTransitionCount(String eventSymbol) {

		final PTATransition transition = outTransitions.get(eventSymbol);

		if (transition == null) {
			return 0;
		} else {
			return transition.getCount();
		}
	}

	public boolean compatibleWith(PTAState state){

		final PTAState stateV = this;
		final PTAState stateW = state;

		if (stateV == stateW) {
			return true;
		}

		final int inTansitionCountV = stateV.getInTransitionCount();
		final int inTransitionCountW = stateW.getInTransitionCount();
		final int outTransitionCountV = stateV.getOutTransitionCount();
		final int outTransitionCountW = stateW.getOutTransitionCount();

		/*
		 * if (stateV.inTransitions.size() != stateW.inTransitions.size() || stateV.outTransitions.size() != stateW.outTransitions.size()) { return false; }
		 */

		if (fractionDifferent(inTansitionCountV, inTansitionCountV - outTransitionCountV, inTransitionCountW, inTransitionCountW - outTransitionCountW, a)) {
			return false;
		}

		for (final Event event : pta.events.values()) {
			for (final SplittedEvent subEvent : event.splittedEvents) {
				final String eventSymbol = subEvent.getSymbol();

				final int inTansitionCount1 = this.getInTransitionCount(eventSymbol);
				final int inTransitionCount2 = state.getInTransitionCount(eventSymbol);
				final int outTransitionCount1 = this.getOutTransitionCount(eventSymbol);
				final int outTransitionCount2 = state.getOutTransitionCount(eventSymbol);

				if (fractionDifferent(inTansitionCount1, inTansitionCount1 - outTransitionCount1, inTransitionCount2, inTransitionCount2 - outTransitionCount2,
						a)) {
					return false;
				}

				if (outTransitionCount1 == 0 || outTransitionCount2 == 0) {
					return false; // TODO check
				}

				if (this.getTransition(eventSymbol).getTarget().compatibleWith(state.getTransition(eventSymbol).getTarget())) { // only 1 ausgehende?
					return false;
				}

			}
		}

		return true;

	}

	public static void merge(PTAState firstState, PTAState secondState) {
		if (firstState == secondState){
			return;
		}

		System.out.println("MergeST begin: \t" + firstState + "\n \t\t" + secondState);

		LinkedList<PTATransition> transitionsToAdd = new LinkedList<>();
		LinkedList<PTATransition> transitionsToRemove = new LinkedList<>();
		LinkedList<Pair<PTATransition, PTATransition>> transitionsToMerge = new LinkedList<>();
		LinkedList<Pair<PTAState, PTAState>> statesToMerge = new LinkedList<>();

		// Merge incoming transitions
		for (final String eventSymbol : secondState.inTransitions.keySet()) {

			final LinkedHashMap<Integer, PTATransition> firstStateInTransitions = firstState.inTransitions.get(eventSymbol);
			final LinkedHashMap<Integer, PTATransition> secondStateInTransitions = secondState.inTransitions.get(eventSymbol);

			if (firstStateInTransitions == null) {
				for (final PTATransition redundantTransition : secondStateInTransitions.values()) {
					transitionsToAdd.add(new PTATransition(redundantTransition.getSource(), firstState, redundantTransition.getEvent(), redundantTransition
							.getCount()));
					transitionsToRemove.add(redundantTransition);
				}
			} else {
				for (final PTATransition redundantTransition : secondStateInTransitions.values()) {
					final PTATransition transition = firstStateInTransitions.get(redundantTransition.source.id);

					if (transition == null) {
						transitionsToAdd.add(new PTATransition(redundantTransition.getSource(), firstState, redundantTransition.getEvent(), redundantTransition
								.getCount()));
						transitionsToRemove.add(redundantTransition);
					} else {
						transitionsToMerge.add(new Pair<>(transition, redundantTransition));
					}

				}
			}

		}

		PTATransition.add(transitionsToAdd);
		PTATransition.remove(transitionsToRemove);
		PTATransition.merge(transitionsToMerge);
		PTAState.merge(statesToMerge);

		transitionsToAdd = new LinkedList<>();
		transitionsToRemove = new LinkedList<>();
		transitionsToMerge = new LinkedList<>();
		statesToMerge = new LinkedList<>();

		// Merge outgoing transitions
		for (final PTATransition redundantTransition : secondState.outTransitions.values()) { // TODO concurentmodification
			final String eventSymbol = redundantTransition.getEvent().getSymbol();
			final PTATransition transition = firstState.outTransitions.get(eventSymbol);

			if (transition == null) {
				transitionsToAdd.add(new PTATransition(firstState, redundantTransition.getTarget(), redundantTransition.getEvent(), redundantTransition
						.getCount()));
				transitionsToRemove.add(redundantTransition);
			} else {
				transitionsToMerge.add(new Pair<>(transition, redundantTransition)); // TODO check
				statesToMerge.add(new Pair<>(transition.getTarget(), redundantTransition.getTarget()));
			}
		}

		PTATransition.add(transitionsToAdd);
		PTATransition.remove(transitionsToRemove);
		PTATransition.merge(transitionsToMerge);
		System.out.println("MergeST after: \t" + firstState + "\n \t\t" + secondState);
		PTAState.merge(statesToMerge);

	}

	public int getId() {

		return id;
	}

	private static boolean fractionDifferent(int n0, int f0, int n1, int f1, float a) {

		if (n0 == 0 && n1 == 0) {
			return false;
		} else if (n0 == 0 || n1 == 0) {
			return false; // TODO fragen
		}

		return Math.abs((f0 / n0) - (f1 / n1)) > (Math.sqrt(0.5 * Math.log(2 / a) / Math.log(2)) * (1 / Math.sqrt(n0) + 1 / Math.sqrt(n1)));

	}

	private static int getTransitionsCount(Map<String, PTATransition> transitions) {

		final int countSum = 0;

		return countSum;
	}

	private int transitionCount(PTATransition transition) {
		if (transition == null) {
			return 0;
		} else {
			return transition.getCount();
		}
	}

	public static void merge(LinkedList<Pair<PTAState, PTAState>> statesToMerge) {
		for (final Pair<PTAState, PTAState> statePair : statesToMerge) {
			PTAState.merge(statePair.getFirstItem(), statePair.getSecondItem());
		}
	}

	@Override
	public String toString(){

		final StringBuilder stringbuilder = new StringBuilder(50);

		stringbuilder.append("State " + this.id + "(in: ");

		for (final LinkedHashMap<Integer, PTATransition> transitions : inTransitions.values()) {
			for (final PTATransition transition : transitions.values()) {
				stringbuilder.append(transition + " ");
			}
		}

		stringbuilder.append(", out: ");

		for (final PTATransition transition : outTransitions.values()) {
			stringbuilder.append(transition + " ");
		}

		stringbuilder.append(")");

		return stringbuilder.toString();
	}

}
