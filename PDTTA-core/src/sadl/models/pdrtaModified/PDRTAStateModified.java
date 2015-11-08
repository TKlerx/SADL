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

	protected double sumProbabilities = 0.0d;

	public PDRTAStateModified(int id, double endProbability) {

		if (Double.isNaN(endProbability) || endProbability < 0.0d || endProbability > 1.0d) {
			throw new IllegalArgumentException("Wrong parameter endProbability: " + endProbability);
		}

		this.id = id;
		this.endProbability = endProbability;
		this.sumProbabilities = endProbability;
	}

	public PDRTAStateModified getNextState(String eventSymbol, double time) {

		final PDRTATransitionModified transition = this.getTransition(eventSymbol, time);

		if (transition != null && transition.inInterval(time)) {
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

		final PDRTATransitionModified transition = transitionEntry.getValue();

		if (transition.inInterval(time)) {
			return transition;
		}

		return null;
	}

	public PDRTATransitionModified getMostProbablyTransition(String eventSymbol, double time) {

		PDRTATransitionModified probablyTransition = getTransition(eventSymbol, time);

		if (probablyTransition == null) {
			final TreeMap<Double, PDRTATransitionModified> eventTransitions = transitions.get(eventSymbol);

			if (eventTransitions == null) {
				return null;
			}

			double maxProbability = 0.0;

			for (final PDRTATransitionModified transition : eventTransitions.values()){

				final double probability = transition.getEvent().calculateProbability(time);

				if (probability > maxProbability) {
					maxProbability = probability;
					probablyTransition = transition;
				}
			}
		}

		return probablyTransition;
	}

	public PDRTATransitionModified getRandomTransition() {

		if (sumProbabilities < 1.0d) {
			throw new IllegalStateException("Probability not 1.0");
		}

		final double random = new Random().nextDouble();

		if (random <= endProbability) {
			return null;
		}

		final Entry<Double, PDRTATransitionModified> transitionEntry = transitionsProbability.floorEntry(random);

		if (transitionEntry == null) {
			throw new IllegalStateException("No transition selected(" + random + ")" + this);
		}

		final PDRTATransitionModified transition = transitionEntry.getValue();

		if (transitionEntry.getKey() + transition.getPropability() < random) {
			throw new IllegalStateException("No transition selected.");
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
		transitionsProbability.put(sumProbabilities, transition);
		sumProbabilities += probability;

		if (sumProbabilities > 1.0d) {
			throw new IllegalStateException("Sum of probabilities > 1.0");
		}
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
