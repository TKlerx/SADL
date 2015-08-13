package sadl.modellearner;

import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.ModelLearner;
import sadl.models.PTA.Event;
import sadl.models.PTA.PTA;
import sadl.models.pdrta.PDRTA;

public class ButlaPdrtaLearner implements ModelLearner {
	/**
	 * 
	 * 
	 * 
	 * 
	 */

	@Override
	public PDRTA train(TimedInput TimedTrainingSequences) {

		final Map<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(TimedTrainingSequences);
		final Map<String, Event> eventsMap = generateSplittedEvents(eventToTimelistMap);

		final PTA pta;

		try {
			pta = new PTA(eventsMap, TimedTrainingSequences);
		} catch (final UnexpectedException e) {
			System.out.println(e.getMessage());
			return null;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}

		pta.mergeCompatibleStates();

		return null;
	}

	/**
	 * 
	 * @param timedEventSequences
	 *            Sequences of timed events.
	 * @return
	 */
	public Map<String, LinkedList<Double>> mapEventsToTimes(TimedInput timedEventSequences) {

		final Map<String, LinkedList<Double>> eventTimesMap = new HashMap<>(timedEventSequences.getSymbols().length);

		for (final TimedWord word : timedEventSequences) {
			for (int i = 0; i < word.length(); i++) {
				final String event = word.getSymbol(i);
				LinkedList<Double> timeList = eventTimesMap.get(event);
				final double time = word.getTimeValue(i);

				if (timeList == null) {
					timeList = new LinkedList<>();
					eventTimesMap.put(event, timeList);
				}

				timeList.add(time);
			}
		}

		return eventTimesMap;

	}

	public void splitEventsInTimedSequences(TimedInput timedSequences) {

		final Map<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(timedSequences);
		final Map<String, Event> eventsMap = generateSplittedEvents(eventToTimelistMap);

		for (final TimedWord word : timedSequences) {
			for (int i = 0; i < word.length(); i++) {
				final String eventSymbol = word.getSymbol(i);
				final double time = word.getTimeValue(i);
				eventsMap.get(eventSymbol).getSplittedEventFromTime(time).toString(); // TODO
			}
		}
	}

	public Map<String, Event> generateSplittedEvents(Map<String, LinkedList<Double>> eventTimesMap) {

		final Set<String> eventSymbolsSet = eventTimesMap.keySet();
		final Map<String, Event> eventsMap = new HashMap<>(eventSymbolsSet.size());

		for (final String eventSysbol : eventSymbolsSet) {
			final List<Double> timeList = eventTimesMap.get(eventSysbol);
			eventsMap.put(eventSysbol, Event.generateEvent(eventSysbol, listToDoubleArray(timeList)));
		}

		return eventsMap;
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
