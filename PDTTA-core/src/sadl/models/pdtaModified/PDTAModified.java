package sadl.models.pdtaModified;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TIntLinkedList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;

import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.interfaces.Model;
import sadl.models.PTA.Event;
import sadl.models.PTA.SubEvent;

public class PDTAModified implements AutomatonModel, Model {

	PDTAStateModified root;
	HashMap<Integer, PDTAStateModified> states;
	HashMap<String, Event> events;

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {

		final TDoubleList probabilities1 = new TDoubleArrayList(s.length());
		final TDoubleList probabilities2 = new TDoubleArrayList(s.length());

		final PDTAStateModified currentState = root;

		for (int i = 0; i < s.length(); i++) {
			final String eventSymbol = s.getSymbol(i);
			final double time = s.getTimeValue(i);

			final PDTATransitionModified currentTransition = currentState.getMostProbablyTransition(eventSymbol, time);

			if (currentTransition == null) {
				probabilities1.add(0.0);
				probabilities2.add(0.0);
				break;
			}

			probabilities1.add(currentTransition.getPropability());
			probabilities2.add(currentTransition.getEvent().calculateProbability(time));
		}

		return new Pair<>(probabilities1, probabilities2);
	}

	public PDTAModified(PDTAStateModified root, HashMap<Integer, PDTAStateModified> states, HashMap<String, Event> events) {
		this.root = root;
		this.states = states;
		this.events = events;
	}

	public PDTAStateModified getRoot() {

		return root;
	}

	public TimedInput generateRandomSequences(boolean allowAnomaly, int count) {

		final LinkedList<TimedWord> words = new LinkedList<>();

		for (int i = 0; i < count; i++) {

			words.add(generateRandomWord(allowAnomaly));
		}

		return new TimedInput(words);

	}

	public TimedInput generateAnomalySequences(int eventAnomaliesCount, int count) {

		final LinkedList<TimedWord> words = new LinkedList<>();

		for (int i = 0; i < count; i++) {

			words.add(generateAnomalyWord(eventAnomaliesCount));
		}

		return new TimedInput(words);

	}

	public TimedWord generateRandomWord(boolean allowAnomaly) {

		final LinkedList<String> symbols = new LinkedList<>();
		final TIntLinkedList timeValues = new TIntLinkedList();

		PDTAStateModified currentState = root;

		while (currentState != null) {

			final PDTATransitionModified nextTransition = currentState.getRandomTransition();

			if (nextTransition != null) {
				final SubEvent event = nextTransition.getEvent();
				final String eventSymbol = event.getEvent().getSymbol();
				final double time = event.generateRandomTime(allowAnomaly);
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

		final LinkedList<String> symbols = new LinkedList<>();
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
				timeValues.add((int) randomSubEvent.generateRandomTime(false));
			}
		}

		final TimedWord wordAnomaly = new TimedWord(symbols, timeValues, ClassLabel.ANOMALY);

		if (this.hasAnomalie(wordAnomaly)) {
			return wordAnomaly;
		} else {
			return generateAnomalyWord(anomaliesMaxCount);
		}
	}

	public boolean hasAnomalie(TimedWord word) {

		PDTAStateModified currentState = root;

		for (int i = 0; i < word.length(); i++) {
			final String eventSymbol = word.getSymbol(i);
			final double time = word.getTimeValue(i);

			final PDTATransitionModified transition = currentState.getTransition(eventSymbol, time);

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
		final BufferedWriter writer = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8);
		writer.write("digraph G {\n");

		// write states
		for (final PDTAStateModified state : states.values()) {

			writer.write(Integer.toString(state.getId()));
			writer.write(" [shape=");

			if (state.isFinalState()) {
				writer.write("double");
			}

			writer.write("circle, label=\"" + Integer.toString(state.getId()) + "\"");
			writer.write("]\n");
		}

		for (final PDTAStateModified state : states.values()) {
			for (final TreeMap<Double, PDTATransitionModified> transitions : state.getTransitions().values()) {
				for (final PDTATransitionModified transition : transitions.values()) {
					writer.write(Integer.toString(state.getId()) + "->" + Integer.toString(transition.getTarget().getId()) + " [label=<"
							+ transition.getEvent().getSymbol() + ">;];\n");
				}
			}
		}

		writer.write("}");
		writer.close();
	}
}
