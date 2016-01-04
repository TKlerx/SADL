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
package sadl.models.pdrta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.input.TimedInput;
import sadl.input.TimedWord;

/**
 * 
 * @author Fabian Witter
 *
 */
public class PDRTAInput implements Serializable {

	private static final long serialVersionUID = 20150504L;

	private int maxTimeDelay;
	private int minTimeDelay;

	private int[] histoBorders;
	private int[] histoSizes;

	private List<TimedTail> tails;

	private final TimedInput inp;

	public PDRTAInput(TimedInput inp, String histBins, boolean expand) {

		this.inp = inp;
		final TIntList timePoints = loadTimeDelays(expand);
		setHistBorders(timePoints, histBins);
		init();
	}

	private PDRTAInput(TimedInput inp, int[] histBorders, int minDelay, int maxDelay) {

		this.inp = inp;
		histoBorders = histBorders;
		maxTimeDelay = maxDelay;
		minTimeDelay = minDelay;
		init();
	}

	private void init() {

		checkBorders();
		calcHistSizes();
		tails = new ArrayList<>(inp.size());
		for (int i = 0; i < inp.size(); i++) {
			tails.add(createTailChain(i, inp.get(i)));
		}
		// TODO Decide what to do about TimedWords in memory
		// inp.clearWords();
	}

	private TIntList loadTimeDelays(boolean expand) {

		final TIntList timePoints = new TIntArrayList();
		maxTimeDelay = Integer.MIN_VALUE;
		minTimeDelay = Integer.MAX_VALUE;

		int timeDelay;
		for (final TimedWord w : inp) {
			for (int i = 0; i < w.length(); i++) {
				timeDelay = w.getTimeValue(i);
				if (timeDelay > maxTimeDelay) {
					maxTimeDelay = timeDelay;
				}
				if (timeDelay < minTimeDelay) {
					minTimeDelay = timeDelay;
				}
				timePoints.add(timeDelay);
			}
		}

		if (expand) {
			final int val = (int) Math.rint((maxTimeDelay - minTimeDelay) * 0.05);
			minTimeDelay -= val;
			maxTimeDelay += val;
			if (minTimeDelay < 0) {
				minTimeDelay = 0;
			}
		}

		return timePoints;
	}

	private void setHistBorders(TIntList timePoints, String histBins) {

		boolean err = false;
		if (histBins == null) {
			err = true;
		} else {
			final String[] b = histBins.split("-", -1);
			if (b.length == 1) {
				try {
					final int numHistoBins = Integer.parseInt(b[0]);
					calcHistBorders(timePoints, numHistoBins);
				} catch (final NumberFormatException e) {
					err = true;
				}
			} else {
				if (!(b[0].equals("") || !b[b.length - 1].equals(""))) {
					err = true;
				} else {
					histoBorders = new int[b.length - 2];
					for (int i = 1; i < (b.length - 1); i++) {
						try {
							histoBorders[i - 1] = Integer.parseInt(b[i]);
						} catch (final NumberFormatException e) {
							err = true;
						}
					}
				}
			}
		}
		if (err) {
			throw new IllegalArgumentException(
					"Wrong parameter: HISTOGRAM_BINS must be " + "a sequence of borders -b1-b2-...-bn- or the number of bins to use");
		}
	}

	private void calcHistBorders(TIntList timePoints, int numHistoBins) {

		timePoints.sort();
		histoBorders = new int[numHistoBins - 1];
		for (int i = 1; i < numHistoBins; i++) {
			final double idx = ((double) i / (double) numHistoBins) * (timePoints.size() - 1);
			final double idxFloor = Math.floor(idx);
			if (Precision.equals(idx, idxFloor)) {
				histoBorders[i - 1] = timePoints.get((int) idx);
			} else {
				final double vFloor = timePoints.get((int) idxFloor);
				final double vCeil = timePoints.get((int) (idxFloor + 1.0));
				final double val = (vFloor * (1.0 - (idx - idxFloor))) + (vCeil * (idx - idxFloor));
				histoBorders[i - 1] = (int) Math.rint(val);
			}
		}
	}

