package sadl.input;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;

/**
 * Class that encapsulates a timed sequence
 * 
 * @author Fabian Witter
 * @author Timo Klerx
 *
 */
public class TimedWord {
	private static Logger logger = LoggerFactory.getLogger(TimedWord.class);

	protected TIntList timeValues;
	protected List<String> symbols;
	private ClassLabel label;

	TimedWord() {
		timeValues = new TIntArrayList();
		symbols = new ArrayList<>();
		label = ClassLabel.NORMAL;
	}

	TimedWord(ClassLabel l) {
		this();
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
	// XXX why not throw an arrayOutOfBoundException instead of returning null?
	public String getSymbol(int i) {
		if (i < symbols.size() && i >= 0) {
			return symbols.get(i);
		}
		return null;
	}

	public int getIntSymbol(int i) {
		logger.warn("Calling getIntSymbol on TimedWord. This is slow! Think of converting to TimedIntWord instead.");
		return Integer.parseInt(getSymbol(i));
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
	@Deprecated
	public int getLength() {
		return symbols.size();
	}

	/**
	 * Returns the length (number of pairs) of the {@link TimedWord}.
	 * 
	 * @return The length of the {@link TimedWord}
	 */
	public int length() {
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
		bw.append(Integer.toString(this.length()));
		bw.append(' ');
		for (int j = 0; j < this.length(); j++) {
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

	public String getSymbolString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length(); i++) {
			sb.append(getSymbol(i));
			if (i != length() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public TIntList getTimeValues() {
		return timeValues;
	}

	public TIntList getIntSymbols() {
		logger.warn("Transforming String to int symbols. This is slow! Think of transforming to TimedIntWords");
		return transformToIntList();
	}

	TIntList transformToIntList() {
		return new TIntArrayList(symbols.stream().mapToInt(s -> Integer.parseInt(s)).toArray());
	}

	public TimedWord toIntWord() {
		return new TimedIntWord(this);
	}

	public String toTrebaString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length(); i++) {
			sb.append(getSymbol(i));
			sb.append(' ');
			sb.append(getTimeValue(i));
			if (i != length() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

}
