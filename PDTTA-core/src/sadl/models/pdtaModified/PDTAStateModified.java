package sadl.models.pdtaModified;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDTAStateModified {

	protected int id;
	protected double endProbability;
	protected HashMap<String, TreeMap<Double, PDTATransitionModified>> transitions = new HashMap<>();
	protected TreeMap<Double, PDTATransitionModified> transitionsProbability = new TreeMap<>();

	protected double sumProbabilities = 0.0d;

	public PDTAStateModified(int id, double endProbability) {

		if (Double.isNaN(endProbability) || endProbability < 0.0d || endProbability > 1.0d) {
			throw new IllegalArgumentException("Wrong parameter endProbability: " + endProbability);
		}

		this.id = id;
		this.endProbability = endProbability;
		this.sumProbabilities = endProbability;
	}

	public PDTAStateModified getNextState(String eventSymbol, double time) {

		final PDTATransitionModified transition = this.getTransition(eventSymbol, time);

		if (transition != null && transition.inInterval(time)) {
			return transition.getTarget();
		}

		return null;
	}

	public HashMap<String, TreeMap<Double, PDTATransitionModified>> getTransitions() {

		return transitions;
	}

	public PDTATransitionModified getTransition(String eventSymbol, double time) {

		final TreeMap<Double, PDTATransitionModified> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			return null;
		}

		final Entry<Double, PDTATransitionModified> transitionEntry = eventTransitions.floorEntry(time);

		if (transitionEntry == null) {
			return null;
		}

		final PDTATransitionModified transition = transitionEntry.getValue();

		if (transition.inInterval(time)) {
			return transition;
		}

		return null;
	}

	public PDTATransitionModified getMostProbablyTransition(String eventSymbol, double time) {

		PDTATransitionModified probablyTransition = getTransition(eventSymbol, time);

		if (probablyTransition == null) {
			final TreeMap<Double, PDTATransitionModified> eventTransitions = transitions.get(eventSymbol);

			if (eventTransitions == null) {
				return null;
			}

			double maxProbability = 0.0;

			for (final PDTATransitionModified transition : eventTransitions.values()){

				final double probability = transition.getEvent().calculateProbability(time);

				if (probability > maxProbability) {
					maxProbability = probability;
					probablyTransition = transition;
				}
			}
		}

		return probablyTransition;
	}

	public PDTATransitionModified getRandomTransition() {

		if (sumProbabilities < 1.0d) {
			throw new IllegalStateException("Probability not 1.0");
		}

		final double random = new Random().nextDouble();

		if (random <= endProbability) {
			return null;
		}

		final Entry<Double, PDTATransitionModified> transitionEntry = transitionsProbability.floorEntry(random);

		if (transitionEntry == null) {
			throw new IllegalStateException("No transition selected(" + random + ")" + this);
		}

		final PDTATransitionModified transition = transitionEntry.getValue();

		if (transitionEntry.getKey() + transition.getPropability() < random) {
			throw new IllegalStateException("No transition selected.");
		}

		return transition;
	}

	public void addTransition(SubEvent event, PDTAStateModified target, Range<Double> interval, double probability) {

		final PDTATransitionModified transition = new PDTATransitionModified(event, target, interval, probability);
		final String eventSymbol = event.getEvent().getSymbol();

		TreeMap<Double, PDTATransitionModified> eventTransitions = transitions.get(eventSymbol);

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

		for (final TreeMap<Double, PDTATransitionModified> event : transitions.values()) {
			for (final PDTATransitionModified transition : event.values()) {
				stringBuilder.append(transition + " ");
			}
		}

		stringBuilder.append(")");

		return stringBuilder.toString();
	}
}
