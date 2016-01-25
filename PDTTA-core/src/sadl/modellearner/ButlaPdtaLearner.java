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
package sadl.modellearner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.EventsCreationStrategy;
import sadl.constants.IntervalCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.TransitionsType;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.CompatibilityChecker;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.models.pdta.PDTA;
import sadl.models.pta.Event;
import sadl.models.pta.EventGenerator;
import sadl.models.pta.PTA;
import sadl.models.pta.PTAState;

public class ButlaPdtaLearner implements ProbabilisticModelLearner, CompatibilityChecker {
	private static Logger logger = LoggerFactory.getLogger(ButlaPdtaLearner.class);

	EventGenerator eventGenerator;
	double a;
	TransitionsType transitionsToCheck;
	PTAOrdering mergeStrategy;
	EventsCreationStrategy splittingStrategy;
	IntervalCreationStrategy intervalCreation;

	public ButlaPdtaLearner(double bandwidth, double a, TransitionsType transitionsToCheck, double anomalyProbability, double warningProbability,
			PTAOrdering mergeStrategy, EventsCreationStrategy splittingStrategy, KDEFormelVariant formelVariant, IntervalCreationStrategy intervalCreation) {

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
		this.intervalCreation = intervalCreation;
	}

	/**
	 * Same as alergia with recursion
	 * @param alpha
	 */
	public ButlaPdtaLearner(double alpha) {
		this.eventGenerator = new EventGenerator(0, 0, 0, KDEFormelVariant.OriginalKDE);
		this.splittingStrategy = EventsCreationStrategy.DontSplitEvents;
		this.a = alpha;
		this.mergeStrategy = PTAOrdering.TopDown;
		this.transitionsToCheck = TransitionsType.Incoming;
	}

	public ButlaPdtaLearner(EventsCreationStrategy splittingStrategy, KDEFormelVariant formelVariant) {
		this.eventGenerator = new EventGenerator(0, 0, 0, formelVariant);
		this.splittingStrategy = splittingStrategy;
	}

	public ButlaPdtaLearner(double bandwidth, EventsCreationStrategy splittingStrategy, KDEFormelVariant formelVariant) {
		this.eventGenerator = new EventGenerator(bandwidth, 0, 0, formelVariant);
		this.splittingStrategy = splittingStrategy;
	}

