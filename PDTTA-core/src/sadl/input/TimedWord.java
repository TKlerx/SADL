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
package sadl.input;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.ClassLabel;

/**
 * Class that encapsulates a timed sequence
 * 
 * @author Fabian Witter
 * @author Timo Klerx
 *
 */
public class TimedWord implements Serializable{
	private static final long serialVersionUID = 111992823193054086L;

	@SuppressWarnings("unused")
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

	public TimedWord(List<String> symbols, TIntList timeValues, ClassLabel label) {
		super();
		this.timeValues = timeValues;
		this.symbols = symbols;
		this.label = label;
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
		return symbols.get(i);
	}


	/**
	 * Returns the time delay at the given index of the {@link TimedWord}.
	 * 
	 * @param i
	 *            The index to get the time delay for
	 * @return The time delay at the given index or {@code -1} if the index does not exist
	 */
	public int getTimeValue(int i) {
		return timeValues.get(i);
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
		for (int j = 0; j < this.length(); j++) {
			bw.append('(');
			bw.append(this.getSymbol(j));
			bw.append(',');
			bw.append(Integer.toString(this.getTimeValue(j)));
			bw.append(')');
			if (j < (this.length() - 1)) {
				bw.append(' ');
			}
		}
		if (withClassLabel) {
			bw.append(':');
			// logger.info("classlabel={}", this.getLabel());
			bw.append(Integer.toString(this.getLabel().getClassLabel()));
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
			if (j < (this.length() - 1)) {
				bw.append(' ');
				bw.append(' ');
			} else if (withClassLabel) {
				bw.append(':');
				bw.append(Integer.toString(this.getLabel().getClassLabel()));
			}
		}
		return bw.toString();
	}



	public TIntList getTimeValues() {
		return timeValues;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((symbols == null) ? 0 : symbols.hashCode());
		result = prime * result + ((timeValues == null) ? 0 : timeValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TimedWord other = (TimedWord) obj;
		if (label != other.label) {
			return false;
		}
		if (symbols == null) {
			if (other.symbols != null) {
				return false;
			}
		} else if (!symbols.equals(other.symbols)) {
			return false;
		}
		if (timeValues == null) {
			if (other.timeValues != null) {
				return false;
			}
		} else if (!timeValues.equals(other.timeValues)) {
			return false;
		}
		return true;
	}

	/**
	 * Generates a subword of this word
	 * 
	 * @param newLength the length of the new subword @return the subword of the specified length
	 */
	public TimedWord getSubWord(int newLength){
		final List<String> newSymbols = new ArrayList<>();
		final TIntList newTimeValues = new TIntArrayList();
		for(int i = 0;i<newLength;i++){
			newSymbols.add(getSymbol(i));
			newTimeValues.add(getTimeValue(i));
		}
		return new TimedWord(newSymbols, newTimeValues, getLabel());
	}

}
