package sadl.modellearner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.ModelLearner;
import sadl.models.PTA.Event;
import sadl.models.pdrta.PDRTA;

public class ButlaPdrtaLearner implements ModelLearner {

	@Override
	public PDRTA train(TimedInput trainingSequences) {

		final Map<String, LinkedList<Double>> symbolTimes = new HashMap<>();
		final Event[] events;

		for (final TimedWord word : trainingSequences) {
			for (int i = 0; i < word.length(); i++) {
				LinkedList<Double> timeList = symbolTimes.get(word.getSymbol(i));

				if (timeList != null) {
					timeList.add((double) word.getTimeValue(i)); // TODO Why int?
				} else {
					timeList = new LinkedList<>();
					timeList.add((double) word.getTimeValue(i));
					symbolTimes.put(word.getSymbol(i), timeList);
				}
			}
		}

		final Set<String> symbolsSet = symbolTimes.keySet();
		events = new Event[symbolsSet.size()];
		int i = 0;

		for (final String symbol : symbolsSet) {
			final List<Double> timeList = symbolTimes.get(symbol);// TODO toArray and New ArrayList size check
			events[i] = Event.generateEvent(symbol, listToDoubleArray(timeList));
			i++;
		}

		return null;
	}

	// TODO move?
	private double[] listToDoubleArray(List<Double> list) {

		final double[] array = new double[list.size()];
		int i = 0;

		for (final Double element : list) {
			array[i] = element.doubleValue();
			i++;
		}

		return array;
	}

}
