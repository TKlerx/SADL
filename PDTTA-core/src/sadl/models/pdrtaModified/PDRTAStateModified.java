package sadl.models.pdrtaModified;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDRTAStateModified {

	protected int id;
	protected double endProbability;
	protected HashMap<String, TreeMap<Double, PDRTATransitionModified>> transitions = new HashMap<>();

	public PDRTAStateModified(int id, double endProbability) {
		this.id = id;
		this.endProbability = endProbability;
	}

	public PDRTAStateModified getNextState(String eventSymbol, double time) {

		final TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);
		final Entry<Double, PDRTATransitionModified> transitionEntry = eventTransitions.floorEntry(time);
		final PDRTATransitionModified transition = this.getTransition(eventSymbol, time);

		if (transition != null && transition.inIntervall(time)) {
			return transition.getTarget();
		}

		return null;
	}

	public PDRTATransitionModified getTransition(String eventSymbol, double time) {

		final TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);
		final Entry<Double, PDRTATransitionModified> transitionEntry = eventTransitions.floorEntry(time);

		if (transitionEntry == null) {
			return null;
		}

		return transitionEntry.getValue();
	}

	public void addTransition(SubEvent event, PDRTAStateModified target, Range<Double> intervall, double probability) {

		final PDRTATransitionModified transition = new PDRTATransitionModified(event, target, intervall, probability);
		final String eventSymbol = event.getEvent().getSymbol();

		TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			eventTransitions = new TreeMap<>();
			transitions.put(eventSymbol, eventTransitions);
		}

		eventTransitions.put(intervall.getMinimum(), transition);
	}

	public boolean isFinalState() {

		return endProbability > 0.0d;
	}

	public int getId() {
		return this.id;
	}

	@Override
	public String toString(){

		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("State " + this.id + "(");

		for (final TreeMap<Double, PDRTATransitionModified> event : transitions.values()) {
			for (final PDRTATransitionModified transition : event.values()) {
				stringBuilder.append(transition + " ");
			}
		}

		stringBuilder.append(")");

		return stringBuilder.toString();
	}
}
