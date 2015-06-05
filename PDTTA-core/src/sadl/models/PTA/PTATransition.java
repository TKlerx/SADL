package sadl.models.PTA;

public class PTATransition {

	protected SplittedEvent event;
	protected PTAState fromState;
	protected PTAState toState;
	protected int count;

	public PTATransition(PTAState fromState, PTAState toState, SplittedEvent event) {
		this.fromState = fromState;
		this.toState = toState;
		this.event = event;
		count = 0;
	}

	public PTAState previousState() {

		return fromState;
	}

	public PTAState nextState() {

		return toState;
	}

	public void incrementCount(int count) {

		this.count += count;
	}
}
