/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.models.pdta;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;

import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.models.PDFA;
import sadl.models.pta.Event;
import sadl.models.pta.HalfClosedInterval;
import sadl.models.pta.SubEvent;

public class PDTA implements AutomatonModel {

	PDTAState root;
	TIntObjectMap<PDTAState> states;
	HashMap<String, Event> events;

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {

		final TDoubleList probabilities1 = new TDoubleArrayList(s.length());
		final TDoubleList probabilities2 = new TDoubleArrayList(s.length());

		PDTAState currentState = root;

		for (int i = 0; i < s.length(); i++) {
			final String eventSymbol = s.getSymbol(i);
			final double time = s.getTimeValue(i);

			final PDTATransition currentTransition = currentState.getTransition(eventSymbol, time);

			if (currentTransition == null) {
				probabilities1.add(0.0);
				probabilities2.add(0.0);
				return new Pair<>(probabilities1, probabilities2);
			}

			probabilities1.add(currentTransition.getPropability());
			probabilities2.add(currentTransition.getEvent().calculateProbability(time));
			currentState = currentTransition.getTarget();
		}

		probabilities1.add(currentState.getEndProbability());
		return new Pair<>(probabilities1, probabilities2);
	}

	public PDTA(PDTAState root, TIntObjectMap<PDTAState> states, HashMap<String, Event> events) {
		this.root = root;
		this.states = states;
		this.events = events;
	}

	public PDTAState getRoot() {

		return root;
	}

	public TIntObjectMap<PDTAState> getStates() {

		return states;
	}

	public TimedInput generateRandomSequences(boolean allowAnomaly, int count) {

		final ArrayList<TimedWord> words = new ArrayList<>();

		for (int i = 0; i < count; i++) {

			words.add(generateRandomWord(allowAnomaly));
		}

		return new TimedInput(words);

	}

	public TimedInput generateAnomalySequences(int eventAnomaliesCount, int count) {

		final ArrayList<TimedWord> words = new ArrayList<>();

		for (int i = 0; i < count; i++) {

			words.add(generateAnomalyWord(eventAnomaliesCount));
		}

		return new TimedInput(words);

	}

	public TimedWord generateRandomWord(boolean allowAnomaly) {

		final ArrayList<String> symbols = new ArrayList<>();
		final TIntLinkedList timeValues = new TIntLinkedList();

		PDTAState currentState = root;

		while (currentState != null) {

			final PDTATransition nextTransition = currentState.getRandomTransition();

			if (nextTransition != null) {
				final SubEvent event = nextTransition.getEvent();
				final String eventSymbol = event.getEvent().getSymbol();
				HalfClosedInterval allowedInterval;

				if (allowAnomaly) {
					allowedInterval = new HalfClosedInterval(0.0, Double.POSITIVE_INFINITY);
				} else {
					allowedInterval = nextTransition.getInterval();
				}

				final double time = event.generateRandomTime(allowedInterval);
				symbols.add(eventSymbol);
				timeValues.add((int) time);

				currentState = nextTransition.getTarget();
			} else {
				currentState = null;
			}
		}

		return new TimedWord(symbols, timeValues, ClassLabel.NORMAL);

	}

	public TimedWord generateAnomalyWord(int anomaliesMaxCount) {

		final TimedWord word = generateRandomWord(false);
		final boolean anomalyPositions[] = new boolean[word.length()];

		if (anomaliesMaxCount > anomalyPositions.length) {
			anomaliesMaxCount = anomalyPositions.length;
		}

		final Random random = new Random();
		final Event eventsArray[] = events.values().toArray(new Event[0]);

		final ArrayList<String> symbols = new ArrayList<>();
		final TIntLinkedList timeValues = new TIntLinkedList();

		while (anomaliesMaxCount > 0){
			final int position = random.nextInt(word.length());

			if (anomalyPositions[position] == false){
				anomalyPositions[position] = true;
				anomaliesMaxCount--;
			}
		}

		for (int i = 0; i < word.length(); i++){
			if (anomalyPositions[i] == false){
				symbols.add(word.getSymbol(i));
				timeValues.add(word.getTimeValue(i));
			}
			else{
				final Event randomEvent = eventsArray[random.nextInt(eventsArray.length)];
				final SubEvent randomSubEvent = randomEvent.getRandomSubEvent();
				symbols.add(randomSubEvent.getEvent().getSymbol());
				timeValues.add((int) randomSubEvent.generateRandomTime(randomSubEvent.getAnomalyBounds()));
			}
		}

		final TimedWord wordAnomaly = new TimedWord(symbols, timeValues, ClassLabel.ANOMALY);

		if (this.hasAnomaly(wordAnomaly)) {
			return wordAnomaly;
		} else {
			return generateAnomalyWord(anomaliesMaxCount);
		}
	}

	public boolean hasAnomaly(TimedWord word) {

		PDTAState currentState = root;

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDTATransition transition = currentState.getTransition(eventSymbol, time);

			if (transition == null) {
				// System.out.println("ERROR: (" + currentState.getId() + ")");
				return true;
			}

			final SubEvent event = transition.getEvent();

			if (event.hasWarning(time)) {
				// System.out.println("WARNING: time in warning arrea. (" + currentState.getId() + ")");
			}

			if (event.isInCriticalArea(time)) {
				// System.out.println("WARNING: time in critical area. Wrong decision possible. (" + currentState.getId() + ")");
			}

			currentState = transition.getTarget();
		}

		if (!currentState.isFinalState()) {
			// System.out.println("ERROR: ended not in final state. (" + currentState.getId() + ")");
			return true;
		}

		return false;
	}

	public void toGraphvizFile(Path resultPath) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8)) {
			writer.write("digraph G {\n");

			// write states
			for (final PDTAState state : states.valueCollection()) {

				writer.write(Integer.toString(state.getId()));
				writer.write(" [shape=");

				if (state.isFinalState()) {
					writer.write("double");
				}

				writer.write("circle, label=\"" + Integer.toString(state.getId()) + "\"");
				writer.write("]\n");
			}

			for (final PDTAState state : states.valueCollection()) {
				for (final TreeMap<Double, PDTATransition> transitions : state.getTransitions().values()) {
					for (final PDTATransition transition : transitions.values()) {
						writer.write(Integer.toString(state.getId()) + "->" + Integer.toString(transition.getTarget().getId()) + " [label=<"
								+ transition.getEvent().getSymbol() + ">;];\n");
					}
				}
			}

			writer.write("}");
		}
	}

	@Override
	public int getStateCount() {
		return states.size();
	}

	@Override
	public int getTransitionCount() {
		int result = 0;
		for (final PDTAState state : states.valueCollection()) {
			for (final TreeMap<Double, PDTATransition> transitions : state.getTransitions().values()) {
				result += transitions.size();
			}
		}
		return result;
	}

	public PDFA toPDFA() {
		// TODO implement
		return null;
	}

}
