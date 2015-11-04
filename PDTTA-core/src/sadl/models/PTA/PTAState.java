package sadl.models.PTA;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import jsat.utils.Pair;

public class PTAState implements Cloneable {

	protected int id;
	protected PTA pta;
	protected String word;
	protected PTAState father;
	protected LinkedHashMap<String, LinkedHashMap<Integer, PTATransition>> inTransitions = new LinkedHashMap<>();
	protected LinkedHashMap<String, PTATransition> outTransitions = new LinkedHashMap<>();

	protected PTAState mergedWith;
	protected boolean marked = false;

	protected HashMap<Integer, PTAState> compatibilityCheckingStates = new HashMap<>();

	private static int idCounter = 0;

	public PTAState(String word, PTAState father, PTA pta) {
		this.id = idCounter++;
		this.word = word;
		this.father = father;
		this.pta = pta;

		for (final Event event : pta.getEvents().values()) {
			for (final SubEvent subEvent : event) {
				inTransitions.put(subEvent.getSymbol(), new LinkedHashMap<Integer, PTATransition>());
			}
		}
	}

	public PTA getPTA() {

		return pta;
	}

	public PTATransition getTransition(String symbol) {

		return outTransitions.get(symbol);
	}

	public String getWord() {

		return word;
	}

	public PTAState getNextState(String symbol) {

		final PTATransition transition = this.getTransition(symbol);

		if (transition == null) {
			return null;
		}

		return transition.getTarget();
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

	public PTAState getFatherState() {

		return father;
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

		return (double) endCount / outTransitionsCount;
	}

	public boolean isRemoved() {

		if (mergedWith == null) {
			return false;
		}

		return true;
	}

	public boolean isMarked() {

		return marked;
	}

	public void mark() {

		marked = true;
	}

	public static void merge(PTAState firstState, PTAState secondState) {

		if (firstState.isRemoved()) {
			firstState = firstState.isMergedWith();
		}

		if (secondState.isRemoved()) {
			secondState = secondState.isMergedWith();
		}

		if (firstState == secondState) {
			return;
		}

		LinkedList<PTATransition> transitionsToAdd = new LinkedList<>();
		LinkedList<PTATransition> transitionsToRemove = new LinkedList<>();
		final LinkedList<Pair<PTAState, PTAState>> statesToMerge = new LinkedList<>();

		// Merge incoming transitions
		for (final LinkedHashMap<Integer, PTATransition> secondStateEventInTransitions : secondState.inTransitions.values()) {
			for (final PTATransition redundantInTransition : secondStateEventInTransitions.values()) {
				transitionsToAdd.add(new PTATransition(redundantInTransition.getSource(), firstState, redundantInTransition.getEvent(), redundantInTransition
						.getCount()));
				transitionsToRemove.add(redundantInTransition);
			}
		}

		PTATransition.remove(transitionsToRemove);
		PTATransition.add(transitionsToAdd);

		transitionsToAdd = new LinkedList<>();
		transitionsToRemove = new LinkedList<>();

		// Merge outgoing transitions
		for (final PTATransition redundantTransition : secondState.outTransitions.values()) {
			final String eventSymbol = redundantTransition.getEvent().getSymbol();
			PTATransition transition = firstState.outTransitions.get(eventSymbol);

			if (transition == null) {
				transition = new PTATransition(firstState, redundantTransition.getTarget(), redundantTransition.getEvent(), redundantTransition.getCount());
				transitionsToAdd.add(transition);
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
		PTAState.merge(statesToMerge);

	}

	public int getId() {

		return id;
	}

	public PTAState isMergedWith() {

		if (mergedWith.isRemoved()) {
			return mergedWith.isMergedWith();
		}

		return mergedWith;
	}



	public static void merge(List<Pair<PTAState, PTAState>> statesToMerge) {
		for (final Pair<PTAState, PTAState> statePair : statesToMerge) {
			PTAState.merge(statePair.getFirstItem(), statePair.getSecondItem());
		}
	}

	public static void setCompatibilityChecking(PTAState firstState, PTAState secondState) {

		if (firstState.getId() < secondState.getId() ) {
			firstState.compatibilityCheckingStates.put(secondState.getId(), secondState);
		} else if (secondState.getId() < firstState.getId() ) {
			secondState.compatibilityCheckingStates.put(firstState.getId(), firstState);
		}

	}

	public static void unsetCompatibilityChecking(PTAState firstState, PTAState secondState) {

		final PTAState removedState = null;

		if (firstState.getId() < secondState.getId()) {
			firstState.compatibilityCheckingStates.remove(secondState.getId());
		} else if (secondState.getId() < firstState.getId()) {
			secondState.compatibilityCheckingStates.remove(firstState.getId());
		}

	}

	public static boolean compatibilityIsChecking(PTAState firstState, PTAState secondState) {

		if (firstState.getId() < secondState.getId() && firstState.compatibilityCheckingStates.containsKey(secondState.getId())) {
			return true;
		} else if (secondState.getId() < firstState.getId() && secondState.compatibilityCheckingStates.containsKey(firstState.getId())) {
			return true;
		}

		return false;
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