	@Override
	public PDTA train(TimedInput TimedTrainingSequences) {
		logger.debug("Starting to learn PDTA with BUTLA...");
		final HashMap<String, TDoubleList> eventToTimelistMap = mapEventsToTimes(TimedTrainingSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);

		try {
			logger.debug("Starting to build PTA ...");
			final PTA pta = new PTA(eventsMap, TimedTrainingSequences);
			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\output.gv"));
			logger.debug("Built PTA ({} states).", pta.getStates().size());
			logger.debug("Starting to merge compatible states...");
			mergeCompatibleStates(pta, pta.getStatesOrdered(mergeStrategy));

			if (splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeAfter) {
				pta.mergeTransitionsInCriticalAreas();
			}
			logger.debug("Merged compatible states.");
			// pta.toGraphvizFile(Paths.get("C:\\Private Daten\\GraphViz\\bin\\in-out.gv"));
			final PDTA pdta = pta.toPDTA(intervalCreation);
			logger.info("Learned PDTA ({} states) with BUTLA", pdta.getStateCount());
			return pdta;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param timedEventSequences
	 * Sequences of timed events.
	 * @return
	 */
	public HashMap<String, TDoubleList> mapEventsToTimes(TimedInput timedEventSequences) {
		logger.debug("Starting to gather time values...");
		final HashMap<String, TDoubleList> eventTimesMap = new HashMap<>(timedEventSequences.getSymbols().length);

		for (final TimedWord word : timedEventSequences) {
			if (!word.isAnomaly()) {
				for (int i = 0; i < word.length(); i++) {
					final String event = word.getSymbol(i);
					final double time = word.getTimeValue(i);

					TDoubleList timeList = eventTimesMap.get(event);

					if (timeList == null) {
						timeList = new TDoubleArrayList();
						eventTimesMap.put(event, timeList);
					}

					timeList.add(time);
				}
			}
		}
		logger.debug("Gathered time values.");
		return eventTimesMap;

	}

	/*
	 * public void mergeCompatibleStates(PTA pta, List<PTAState> statesOrdering) {
	 * 
	 * final ArrayList<PTAState> workedOffStates = new ArrayList<>();
	 * 
	 * outerloop: for (final PTAState state : statesOrdering) {
	 * 
	 * for (final ListIterator<PTAState> workedOffIterator = workedOffStates.listIterator(); workedOffIterator.hasNext();) {
	 * final PTAState workedOffState = workedOffIterator.next();
	 * 
	 * if (!state.exists()) {
	 * continue outerloop;
	 * }
	 * 
	 * if (!workedOffState.exists()) {
	 * workedOffIterator.remove();
	 * continue;
	 * }
	 * 
	 * if (compatible(workedOffState, state)) {
	 * logger.trace("Merging state {} and {}.", workedOffState, state);
	 * PTAState.merge(workedOffState, state, splittingStrategy);
	 * break;
	 * }
	 * 
	 * }
	 * 
	 * if (state.exists()) {
	 * workedOffStates.add(state);
	 * }
	 * }
	 * 
	 * workedOffStates.add(pta.getRoot());
	 * pta.setStates(workedOffStates);
	 * }
	 */

	public void mergeCompatibleStates(PTA pta, List<PTAState> statesOrdering) {

		// int i = 0;
		// long time1 = 0, time2 = 0, sum1 = 0, sum2 = 0, sum3 = 0;

		// int i = 0;
		// long time1 = 0, time2 = 0, sum1 = 0, sum2 = 0, sum3 = 0;

		final ArrayList<PTAState> workedOffStates = new ArrayList<>();

		outerloop: for (PTAState state : statesOrdering) {
			// time2 = System.currentTimeMillis();

			for (final ListIterator<PTAState> workedOffIterator = workedOffStates.listIterator(); workedOffIterator.hasNext();) {

				final PTAState workedOffState = workedOffIterator.next();

				if (!state.exists()) {
					state = state.isMergedWith();
				}

				if (state.isMarked()) {
					// sum3 += System.currentTimeMillis() - time2;
					continue outerloop;
				}

				if (!workedOffState.exists()) {
					workedOffIterator.remove();
					continue;
				}

				// time1 = System.currentTimeMillis();
				if (compatible(workedOffState, state)) {
					logger.trace("Merging state {} and {}.", workedOffState, state);
					// sum1 += System.currentTimeMillis() - time1;
					// time1 = System.currentTimeMillis();
					PTAState.merge(workedOffState, state, splittingStrategy);
					// sum2 += System.currentTimeMillis() - time1;
					break;
				}
				else {
					// sum1 += System.currentTimeMillis() - time1;
				}

			}

			if (state.exists()) {
				state.mark();
				workedOffStates.add(state);
			}

			// sum3 += System.currentTimeMillis() - time2;
		}

		workedOffStates.add(pta.getRoot());
		pta.setStates(workedOffStates);
	}

	public Pair<TimedInput, Map<String, Event>> splitEventsInTimedSequences(TimedInput timedSequences) {
		final HashMap<String, TDoubleList> eventToTimelistMap = mapEventsToTimes(timedSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);
		return Pair.of(getSplitInputForMapping(timedSequences, eventsMap), eventsMap);
	}

	public TimedInput getSplitInputForMapping(TimedInput timedSequences, final Map<String, Event> eventsMap) {
		final ArrayList<TimedWord> words = new ArrayList<>();

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

	public HashMap<String, Event> generateSubEvents(Map<String, TDoubleList> eventTimesMap) {
		final Set<String> eventSymbolsSet = eventTimesMap.keySet();
		final HashMap<String, Event> eventsMap = new HashMap<>(eventSymbolsSet.size());
		logger.info("There are {} events", eventSymbolsSet.size());
		logger.debug("Starting to generate subevents...");
		int subEventCount = 0;
		for (final String eventSysbol : eventSymbolsSet) {
			final TDoubleList timeList = eventTimesMap.get(eventSysbol);
			Event event = null;

			if (splittingStrategy == EventsCreationStrategy.SplitEvents) {
				event = eventGenerator.generateSplittedEvent(eventSysbol, timeList.toArray());
			} else if (splittingStrategy == EventsCreationStrategy.DontSplitEvents) {
				event = eventGenerator.generateNotSplittedEvent(eventSysbol, timeList.toArray());
			} else if (splittingStrategy == EventsCreationStrategy.NotTimedEvents) {
				event = eventGenerator.generateNotTimedEvent(eventSysbol, timeList.toArray());
			} else if (splittingStrategy == EventsCreationStrategy.IsolateCriticalAreas
					|| splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeInProcess
					|| splittingStrategy == EventsCreationStrategy.IsolateCriticalAreasMergeAfter) {
				event = eventGenerator.generateSplittedEventWithIsolatedCriticalArea(eventSysbol, timeList.toArray());
			} else {
				throw new IllegalStateException("SplittingStrategy " + splittingStrategy + " not allowed for BUTLA");
			}

			eventsMap.put(eventSysbol, event);

			if (event != null) {
				logger.debug("Splitted event {} into {} subevents.", eventSysbol, event.getSubEventsCount());
				subEventCount += event.getSubEventsCount();
				eventsMap.put(eventSysbol, event);
			}
		}
		logger.debug("Generated subevents.");
		logger.info("There are {} subevents.", subEventCount);
		return eventsMap;
	}

	@Override
	public boolean compatible(PTAState stateV, PTAState stateW) {

		if (stateV.getId() == stateW.getId()) {
			return true;
		}

		if (mergeStrategy == PTAOrdering.BottomUp && PTAState.compatibilityIsChecking(stateV, stateW)) {
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

		if (mergeStrategy == PTAOrdering.BottomUp) {
			PTAState.setCompatibilityChecking(stateV, stateW);
		}

		final Set<String> usedEvents = new HashSet<>();

		if (transitionsToCheck == TransitionsType.Incoming || transitionsToCheck == TransitionsType.Both) {
			usedEvents.addAll(stateV.getEventSymbolsInTransitions());
			usedEvents.addAll(stateW.getEventSymbolsInTransitions());
		}

		if (transitionsToCheck == TransitionsType.Outgoing || transitionsToCheck == TransitionsType.Both) {
			usedEvents.addAll(stateV.getEventSymbolsOutTransitions());
			usedEvents.addAll(stateW.getEventSymbolsOutTransitions());
		}

		// for (final Event event : stateV.getPTA().getEvents().values()) {
		// for (final SubEvent subEvent : event) {
		// final String eventSymbol = subEvent.getSymbol();
		for (final String eventSymbol : usedEvents) {
			final PTAState nextV = stateV.getNextState(eventSymbol);
			final PTAState nextW = stateW.getNextState(eventSymbol);

			if (transitionsToCheck == TransitionsType.Incoming || transitionsToCheck == TransitionsType.Both) {
				final int inTransitionEventCountV = stateV.getInTransitionsCount(eventSymbol);
				final int inTransitionEventCountW = stateW.getInTransitionsCount(eventSymbol);

				if (fractionDifferent(inTransitionCountV, inTransitionEventCountV, inTransitionCountW, inTransitionEventCountW)) {
					if (mergeStrategy == PTAOrdering.BottomUp) {
						PTAState.unsetCompatibilityChecking(stateV, stateW);
					}
					return false;
				}
			}

			if (nextV == null && nextW == null) {
				continue;
			}

			if (transitionsToCheck == TransitionsType.Outgoing || transitionsToCheck == TransitionsType.Both) {
				final int outTransitionEventCountV = stateV.getOutTransitionsCount(eventSymbol);
				final int outTransitionEventCountW = stateW.getOutTransitionsCount(eventSymbol);

				if (fractionDifferent(inTransitionCountV, outTransitionEventCountV, inTransitionCountW, outTransitionEventCountW)) {
					if (mergeStrategy == PTAOrdering.BottomUp) {
						PTAState.unsetCompatibilityChecking(stateV, stateW);
					}
					return false;
				}
			}

			if (nextV == null || nextW == null) {
				continue;
			}

			if (!compatible(nextV, nextW)) {
				if (mergeStrategy == PTAOrdering.BottomUp) {
					PTAState.unsetCompatibilityChecking(stateV, stateW);
				}
				return false;
			}

			// }
		}

		if (mergeStrategy == PTAOrdering.BottomUp) {
			PTAState.unsetCompatibilityChecking(stateV, stateW);
		}
		return true;

	}

	public boolean fractionDifferent(int n0, int f0, int n1, int f1) {

		return Math.abs(((double) f0 / n0) - ((double) f1 / n1)) > (Math.sqrt(0.5 * Math.log(2.0 / a)) * ((1.0 / Math.sqrt(n0)) + (1.0 / Math.sqrt(n1))));
	}

}
