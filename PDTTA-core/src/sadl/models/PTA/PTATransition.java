package sadl.models.PTA;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import jsat.utils.Pair;

public class PTATransition {

	protected int id;
	protected SubEvent event;
	protected PTAState source;
	protected PTAState target;
	protected int count;

	private static int idCounter = 0;

	public PTATransition(PTAState source, PTAState target, SubEvent event, int count) {

		this.id = idCounter++;
		this.source = source;
		this.target = target;
		this.event = event;
		this.count = count;
	}

	public void add() throws Exception {
		final String eventSymbol = event.getSymbol();

		if (source.outTransitions.put(eventSymbol, this) != null) {
			throw new Exception("Transition adding: " + "outgoing transition already exists");
		}

		LinkedHashMap<Integer, PTATransition> transitions = target.inTransitions.get(eventSymbol);

		if (transitions == null) {
			transitions = new LinkedHashMap<>();
			target.inTransitions.put(eventSymbol, transitions);
		}

		if (transitions.put(source.getId(), this) != null) {
			throw new Exception("Transition adding: " + "incoming transition already exists");
		}
	}

	public void remove() throws Exception {
		final String eventSymbol = event.getSymbol();
		if (source.outTransitions.remove(eventSymbol) == null || target.inTransitions.get(eventSymbol).remove(source.getId()) == null) {
			throw new Exception("Transition removing: " + "transition not exists");
		}
	}

	public PTAState getSource() {

		return source;
	}

	public PTAState getTarget() {

		return target;
	}

	public SubEvent getEvent() {

		return event;
	}

	public int getCount() {

		return count;
	}

	public void incrementCount(int addCount) {

		this.count += addCount;
	}

	@Override
	public String toString() {
		return source.getId() + "=(" + event + "," + count + ")=>" + target.getId();
	}

	public static void merge(PTATransition firstTransition, PTATransition secondTransition) throws Exception {
		System.out.println("MergeTR begin: \t" + firstTransition + "\n \t\t" + secondTransition);

		if (firstTransition == secondTransition) {
			return;
		}
		else if (firstTransition.getSource() != secondTransition.getSource()){
			throw new Exception("transition merging: not same source state");
		}

		firstTransition.incrementCount(secondTransition.getCount()); // TODO states merge?
		secondTransition.remove();
		System.out.println("MergeTR after: \t" + firstTransition + "\n \t\t" + secondTransition);
	}

	public static void add(LinkedList<PTATransition> transitionsToAdd) throws Exception {
		for (final PTATransition transition : transitionsToAdd) {
			transition.add();
		}
	}

	public static void remove(LinkedList<PTATransition> transitionsToRemove) throws Exception {
		for (final PTATransition transition : transitionsToRemove) {
			transition.remove();
		}
	}

	public static void merge(LinkedList<Pair<PTATransition, PTATransition>> transitionsToMerge) throws Exception {
		for (final Pair<PTATransition, PTATransition> transitionPair : transitionsToMerge) {
			PTATransition.merge(transitionPair.getFirstItem(), transitionPair.getSecondItem());
		}
	}
}