	/**
	 * States whether the {@link PDRTAInput} contains any timed sequences or not.
	 * 
	 * @return {@code true} if and only if the {@link TimedInput} contains at least one timed sequence
	 */
	boolean isEmpty() {
		return tails.isEmpty();
	}

	/**
	 * Removes all {@link TimedTail}s from the {@link PDRTAInput} to reduce memory consumption.
	 */
	public void clear() {
		tails.clear();
	}

	/**
	 * Returns the {@link TimedTail} chain at the given index.
	 * 
	 * @param i
	 *            The index to get the {@link TimedTail} chain for
	 * @return The {@link TimedTail} chain at the given index or {@code null} if the index does not exist
	 */
	TimedTail getTailChain(int i) {

		if (i >= 0 && i < tails.size()) {
			return tails.get(i);
		}
		return null;
	}

	/**
	 * Returns the number of timed sequences contained in the {@link PDRTAInput} .
	 * 
	 * @return The number of timed sequences
	 */
	public int size() {
		return tails.size();
	}

	/**
	 * Returns the number of distinct symbol {@link String}s contained in the {@link PDRTAInput}.
	 * 
	 * @return The number of distinct symbols
	 */
	public int getAlphSize() {
		return inp.getAlphSize();
	}

	/**
	 * Returns the symbol {@link String} with the given index from the {@link PDRTAInput}.
	 * 
	 * @param i
	 *            The index to get the symbol {@link String} for
	 * @return The symbol with the given index or {@code null} if the index does not exist
	 */
	public String getSymbol(int i) {
		return inp.getSymbol(i);
	}

	/**
	 * Returns the index of a given symbol {@link String} in the {@link PDRTAInput}.
	 * 
	 * @param s
	 *            The symbol to get the index for
	 * @return The index for the given symbol or {@code -1} if the symbol is not contained in the {@link PDRTAInput}
	 */
	public int getAlphIndex(String s) {
		return inp.getAlphIndex(s);
	}

	private void calcHistSizes() {

		histoSizes = new int[histoBorders.length + 1];
		if (histoBorders.length == 0) {
			histoSizes[0] = (maxTimeDelay - minTimeDelay) + 1;
		} else {
			histoSizes[0] = (histoBorders[0] - minTimeDelay) + 1;
			for (int i = 0; i < histoBorders.length - 1; i++) {
				histoSizes[i + 1] = histoBorders[i + 1] - histoBorders[i];
			}
			histoSizes[histoSizes.length - 1] = maxTimeDelay - histoBorders[histoBorders.length - 1];
		}
	}

	private void checkBorders() {

		Arrays.sort(histoBorders);
		int delCount = 0;
		int curBorder = minTimeDelay - 1;

		for (int i = 0; i < histoBorders.length; i++) {
			if (curBorder >= histoBorders[i]) {
				histoBorders[i] = Integer.MIN_VALUE;
				delCount++;
			} else {
				curBorder = histoBorders[i];
			}
		}
		for (int i = histoBorders.length - 1; i >= 0; i--) {
			if (histoBorders[i] != Integer.MIN_VALUE) {
				if (histoBorders[i] >= maxTimeDelay) {
					histoBorders[i] = Integer.MIN_VALUE;
					delCount++;
				} else {
					break;
				}
			}
		}

		if (delCount > 0) {
			if (delCount < histoBorders.length) {
				int idx = 0;
				final int[] tmp = new int[histoBorders.length - delCount];
				for (int i = 0; i < histoBorders.length; i++) {
					if (histoBorders[i] != Integer.MIN_VALUE) {
						tmp[idx] = histoBorders[i];
						idx++;
					}
				}
				histoBorders = tmp;
			} else {
				histoBorders = new int[0];
			}
		}
	}

	public int getMaxTimeDelay() {
		return maxTimeDelay;
	}

	public int getMinTimeDelay() {
		return minTimeDelay;
	}

	public int getNumHistogramBars() {
		return histoSizes.length;
	}

	protected int[] getHistSizes() {
		return histoSizes;
	}

	protected int[] getHistBorders() {
		return histoBorders;
	}

