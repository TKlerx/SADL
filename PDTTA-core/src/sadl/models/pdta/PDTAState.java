/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.models.pdta;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import sadl.models.pta.HalfClosedInterval;
import sadl.models.pta.SubEvent;
import sadl.utils.MasterSeed;

public class PDTAState {

	protected int id;
	protected double endProbability;
	protected HashMap<String, TreeMap<Double, PDTATransition>> transitions = new HashMap<>();
	protected TreeMap<Double, PDTATransition> transitionsProbability = new TreeMap<>();

	protected double sumProbabilities = 0.0d;
	Random rand;

	public PDTAState(int id, double endProbability) {
		rand = MasterSeed.nextRandom();
		if (Double.isNaN(endProbability) || endProbability < 0.0d || endProbability > 1.0d) {
			throw new IllegalArgumentException("Wrong parameter endProbability: " + endProbability);
		}

		this.id = id;
		this.endProbability = endProbability;
		this.sumProbabilities = endProbability;
	}

	public PDTAState getNextState(String eventSymbol, double time) {

		final PDTATransition transition = this.getTransition(eventSymbol, time);

		if (transition != null && transition.inInterval(time)) {
			return transition.getTarget();
		}

		return null;
	}

	public HashMap<String, TreeMap<Double, PDTATransition>> getTransitions() {

		return transitions;
	}

	public PDTATransition getTransition(String eventSymbol, double time) {

		final TreeMap<Double, PDTATransition> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			return null;
		}

		final Entry<Double, PDTATransition> transitionEntry = eventTransitions.floorEntry(time);

		if (transitionEntry == null) {
			return null;
		}

		final PDTATransition transition = transitionEntry.getValue();

		if (transition.inInterval(time)) {
			return transition;
		}

		return null;
	}

	public PDTATransition getMostProbablyTransition(String eventSymbol, double time) {

		PDTATransition probablyTransition = getTransition(eventSymbol, time);

		if (probablyTransition == null) {
			final TreeMap<Double, PDTATransition> eventTransitions = transitions.get(eventSymbol);

			if (eventTransitions == null) {
				return null;
			}

			double maxProbability = 0.0;

			for (final PDTATransition transition : eventTransitions.values()){

				final double probability = transition.getEvent().calculateProbability(time);

				if (probability > maxProbability) {
					maxProbability = probability;
					probablyTransition = transition;
				}
			}
		}

		return probablyTransition;
	}

	public PDTATransition getRandomTransition() {

		if (sumProbabilities < 1.0d) {
			throw new IllegalStateException("Probability not 1.0");
		}

		final double random = rand.nextDouble();

		if (random <= endProbability) {
			return null;
		}

		final Entry<Double, PDTATransition> transitionEntry = transitionsProbability.floorEntry(random);

		if (transitionEntry == null) {
			throw new IllegalStateException("No transition selected(" + random + ")" + this);
		}

		final PDTATransition transition = transitionEntry.getValue();

		if (transitionEntry.getKey() + transition.getPropability() < random) {
			throw new IllegalStateException("No transition selected.");
		}

		return transition;
	}

	public void addTransition(SubEvent event, PDTAState target, HalfClosedInterval interval, double probability) {

		final PDTATransition transition = new PDTATransition(event, target, interval, probability);
		final String eventSymbol = event.getEvent().getSymbol();

		TreeMap<Double, PDTATransition> eventTransitions = transitions.get(eventSymbol);

		if (eventTransitions == null) {
			eventTransitions = new TreeMap<>();
			transitions.put(eventSymbol, eventTransitions);
		}

		eventTransitions.put(interval.getMinimum(), transition);
		transitionsProbability.put(sumProbabilities, transition);
		sumProbabilities += probability;

		if (sumProbabilities > 1.0001d) {
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

		for (final TreeMap<Double, PDTATransition> event : transitions.values()) {
			for (final PDTATransition transition : event.values()) {
				stringBuilder.append(transition + " ");
			}
		}

		stringBuilder.append(")");

		return stringBuilder.toString();
	}
}
