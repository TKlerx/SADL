package de.upb.fwitter.input;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import de.upb.timok.constants.ClassLabel;

/**
 * Class that encapsulates a timed sequence
 * 
 * @author Fabian Witter
 *
 */
public class TimedWord {

	private TIntList timeValues;
	private List<String> symbols;
	private ClassLabel label;

	TimedWord() {

		timeValues = new TIntArrayList();
		symbols = new ArrayList<String>();
		label = ClassLabel.NORMAL;
	}

	TimedWord(ClassLabel l) {

		timeValues = new TIntArrayList();
		symbols = new ArrayList<String>();
		label = l;
	}

	void appendPair(String symbol, int timeDelay) {

		symbols.add(symbol);
		timeValues.add(timeDelay);
	}

	/**
	 * Sets the {@link ClassLabel} of the {@link TimedWord} to the given one.
	 * 
	 * @param l
	 *            The class label to be set
	 */
	public void setLabel(ClassLabel l) {
		label = l;
	}

	/**
	 * Returns the symbol {@link String} at the given index of the
	 * {@link TimedWord}.
	 * 
	 * @param i
	 *            The index to get the symbol for
	 * @return The symbol at the given index or {@link null} if the index does
	 *         not exist
	 */
	public String getSymbol(int i) {

		if (i < symbols.size() && i >= 0) {
			return symbols.get(i);
		}
		return null;
	}

	/**
	 * Returns the time delay at the given index of the {@link TimedWord}.
	 * 
	 * @param i
	 *            The index to get the time delay for
	 * @return The time delay at the given index or {@code -1} if the index does
	 *         not exist
	 */
	public int getTimeValue(int i) {

		if (i < timeValues.size() && i >= 0) {
			return timeValues.get(i);
		}
		return -1;
	}

	/**
	 * Returns the {@link ClassLabel} of the {@link TimedWord}.
	 * 
	 * @return The class label
	 */
	public ClassLabel getLabel() {
		return label;
	}

	/**
	 * States whether the {@link TimedWord} is an anomaly according to the
	 * {@link ClassLabel}.
	 * 
	 * @return {@link true} if and only if the value of the class label is
	 *         {@link ClassLabel#ANOMALY}
	 */
	public boolean isAnomaly() {
		return label.equals(ClassLabel.ANOMALY);
	}

	/**
	 * Returns the length (number of pairs) of the {@link TimedWord}.
	 * 
	 * @return The length of the {@link TimedWord}
	 */
	public int getLength() {
		return symbols.size();
	}
}