	private TimedTail createTailChain(int idxWord, TimedWord word) {

		// TODO Try to reuse this param check. Error when testing with trained tester, input not empty
		// if (inp.isEmpty() || inp.get(idxWord).equals(word)) {

		String s = "startTail";
		int sIdx = Integer.MIN_VALUE;
		int tDel = sIdx;
		int tDelIdx = sIdx;
		final TimedTail startTail = new TimedTail(s, sIdx, tDel, tDelIdx, idxWord, Integer.MIN_VALUE, null);

		TimedTail prevTail = startTail;
		for (int i = 0; i < word.length(); i++) {
			s = word.getSymbol(i);
			sIdx = getAlphIndex(s);
			tDel = word.getTimeValue(i);
			tDelIdx = getHistBarIdx(tDel);
			prevTail = new TimedTail(s, sIdx, tDel, tDelIdx, idxWord, i, prevTail);
		}
		return startTail;
		// } else {
		// throw new IllegalArgumentException("The given TimedWord must be part of the TimedInput");
		// }
	}

	private int getHistBarIdx(int time) {

		if (time < minTimeDelay || time > maxTimeDelay) {
			return -1;
		} else {
			for (int i = 0; i < histoBorders.length; i++) {
				if (time <= histoBorders[i]) {
					return i;
				}
			}
			return histoBorders.length;
		}
	}

	TimedTail toTestTailChain(TimedWord word) {
		return createTailChain(-1, word);
	}

	static PDRTAInput parse(List<String> data) {

		if (data.size() != 4) {
			throw new IllegalArgumentException("Content is not correct!");
		}

		int min, max;
		final String[] alph;
		int[] borders;

		if (data.get(0).startsWith("minTimeDelay=")) {
			min = Integer.parseInt(data.get(0).substring(13));
		} else {
			throw new IllegalArgumentException("Content is not correct!");
		}

		if (data.get(1).startsWith("maxTimeDelay=")) {
			max = Integer.parseInt(data.get(1).substring(13));
		} else {
			throw new IllegalArgumentException("Content is not correct!");
		}

		if (data.get(2).startsWith("alphabet={")) {
			final String reduced = data.get(2).substring(10, data.get(2).length() - 1);
			if (!reduced.matches("^(\\w+,)*\\w+$")) {
				throw new IllegalArgumentException("Content is not correct!");
			}
			alph = reduced.split(",");
		} else {
			throw new IllegalArgumentException("Content is not correct!");
		}

		if (data.get(3).startsWith("histoborders={")) {
			final String[] bo = data.get(3).substring(14, data.get(3).length() - 1).split(",");
			if (bo.length == 1 && bo[0].equals("")) {
				borders = new int[0];
			} else {
				borders = new int[bo.length];
				for (int i = 0; i < bo.length; i++) {
					try {
						borders[i] = Integer.parseInt(bo[i]);
					} catch (final NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			throw new IllegalArgumentException("Content is not correct!");
		}

		final TimedInput inp = new TimedInput(alph);
		return new PDRTAInput(inp, borders, min, max);
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(histoBorders);
		result = prime * result + Arrays.hashCode(histoSizes);
		result = prime * result + ((inp == null) ? 0 : inp.hashCode());
		result = prime * result + maxTimeDelay;
		result = prime * result + minTimeDelay;
		result = prime * result + ((tails == null) ? 0 : tails.hashCode());
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
		final PDRTAInput other = (PDRTAInput) obj;
		if (!Arrays.equals(histoBorders, other.histoBorders)) {
			return false;
		}
		if (!Arrays.equals(histoSizes, other.histoSizes)) {
			return false;
		}
		if (inp == null) {
			if (other.inp != null) {
				return false;
			}
		} else if (!inp.equals(other.inp)) {
			return false;
		}
		if (maxTimeDelay != other.maxTimeDelay) {
			return false;
		}
		if (minTimeDelay != other.minTimeDelay) {
			return false;
		}
		if (tails == null) {
			if (other.tails != null) {
				return false;
			}
		} else if (!tails.equals(other.tails)) {
			return false;
		}
		return true;
	}

}
