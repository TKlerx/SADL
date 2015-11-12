package sadl.modellearner;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sadl.constants.ClassLabel;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.MergeStrategy;
import sadl.constants.TransitionsType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.CompatibilityChecker;
import sadl.interfaces.ModelLearner;
import sadl.models.PTA.Event;
import sadl.models.PTA.EventGenerator;
import sadl.models.PTA.PTA;
import sadl.models.PTA.PTAState;
import sadl.models.PTA.SubEvent;
import sadl.models.pdtaModified.PDTAModified;

public class ButlaPdrtaLearner implements ModelLearner, CompatibilityChecker {

	EventGenerator eventGenerator;
	double a;
	TransitionsType transitionsToCheck;
	MergeStrategy mergeStrategy;
	EventsCreationStrategy splittingStrategy;

	public ButlaPdrtaLearner(double bandwidth, double a, TransitionsType check, double anomalyProbability, double warningProbability,
			MergeStrategy mergeStrategy, EventsCreationStrategy splittingStrategy, KDEFormelVariant formelVariant) {

		if (Double.isNaN(a) || a >= 1.0d || a <= 0.0d) {
			throw new IllegalArgumentException("a has to be between 0.0 and 1.0 excluded.");
		}

		if (Double.isNaN(anomalyProbability) || anomalyProbability >= 1.0d || anomalyProbability <= 0.0d) {
			throw new IllegalArgumentException("Wrong parameter: anomalyProbability.");
		}

		if (Double.isNaN(warningProbability) || warningProbability < anomalyProbability) {
			throw new IllegalArgumentException("Wrong parameter: warningProbability.");
		}

		this.eventGenerator = new EventGenerator(bandwidth, anomalyProbability, warningProbability, formelVariant);
		this.a = a;
		this.transitionsToCheck = check;
		this.mergeStrategy = mergeStrategy;
		this.splittingStrategy = splittingStrategy;
	}

