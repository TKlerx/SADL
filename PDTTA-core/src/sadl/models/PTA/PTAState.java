package sadl.models.PTA;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import jsat.utils.Pair;

public class PTAState implements Cloneable {

	protected int id;
	protected PTA pta;
	protected LinkedHashMap<String, LinkedHashMap<Integer, PTATransition>> inTransitions = new LinkedHashMap<>();
	protected LinkedHashMap<String, PTATransition> outTransitions = new LinkedHashMap<>();
	protected static float a = 0.80f;

	protected PTAState mergedWith;
	protected boolean removed = false;
	protected boolean checked = false;

	private static int idCounter = 0;

	public PTAState(PTA pta) {
		this.id = idCounter++;
		this.pta = pta;
	}

	public PTATransition getTransition(String symbol) {

		return outTransitions.get(symbol);
	}

	public int getInTransitionsCount() {

		int sum = 0;
		for (final LinkedHashMap<Integer, PTATransition> transitionsByEvent : inTransitions.values()) {
			for (final PTATransition transition : transitionsByEvent.values()) {
				sum += transition.getCount();
			}
		}

		return sum;
	}

	public int getInTransitionsCount(String eventSymbol) {

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

	public int getOutTransitionsCount() {

		int sum = 0;
		for (final PTATransition transition : outTransitions.values()) {
			sum += transition.getCount();
		}

		return sum;
	}

	public int getOutTransitionsCount(String eventSymbol) {

		final PTATransition transition = outTransitions.get(eventSymbol);

		if (transition == null) {
			return 0;
		} else {
			return transition.getCount();
		}
	}

	public double getEndProbability() {

		final int inTransitionsCount = this.getInTransitionsCount();
		final int outTransitionsCount = this.getOutTransitionsCount();
		final int endCount = inTransitionsCount - outTransitionsCount;

		if (endCount == 0) {
			return 0.0d;
		} else if (outTransitionsCount == 0) {
			return 1.0d;
		}

		return (endCount / outTransitionsCount);
	}

	public boolean compatibleWith(PTAState state){

		final PTAState stateV = this;
		final PTAState stateW = state;

		// System.out.println("Compatible BEGIN: " + stateV + " " + stateW);

		if (stateV == stateW) {
			return true;
		}

		final int inTansitionCountV = stateV.getInTransitionsCount();
		final int inTransitionCountW = stateW.getInTransitionsCount();
		final int outTransitionCountV = stateV.getOutTransitionsCount();
		final int outTransitionCountW = stateW.getOutTransitionsCount();

		/*
		 * if (stateV.inTransitions.size() != stateW.inTransitions.size() || stateV.outTransitions.size() != stateW.outTransitions.size()) { return false; }
		 */

		if (fractionDifferent(inTansitionCountV, inTansitionCountV - outTransitionCountV, inTransitionCountW, inTransitionCountW - outTransitionCountW, a)) {
			// System.out.println("Compatible END-false: " + stateV + " " + stateW);
			return false;
		}

		for (final Event event : pta.events.values()) {
			for (final SubEvent subEvent : event) {
				final String eventSymbol = subEvent.getSymbol();

				final int inTansitionCount1 = this.getInTransitionsCount(eventSymbol);
				final int inTransitionCount2 = state.getInTransitionsCount(eventSymbol);
				final int outTransitionCount1 = this.getOutTransitionsCount(eventSymbol);
				final int outTransitionCount2 = state.getOutTransitionsCount(eventSymbol);

				if (outTransitionCount1 == 0 && outTransitionCount2 == 0) {
					// return false;
					continue;
				}

				if (outTransitionCount1 == 0 || outTransitionCount2 == 0) {
					// System.out.println("Compatible END-false0: " + stateV + " " + stateW);
					return false;
				}// TODO check

				if (fractionDifferent(inTansitionCount1, inTansitionCount1 - outTransitionCount1, inTransitionCount2, inTransitionCount2 - outTransitionCount2,
						a)) {
					// System.out.println("Compatible END-false: " + stateV + " " + stateW);
					return false;
				}


				// TODO nullpointer
				if (!this.getTransition(eventSymbol).getTarget().compatibleWith(state.getTransition(eventSymbol).getTarget())) {
					// System.out.println("Compatible END-false: " + stateV + " " + stateW);
					return false;
				}


			}
		}
		// System.out.println("Compatible END-true: " + stateV + " " + stateW);
		return true;

	}

	public static void merge(PTAState firstState, PTAState secondState) throws Exception {

		if (firstState.removed) {
			firstState = firstState.isMergedWith();
		}

		if (secondState.removed) {
			secondState = secondState.isMergedWith();
		}

		if (firstState == secondState) {
			return;
		}

		if (secondState.getId() == 1687) {
			final int i = 0;
			final int j = i;
		}

		/*
		 * if (firstState.removed && secondState.removed) { throw new Exception(firstState + " and " + secondState + " is removed."); }
		 * 
		 * if (firstState.removed) { throw new Exception(firstState + " is removed."); } else if (secondState.removed) { throw new Exception(secondState +
		 * " is removed."); }
		 */

		// System.out.println("MergeST begin: \t" + firstState + "\n \t\t" + secondState);

		LinkedList<PTATransition> transitionsToAdd = new LinkedList<>();
		LinkedList<PTATransition> transitionsToRemove = new LinkedList<>();
		final LinkedList<Pair<PTAState, PTAState>> statesToMerge = new LinkedList<>();

		// Merge incoming transitions
		for (final String eventSymbol : secondState.inTransitions.keySet()) {

			// final LinkedHashMap<Integer, PTATransition> firstStateInTransitions = firstState.inTransitions.get(eventSymbol);
			final LinkedHashMap<Integer, PTATransition> secondStateInTransitions = secondState.inTransitions.get(eventSymbol);

			for (final PTATransition redundantTransition : secondStateInTransitions.values()) {
				transitionsToAdd.add(new PTATransition(redundantTransition.getSource(), firstState, redundantTransition.getEvent(), redundantTransition
						.getCount()));
				transitionsToRemove.add(redundantTransition);
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);

		transitionsToAdd = new LinkedList<>();
		transitionsToRemove = new LinkedList<>();

		// Merge outgoing transitions
		for (final PTATransition redundantTransition : secondState.outTransitions.values()) {
			final String eventSymbol = redundantTransition.getEvent().getSymbol();
			final PTATransition transition = firstState.outTransitions.get(eventSymbol);

			if (transition == null) {
				transitionsToAdd.add(new PTATransition(firstState, redundantTransition.getTarget(), redundantTransition.getEvent(), redundantTransition
						.getCount()));
				transitionsToRemove.add(redundantTransition);
			} else {
				transition.incrementCount(redundantTransition.getCount());
				transitionsToRemove.add(redundantTransition);
				statesToMerge.add(new Pair<>(transition.getTarget(), redundantTransition.getTarget()));
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);
		secondState.mergedWith = firstState;
		secondState.removed = true;
		PTAState.merge(statesToMerge);

		// System.out.println("MergeST after: \t" + firstState + "\n \t\t" + secondState);
	}

	public int getId() {

		return id;
	}

	public PTAState isMergedWith() {

		if(mergedWith.removed){
			return mergedWith.isMergedWith();
		}

		return mergedWith;
	}

	private static boolean fractionDifferent(int n0, int f0, int n1, int f1, float a) {

		if (n0 == 0 && n1 == 0) {
			return false;
		} else if (n0 == 0 || n1 == 0) {
			return true; // TODO correct?
		}

		return Math.abs((f0 / n0) - (f1 / n1)) > (Math.sqrt(0.5 * Math.log(2 / a)) * ((1 / Math.sqrt(n0)) + (1 / Math.sqrt(n1))));

	}

	public static void merge(List<Pair<PTAState, PTAState>> statesToMerge) throws Exception {
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
