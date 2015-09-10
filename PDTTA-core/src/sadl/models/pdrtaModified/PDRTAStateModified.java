package sadl.models.pdrtaModified;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDRTAStateModified {

	protected int id;
	protected double endProbability;
	protected HashMap<String, TreeMap<Double, PDRTATransitionModified>> transitions = new HashMap<>();
	protected TreeMap<Double, PDRTATransitionModified> transitionsProbability = new TreeMap<>();

	protected double sumProbability = 0.0d;

	public PDRTAStateModified(int id, double endProbability) {
		this.id = id;
		this.endProbability = endProbability;
		this.sumProbability = endProbability;
	}

	public PDRTAStateModified getNextState(String eventSymbol, double time) {

		// final TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);
		final PDRTATransitionModified transition = this.getTransition(eventSymbol, time);

		if (transition != null && transition.inIntervall(time)) {
			return transition.getTarget();
		}

		return null;
	}

	public HashMap<String, TreeMap<Double, PDRTATransitionModified>> getTransitions() {

		return transitions;
	}

	public PDRTATransitionModified getTransition(String eventSymbol, double time) {

		final TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			return null;
		}

		final Entry<Double, PDRTATransitionModified> transitionEntry = eventTransitions.floorEntry(time);

		if (transitionEntry == null) {
			return null;
		}

		return transitionEntry.getValue();
	}

	public PDRTATransitionModified getRandomTransition() throws Exception {

		if (sumProbability < 1.0d) {
			throw new Exception("Probability not 1.0");
		}

		final double random = new Random().nextDouble();

		if (random <= endProbability) {
			return null;
		}

		final Entry<Double, PDRTATransitionModified> transitionEntry = transitionsProbability.floorEntry(random);

		if (transitionEntry == null) {
			throw new Exception("No transition selected(" + random + ")" + this);
		}

		final PDRTATransitionModified transition = transitionEntry.getValue();

		if (transitionEntry.getKey() + transition.getPropability() < random) {
			throw new Exception("No transition selected.");
		}

		return transition;
	}

	public void addTransition(SubEvent event, PDRTAStateModified target, Range<Double> interval, double probability) {

		final PDRTATransitionModified transition = new PDRTATransitionModified(event, target, interval, probability);
		final String eventSymbol = event.getEvent().getSymbol();

		TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			eventTransitions = new TreeMap<>();
			transitions.put(eventSymbol, eventTransitions);
		}

		eventTransitions.put(interval.getMinimum(), transition);
		transitionsProbability.put(sumProbability, transition);
		sumProbability += probability;
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
