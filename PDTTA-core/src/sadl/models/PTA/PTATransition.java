package sadl.models.PTA;

import java.util.Collection;

public class PTATransition {

	protected int id;
	protected SubEvent event;
	protected PTAState source;
	protected PTAState target;
	protected int count;

	protected boolean removed = false; // TODO REMOVE

	private static int idCounter = 0;

	public PTATransition(PTAState source, PTAState target, SubEvent event, int count) {

		this.id = idCounter++;
		this.source = source;
		this.target = target;
		this.event = event;
		this.count = count;
	}

	public void add() {
		final String eventSymbol = event.getSymbol();

		source.outTransitions.put(eventSymbol, this);
		target.inTransitions.get(eventSymbol).put(source.getId(), this);

		target.pta.transitions.add(this);
	}

	public void remove() {
		final String eventSymbol = event.getSymbol();
		source.outTransitions.remove(eventSymbol);
		target.inTransitions.get(eventSymbol).remove(source.getId());
		removed = true;
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

	public boolean isRemoved() {

		return removed;
	}

	public void incrementCount(int count) {

		this.count += count;
	}

	@Override
	public String toString() {
		return source.getId() + "=(" + event.getSymbol() + "," + count + ")=>" + target.getId();
	}

	public static void add(Collection<PTATransition> transitionsToAdd) {
		for (final PTATransition transition : transitionsToAdd) {
			transition.add();
		}
	}

	public static void remove(Collection<PTATransition> transitionsToRemove) {
		for (final PTATransition transition : transitionsToRemove) {
			transition.remove();
		}
	}

}
