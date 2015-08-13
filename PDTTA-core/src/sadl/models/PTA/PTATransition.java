package sadl.models.PTA;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import jsat.utils.Pair;

public class PTATransition {

	protected int id;
	protected SplittedEvent event;
	protected PTAState source;
	protected PTAState target;
	protected int count;

	private static int idCounter = 0;

	public PTATransition(PTAState source, PTAState target, SplittedEvent event, int count) {

		/*
		 * if (count <= 0) { throw new IllegalArgumentException("Count has to be > 0."); }
		 */
		this.id = idCounter++;
		this.source = source;
		this.target = target;
		this.event = event;
		this.count = count;
	}

	public void add() {
		final String eventSymbol = event.getSymbol();
		source.outTransitions.put(eventSymbol, this);
		LinkedHashMap<Integer, PTATransition> transitions = target.inTransitions.get(eventSymbol);

		if (transitions == null) {
			transitions = new LinkedHashMap<>();
			target.inTransitions.put(eventSymbol, transitions);
		}

		transitions.put(source.getId(), this);
	}

	public void remove() {
		final String eventSymbol = event.getSymbol();
		source.outTransitions.remove(eventSymbol);
		target.inTransitions.get(eventSymbol).remove(source.getId());
	}

	public PTAState getSource() {

		return source;
	}

	public PTAState getTarget() {

		return target;
	}

	public SplittedEvent getEvent() {

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

	public static void merge(PTATransition firstTransition, PTATransition secondTransition) {
		System.out.println("MergeTR begin: \t" + firstTransition + "\n \t\t" + secondTransition);
		if (firstTransition == secondTransition) {
			return;
		}
		firstTransition.incrementCount(secondTransition.getCount());
		secondTransition.remove();
		System.out.println("MergeTR after: \t" + firstTransition + "\n \t\t" + secondTransition);
	}

	public static void add(LinkedList<PTATransition> transitionsToAdd) {
		for (final PTATransition transition : transitionsToAdd) {
			transition.add();
		}
	}

	public static void remove(LinkedList<PTATransition> transitionsToRemove) {
		for (final PTATransition transition : transitionsToRemove) {
			transition.remove();
		}
	}

	public static void merge(LinkedList<Pair<PTATransition, PTATransition>> transitionsToMerge) {
		for (final Pair<PTATransition, PTATransition> transitionPair : transitionsToMerge) {
			PTATransition.merge(transitionPair.getFirstItem(), transitionPair.getSecondItem());
		}
	}
}
