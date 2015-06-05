package sadl.models.PTA;

import java.util.LinkedHashMap;
import java.util.Map;

public class PTAState {

	protected Map<String, PTATransition> transitions = new LinkedHashMap<>();

	public PTAState() {

	}

	public void addTransition(PTATransition transition) {

		transitions.put(transition.event.getSymbol(), transition);
	}

	public void removeTransition(String symbol) {

		transitions.remove(symbol);
	}

	public PTATransition getTransition(String symbol) {

		return transitions.get(symbol);
	}
}