	@Override
	public PDTAModified train(TimedInput TimedTrainingSequences) {

		final HashMap<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(TimedTrainingSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);

		try {
			final PTA pta = new PTA(eventsMap, TimedTrainingSequences);
			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\output.gv"));
			if (mergeStrategy == MergeStrategy.TopDown) {
				pta.mergeStatesTopDown(this, splittingStrategy);
			} else if (mergeStrategy == MergeStrategy.BottonUp) {
				pta.mergeStatesBottomUp(this, splittingStrategy);
			}

			if (splittingStrategy == EventsCreationStrategy.SplitEventsIsolateCriticalAreasMergeAfter) {
				pta.mergeTransitionsInCriticalAreas();
			}

			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\in-out.gv"));
			return pta.toPDRTA();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param timedEventSequences
	 *            Sequences of timed events.
	 * @return
	 */
	public HashMap<String, LinkedList<Double>> mapEventsToTimes(TimedInput timedEventSequences) {

		final HashMap<String, LinkedList<Double>> eventTimesMap = new HashMap<>(timedEventSequences.getSymbols().length);

		for (final TimedWord word : timedEventSequences) {
			for (int i = 0; i < word.length(); i++) {
				final String event = word.getSymbol(i);
				final double time = word.getTimeValue(i);

				LinkedList<Double> timeList = eventTimesMap.get(event);

				if (timeList == null) {
					timeList = new LinkedList<>();
					eventTimesMap.put(event, timeList);
				}

				timeList.add(time);
			}
		}

		return eventTimesMap;

	}

	public TimedInput splitEventsInTimedSequences(TimedInput timedSequences) {

		final HashMap<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(timedSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);

		final LinkedList<TimedWord> words = new LinkedList<>();

		for (final TimedWord word : timedSequences) {

			final ArrayList<String> symbols = new ArrayList<>();
			final TIntArrayList timeValues = new TIntArrayList();

			for (int i = 0; i < word.length(); i++) {
				final String eventSymbol = word.getSymbol(i);
				final double time = word.getTimeValue(i);
				final String subEventSymbol = eventsMap.get(eventSymbol).getSubEventByTime(time).getSymbol();

				symbols.add(subEventSymbol);
				timeValues.add((int) time);
			}

			words.add(new TimedWord(symbols, timeValues, ClassLabel.NORMAL));
		}

		return new TimedInput(words);
	}

	public HashMap<String, Event> generateSubEvents(Map<String, LinkedList<Double>> eventTimesMap) {

		final Set<String> eventSymbolsSet = eventTimesMap.keySet();
		final HashMap<String, Event> eventsMap = new HashMap<>(eventSymbolsSet.size());

		for (final String eventSysbol : eventSymbolsSet) {
			final List<Double> timeList = eventTimesMap.get(eventSysbol);

			if (splittingStrategy == EventsCreationStrategy.SplitEvents) {
				eventsMap.put(eventSysbol, eventGenerator.generateSplittedEvent(eventSysbol, listToDoubleArray(timeList)));
			} else if (splittingStrategy == EventsCreationStrategy.DontSplitEvents) {
				eventsMap.put(eventSysbol, eventGenerator.generateNotSplittedEvent(eventSysbol, listToDoubleArray(timeList)));
			} else if (splittingStrategy == EventsCreationStrategy.SplitEventsIsolateCriticalAreasMergeInProcess
					|| splittingStrategy == EventsCreationStrategy.SplitEventsIsolateCriticalAreasMergeAfter) {
				eventsMap.put(eventSysbol, eventGenerator.generateSplittedEventWithIsolatedCriticalArea(eventSysbol, listToDoubleArray(timeList)));
			}
		}

		return eventsMap;
	}

	@Override
	public boolean compatible(PTAState stateV, PTAState stateW) {

		if (stateV.isRemoved()) {
			stateV = stateV.isMergedWith();
		}

		if (stateW.isRemoved()) {
			stateW = stateW.isMergedWith();
		}

		if (stateV == stateW) {
			return true;
		}

		if (PTAState.compatibilityIsChecking(stateV, stateW)) {
			return true;
		}

		PTAState.setCompatibilityChecking(stateV, stateW);

		final int inTransitionCountV = stateV.getInTransitionsCount();
		final int inTransitionCountW = stateW.getInTransitionsCount();
		final int outTransitionCountV = stateV.getOutTransitionsCount();
		final int outTransitionCountW = stateW.getOutTransitionsCount();
		final int endTansitionCountV = inTransitionCountV - outTransitionCountV;
		final int endTansitionCountW = inTransitionCountW - outTransitionCountW;

		if (fractionDifferent(inTransitionCountV, endTansitionCountV, inTransitionCountW, endTansitionCountW)) {
			return false;
		}

		for (final Event event : stateV.getPTA().getEvents().values()) {
			for (final SubEvent subEvent : event) {
				final String eventSymbol = subEvent.getSymbol();

				final int inTransitionEventCountV = stateV.getInTransitionsCount(eventSymbol);
				final int inTransitionEventCountW = stateW.getInTransitionsCount(eventSymbol);
				final int outTransitionEventCountV = stateV.getOutTransitionsCount(eventSymbol);
				final int outTransitionEventCountW = stateW.getOutTransitionsCount(eventSymbol);

				if ((transitionsToCheck == TransitionsType.Incoming || transitionsToCheck == TransitionsType.Both)
						&& fractionDifferent(inTransitionCountV, inTransitionEventCountV, inTransitionCountW, inTransitionEventCountW)) {
					return false;
				}

				if ((transitionsToCheck == TransitionsType.Outgoing || transitionsToCheck == TransitionsType.Both)
						&& fractionDifferent(inTransitionCountV, outTransitionEventCountV, inTransitionCountW, outTransitionEventCountW)) {
					return false;
				}

				final PTAState nextV = stateV.getNextState(eventSymbol);
				final PTAState nextW = stateW.getNextState(eventSymbol);

				if (nextV == null || nextW == null) {
					continue;
				}

				if (!compatible(nextV, nextW)) {
					return false;
				}

			}
		}

		PTAState.unsetCompatibilityChecking(stateV, stateW);

		return true;

	}

	public boolean fractionDifferent(int n0, int f0, int n1, int f1) {

		return Math.abs(((double) f0 / n0) - ((double) f1 / n1)) > (Math.sqrt(0.5 * Math.log(2.0 / a)) * ((1.0 / Math.sqrt(n0)) + (1.0 / Math.sqrt(n1))));
	}

	private double[] listToDoubleArray(List<Double> list) {

		final double[] array = new double[list.size()];
		int i = 0;

		for (final Double element : list) {
			array[i++] = element.doubleValue();
		}

		return array;
	}

}
