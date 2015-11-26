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

package sadl.modellearner;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.TransitionsType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.CompatibilityChecker;
import sadl.interfaces.ModelLearner;
import sadl.models.pdta.PDTA;
import sadl.models.pta.Event;
import sadl.models.pta.EventGenerator;
import sadl.models.pta.PTA;
import sadl.models.pta.PTAState;
import sadl.models.pta.SubEvent;

public class ButlaPdtaLearner implements ModelLearner, CompatibilityChecker {

	EventGenerator eventGenerator;
	double a;
	TransitionsType transitionsToCheck;
	PTAOrdering mergeStrategy;
	EventsCreationStrategy splittingStrategy;

	public ButlaPdtaLearner(double bandwidth, double a, TransitionsType transitionsToCheck, double anomalyProbability, double warningProbability,
			PTAOrdering mergeStrategy, EventsCreationStrategy splittingStrategy, KDEFormelVariant formelVariant) {

		if (Double.isNaN(a) || a >= 1.0d || a <= 0.0d) {
			throw new IllegalArgumentException("a has to be between 0.0 and 1.0 excluded.");
		}

		if (Double.isNaN(anomalyProbability) || anomalyProbability >= 1.0d || anomalyProbability <= 0.0d) {
			throw new IllegalArgumentException("Wrong parameter: anomalyProbability.");
		}

		if (Double.isNaN(warningProbability) || warningProbability < anomalyProbability) {
			throw new IllegalArgumentException("Wrong parameter: warningProbability.");
		}

		if (transitionsToCheck == null || mergeStrategy == null || splittingStrategy == null || formelVariant == null) {
			throw new IllegalArgumentException();
		}


		this.eventGenerator = new EventGenerator(bandwidth, anomalyProbability, warningProbability, formelVariant);
		this.a = a;
		this.transitionsToCheck = transitionsToCheck;
		this.mergeStrategy = mergeStrategy;
		this.splittingStrategy = splittingStrategy;
	}

	@Override
	public PDTA train(TimedInput TimedTrainingSequences) {

		final HashMap<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(TimedTrainingSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);

		try {
			final PTA pta = new PTA(eventsMap, TimedTrainingSequences);
			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\output.gv"));

			mergeCompatibleStates(pta, pta.getStatesOrdered(mergeStrategy));

			if (splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeAfter) {
				pta.mergeTransitionsInCriticalAreas();
			}

			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\in-out.gv"));
			return pta.toPDTA();
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

	public void mergeCompatibleStates(PTA pta, List<PTAState> statesOrdering) {

		final LinkedList<PTAState> workedOffStates = new LinkedList<>();

		outerloop: for (final PTAState state : statesOrdering) {

			for (final ListIterator<PTAState> workedOffIterator = workedOffStates.listIterator(); workedOffIterator.hasNext();) {
				final PTAState workedOffState = workedOffIterator.next();

				if (!state.exists()) {
					continue outerloop;
				}

				if (!workedOffState.exists()) {
					workedOffIterator.remove();
					continue;
				}

				if (compatible(workedOffState, state)) {
					PTAState.merge(workedOffState, state, splittingStrategy);
					break;
				}

			}

			if (state.exists()) {
				workedOffStates.add(state);
			}
		}


		workedOffStates.add(pta.getRoot());
		pta.setStates(workedOffStates);
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

			words.add(new TimedWord(symbols, timeValues, word.getLabel()));
		}

		return new TimedInput(words);
	}

	public HashMap<String, Event> generateSubEvents(Map<String, LinkedList<Double>> eventTimesMap) {

		final Set<String> eventSymbolsSet = eventTimesMap.keySet();
		final HashMap<String, Event> eventsMap = new HashMap<>(eventSymbolsSet.size());

		for (final String eventSysbol : eventSymbolsSet) {
			final List<Double> timeList = eventTimesMap.get(eventSysbol);
			Event event = null;

			if (splittingStrategy == EventsCreationStrategy.SplitEvents) {
				event = eventGenerator.generateSplittedEvent(eventSysbol, listToDoubleArray(timeList));
			} else if (splittingStrategy == EventsCreationStrategy.DontSplitEvents) {
				event = eventGenerator.generateNotSplittedEvent(eventSysbol, listToDoubleArray(timeList));
			} else if (splittingStrategy == EventsCreationStrategy.NotTimedEvents) {
				event = eventGenerator.generateNotTimedEvent(eventSysbol, listToDoubleArray(timeList));
			} else if (splittingStrategy == EventsCreationStrategy.IsolateCriticalAreas
					|| splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeInProcess
					|| splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeAfter) {
				event = eventGenerator.generateSplittedEventWithIsolatedCriticalArea(eventSysbol, listToDoubleArray(timeList));
			}

			eventsMap.put(eventSysbol, event);
			System.out.println("Created event: " + event);
		}

		return eventsMap;
	}

	@Override
	public boolean compatible(PTAState stateV, PTAState stateW) {

		if (stateV == stateW) {
			return true;
		}

		if (PTAState.compatibilityIsChecking(stateV, stateW)) {
			return true;
		}

		final int inTransitionCountV = stateV.getInTransitionsCount();
		final int inTransitionCountW = stateW.getInTransitionsCount();
		final int outTransitionCountV = stateV.getOutTransitionsCount();
		final int outTransitionCountW = stateW.getOutTransitionsCount();
		final int endTansitionCountV = inTransitionCountV - outTransitionCountV;
		final int endTansitionCountW = inTransitionCountW - outTransitionCountW;

		if (fractionDifferent(inTransitionCountV, endTansitionCountV, inTransitionCountW, endTansitionCountW)) {
			return false;
		}

		PTAState.setCompatibilityChecking(stateV, stateW);

		for (final Event event : stateV.getPTA().getEvents().values()) {
			for (final SubEvent subEvent : event) {
				final String eventSymbol = subEvent.getSymbol();

				final int inTransitionEventCountV = stateV.getInTransitionsCount(eventSymbol);
				final int inTransitionEventCountW = stateW.getInTransitionsCount(eventSymbol);
				final int outTransitionEventCountV = stateV.getOutTransitionsCount(eventSymbol);
				final int outTransitionEventCountW = stateW.getOutTransitionsCount(eventSymbol);

				if ((transitionsToCheck == TransitionsType.Incoming || transitionsToCheck == TransitionsType.Both)
						&& fractionDifferent(inTransitionCountV, inTransitionEventCountV, inTransitionCountW, inTransitionEventCountW)) {
					PTAState.unsetCompatibilityChecking(stateV, stateW);
					return false;
				}

				if ((transitionsToCheck == TransitionsType.Outgoing || transitionsToCheck == TransitionsType.Both)
						&& fractionDifferent(inTransitionCountV, outTransitionEventCountV, inTransitionCountW, outTransitionEventCountW)) {
					PTAState.unsetCompatibilityChecking(stateV, stateW);
					return false;
				}

				final PTAState nextV = stateV.getNextState(eventSymbol);
				final PTAState nextW = stateW.getNextState(eventSymbol);

				if (nextV == null || nextW == null) {
					continue;
				}

				if (!compatible(nextV, nextW)) {
					PTAState.unsetCompatibilityChecking(stateV, stateW);
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

	public double[] listToDoubleArray(List<Double> list) {

		final double[] array = new double[list.size()];
		int i = 0;

		for (final Double element : list) {
			array[i++] = element.doubleValue();
		}

		return array;
	}

}
