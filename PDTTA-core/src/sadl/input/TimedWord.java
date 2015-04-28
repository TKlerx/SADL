package sadl.input;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import sadl.constants.ClassLabel;

/**
 * Class that encapsulates a timed sequence
 * 
 * @author Fabian Witter
 * @author Timo Klerx
 *
 */
public class TimedWord {

	private final TIntList timeValues;
	private final List<String> symbols;
	private ClassLabel label;

	TimedWord() {

		timeValues = new TIntArrayList();
		symbols = new ArrayList<>();
		label = ClassLabel.NORMAL;
	}

	TimedWord(ClassLabel l) {

		timeValues = new TIntArrayList();
		symbols = new ArrayList<>();
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
	 * Returns the symbol {@link String} at the given index of the {@link TimedWord}.
	 * 
	 * @param i
	 *            The index to get the symbol for
	 * @return The symbol at the given index or {@code null} if the index does not exist
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
	 * @return The time delay at the given index or {@code -1} if the index does not exist
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
	 * States whether the {@link TimedWord} is an anomaly according to the {@link ClassLabel}.
	 * 
	 * @return {@code true} if and only if the value of the class label is {@link ClassLabel#ANOMALY}
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

	@Override
	public String toString() {
		return toString(true);
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedWord} in standard format
	 * 
	 * @param withClassLabel
	 *            If {@code true} the {@link ClassLabel} will be appended at the end of the resulting {@code String}
	 * @return The {@link String} representation of this {@link TimedWord}
	 */
	public String toString(boolean withClassLabel) {
		final StringBuilder bw = new StringBuilder();
		bw.append('(');
		for (int j = 0; j < this.getLength(); j++) {
			bw.append(this.getSymbol(j));
			bw.append(',');
			bw.append(Integer.toString(this.getTimeValue(j)));
			bw.append(')');
			if (j < (this.getLength() - 1)) {
				bw.append(' ');
				bw.append('(');
			} else if (withClassLabel) {
				bw.append(':');
				bw.append(Integer.toString(this.getLabel().getClassLabel()));
			}
		}
		return bw.toString();
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedWord} in alternative format
	 * 
	 * @param withClassLabel
	 *            If {@code true} the {@link ClassLabel} will be appended at the end of the resulting {@code String}
	 * @return The {@link String} representation of this {@link TimedWord} in alternative format
	 */
	public String toStringAlt(boolean withClassLabel) {
		final StringBuilder bw = new StringBuilder();
		bw.append(Integer.toString(this.getLength()));
		bw.append(' ');
		for (int j = 0; j < this.getLength(); j++) {
			bw.append(this.getSymbol(j));
			bw.append(' ');
			bw.append(Integer.toString(this.getTimeValue(j)));
			if (j < (this.getLength() - 1)) {
				bw.append(' ');
				bw.append(' ');
			} else if (withClassLabel) {
				bw.append(':');
				bw.append(Integer.toString(this.getLabel().getClassLabel()));
			}
		}
		return bw.toString();
	}
}
