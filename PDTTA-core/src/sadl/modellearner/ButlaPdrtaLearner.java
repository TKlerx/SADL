package sadl.modellearner;

import gnu.trove.list.array.TIntArrayList;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.ModelLearner;
import sadl.models.PTA.Event;
import sadl.models.PTA.PTA;
import sadl.models.pdrtaModified.PDRTAModified;

public class ButlaPdrtaLearner implements ModelLearner {
	/**
	 * @throws Exception
	 * 
	 * 
	 * 
	 * 
	 */

	@Override
	public PDRTAModified train(TimedInput TimedTrainingSequences) {

		final HashMap<String, LinkedList<Double>> eventToTimelistMap = mapEventsToTimes(TimedTrainingSequences);
		final HashMap<String, Event> eventsMap = generateSubEvents(eventToTimelistMap);

		final PTA pta;

		try {
			pta = new PTA(eventsMap, TimedTrainingSequences);
			pta.mergeCompatibleStates();
			return pta.toPDRTA();
		} catch (final UnexpectedException e) {
			System.out.println(e.getMessage());
			return null;
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
