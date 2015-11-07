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

package sadl.input;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sadl.constants.ClassLabel;

/**
 * Class for reading a set of timed sequences from a file or writing them to a file.
 * 
 * @author Fabian Witter
 * @author Timo Klerx
 *
 */
public class TimedInput implements Iterable<TimedWord>, Serializable {
	// TODO we also need (untimed)Input for PDFAs etc
	private static final long serialVersionUID = -2175576201528760567L;

	private static Logger logger = LoggerFactory.getLogger(TimedInput.class);

	private final TObjectIntMap<String> alphabet = new TObjectIntHashMap<>();
	private final List<String> alphabetRev = new ArrayList<>();
	private List<TimedWord> words = new ArrayList<>();

	private static final String[] parseSymbols = new String[] { "^\\(", "\\)$", "\\)\\s+\\(", "\\s*,\\s*", "\\s*:\\s*" };
	private static final String[] parseSymbolsAlt = new String[] { "^\\d+ ", "$", "\\s{2}", "\\s", "\\s*:\\s*" };
	private static final int parseStart = 0;
	private static final int parseStartAlt = 1;

	public TimedInput(List<TimedWord> words) {
		this.words.addAll(words);
		for (final TimedWord w : words) {
			if (w.getClass() == TimedWord.class) {
				for (final String s : w.symbols) {
					if (!alphabet.containsKey(s)) {
						alphabet.put(s, alphabet.size());
						alphabetRev.add(s);
					}
				}
			}
		}
	}

	// TODO maybe add parsing for anomaly type?!
	/**
	 * Parses timed sequences from a file. Each line contains exactly one of those sequences that have the following format:
	 * 
	 * <pre>
	 * {@code (s_1,t_1) (s_2,t_2) ... (s_n,t_n) : label}
	 * </pre>
	 * 
	 * where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type {@code int}. The label at the end
	 * is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link Path} that contains timed sequences in the appropriate format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parse(Path in) throws IOException {
		return parseCustom(in, parseStart, parseSymbols[0], parseSymbols[1], parseSymbols[2], parseSymbols[3], parseSymbols[4]);
	}

	/**
	 * Parses timed sequences from a file. Each line contains exactly one of those sequences that have the following format:
	 * 
	 * <pre>
	 * {@code (s_1,t_1) (s_2,t_2) ... (s_n,t_n) : label}
	 * </pre>
	 * 
	 * where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type {@code int}. The label at the end
	 * is of type {@link ClassLabel} and is optional.
	 * 
	 * @param br
	 *            A {@link InputStream} that contains timed sequences in the appropriate format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parse(Reader br) throws IOException {
		return parseCustom(br, parseStart, parseSymbols[0], parseSymbols[1], parseSymbols[2], parseSymbols[3], parseSymbols[4]);
	}

	/**
	 * Parses timed sequences from a file that has the following alternative format:
	 * 
	 * <pre>
	 * {@code x y}
	 * {@code n s_1 t_1  s_2 t_2 ... s_n t_n : label}
	 * :
	 * :
	 * </pre>
	 * 
	 * where @{@code x} is the number of sequences contained in that file and @ {@code y} the number of different symbols contained. Each of the following lines
	 * contains exactly one sequence, where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type
	 * {@link Integer}. The {@code n} at the beginning of the line is the number of symbols in that sequence. Note that between {@code t_i} and {@code s_(i+1)}
	 * is a double space.The label at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link Path} that contains timed sequences in the appropriate alternative format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseAlt(Path in) throws IOException {
		return parseAlt(in, parseStartAlt);
	}

	/**
	 * Parses timed sequences from a file that has the following alternative format:
	 * 
	 * <pre>
	 * {@code x y}
	 * {@code n s_1 t_1  s_2 t_2 ... s_n t_n : label}
	 * :
	 * :
	 * </pre>
	 * 
	 * where @{@code x} is the number of sequences contained in that file and @ {@code y} the number of different symbols contained. Each of the following lines
	 * contains exactly one sequence, where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type
	 * {@link Integer}. The {@code n} at the beginning of the line is the number of symbols in that sequence. Note that between {@code t_i} and {@code s_(i+1)}
	 * is a double space.The label at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param br
	 *            A {@link BufferedReader} that contains timed sequences in the appropriate alternative format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseAlt(Reader br) throws IOException {
		return parseAlt(br, parseStartAlt);
	}

	/**
	 * Parses timed sequences from a file that has the following alternative format:
	 * 
	 * <pre>
	 * {@code x y}
	 * {@code n s_1 t_1  s_2 t_2 ... s_n t_n : label}
	 * :
	 * :
	 * </pre>
	 * 
	 * where @{@code x} is the number of sequences contained in that file and @ {@code y} the number of different symbols contained. Each of the following lines
	 * contains exactly one sequence, where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type
	 * {@link Integer}. The {@code n} at the beginning of the line is the number of symbols in that sequence. Note that between {@code t_i} and {@code s_(i+1)}
	 * is a double space.The label at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link Path} that contains timed sequences in the appropriate alternative format
	 * @param lineOffset
	 *            The number of lines that will be skipped at the beginning of the file because they contain a header with meta data
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseAlt(Path in, int lineOffset) throws IOException {
		return parseCustom(in, lineOffset, parseSymbolsAlt[0], parseSymbolsAlt[1], parseSymbolsAlt[2], parseSymbolsAlt[3], parseSymbolsAlt[4]);
	}

	/**
	 * Parses timed sequences from a file that has the following alternative format:
	 * 
	 * <pre>
	 * {@code x y}
	 * {@code n s_1 t_1  s_2 t_2 ... s_n t_n : label}
	 * :
	 * :
	 * </pre>
	 * 
	 * where @{@code x} is the number of sequences contained in that file and @ {@code y} the number of different symbols contained. Each of the following lines
	 * contains exactly one sequence, where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type
	 * {@link Integer}. The {@code n} at the beginning of the line is the number of symbols in that sequence. Note that between {@code t_i} and {@code s_(i+1)}
	 * is a double space.The label at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param br
	 *            A {@link BufferedReader} that contains timed sequences in the appropriate alternative format
	 * @param lineOffset
	 *            The number of lines that will be skipped at the beginning of the file because they contain a header with meta data
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseAlt(Reader br, int lineOffset) throws IOException {
		return parseCustom(br, lineOffset, parseSymbolsAlt[0], parseSymbolsAlt[1], parseSymbolsAlt[2], parseSymbolsAlt[3], parseSymbolsAlt[4]);
	}

	/**
	 * Parses timed sequences from a file in a custom format:
	 * 
	 * @param in
	 *            A {@link Path} that contains timed sequences in the appropriate alternative format
	 * @param lineOffset
	 *            The number of lines that will be skipped at the beginning of the file because they contain a header with meta data
	 * @param seqPrefix
	 *            A regular expression that matches the prefix of each sequence; after removing the prefix the line must begin with the first symbol
	 *            {@link String}
	 * @param seqPostfix
	 *            A regular expression that matches the postfix of each sequence (until the regular expression in {@code classSep} appears); after removing the
	 *            postfix the line must end with the last time delay.
	 * @param pairSep
	 *            A regular expression that matches the separator between two value pairs in a sequence; must not be a substring of {@code valueSep}
	 * @param valueSep
	 *            A regular expression that matches the separator between two values of a pair in a sequence
	 * @param classSep
	 *            A regular expression that matches the separator between the sequence and the optional class label of a sequence; must not be a substring of
	 *            {@code pairSep}
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseCustom(Path in, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep)
			throws IOException {
		if (Files.notExists(in)) {
			logger.warn("File {} was not found.", in);
			throw new FileNotFoundException("input file on path " + in.toAbsolutePath() + " was not found");
		}
		try (BufferedReader br = Files.newBufferedReader(in)) {
			return parseCustom(br, lineOffset, seqPrefix, seqPostfix, pairSep, valueSep, classSep);
		}
	}

	/**
	 * Parses timed sequences from a file in a custom format:
	 * 
	 * @param br
	 *            A {@link BufferedReader} that contains timed sequences in the appropriate alternative format
	 * @param lineOffset
	 *            The number of lines that will be skipped at the beginning of the file because they contain a header with meta data
	 * @param seqPrefix
	 *            A regular expression that matches the prefix of each sequence; after removing the prefix the line must begin with the first symbol
	 *            {@link String}
	 * @param seqPostfix
	 *            A regular expression that matches the postfix of each sequence (until the regular expression in {@code classSep} appears); after removing the
	 *            postfix the line must end with the last time delay.
	 * @param pairSep
	 *            A regular expression that matches the separator between two value pairs in a sequence; must not be a substring of {@code valueSep}
	 * @param valueSep
	 *            A regular expression that matches the separator between two values of a pair in a sequence
	 * @param classSep
	 *            A regular expression that matches the separator between the sequence and the optional class label of a sequence; must not be a substring of
	 *            {@code pairSep}
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 * @throws IOException
	 */
	public static TimedInput parseCustom(Reader br, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep,
			String classSep) throws IOException {

		final String pre = !seqPrefix.startsWith("^") ? "^" + seqPrefix : seqPrefix;
		final String post = !seqPostfix.endsWith("$") ? seqPostfix + "$" : seqPostfix;

		return new TimedInput(br, lineOffset, pre, post, pairSep, valueSep, classSep);
	}

	private TimedInput(Reader br, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep)
			throws IOException {
		loadData(br, lineOffset, seqPrefix, seqPostfix, pairSep, valueSep, classSep);
	}

	private void loadData(Reader br, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep)
			throws IOException {


		try (BufferedReader in = new BufferedReader(br)) {

			// Skip offset lines at the beginning of the file
			int counter = 0;
			while (counter < lineOffset) {
				in.readLine();
				counter++;
			}

			TimedWord word;
			String symbol;
			int timeDelay;
			String line;
			String[] splitWord;
			String[] splitPair;
			int lineCount = 0;
			while ((line = in.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				word = new TimedWord();

				// Split and parse class label (if it exists)
				splitWord = line.split(classSep, 2);
				line = splitWord[0];
				ClassLabel label;
				if (splitWord.length == 2) {
					switch (splitWord[1]) {
					case "0":
						label = ClassLabel.NORMAL;
						break;
					case "1":
						label = ClassLabel.ANOMALY;
						break;
					default:
						label = ClassLabel.NORMAL;
						break;
					}
					word.setLabel(label);
				}

				// Remove sequence prefix
				line = line.replaceAll(seqPrefix, "");

				// Remove sequence postfix
				line = line.replaceAll(seqPostfix, "");
				if (!line.isEmpty()) {
					// Parse sequence
					splitWord = line.split(pairSep);
					for (int i = 0; i < splitWord.length; i++) {
						splitPair = splitWord[i].split(valueSep, 2);
						if (splitPair.length < 2) {
							final String errorMessage = "Pair \"" + splitWord[i] + "\" in line " + lineCount + " is in the wrong format. Separator \"" + valueSep
									+ "\" not found!";
							final IllegalArgumentException e = new IllegalArgumentException(errorMessage);
							logger.error(errorMessage, e);
							throw e;
						}
						symbol = splitPair[0];
						if (symbol.matches("\\W")) {
							// Only characters, digits and underscores are allowed for
							// event names ([a-zA-Z_0-9])
							final String errorMessage = "Event name \"" + symbol + "\" in line " + lineCount + " contains forbidden characters. "
									+ "Only [a-zA-Z_0-9] are allowed.";
							final IllegalArgumentException e = new IllegalArgumentException(errorMessage);
							logger.error(errorMessage, e);
							throw e;
						}
						timeDelay = Integer.parseInt(splitPair[1].trim());
						if (!alphabet.containsKey(symbol)) {
							alphabet.put(symbol, alphabet.size());
							alphabetRev.add(symbol);
						}
						// Use String in alphabet to avoid redundant event name
						// instances in input
						word.appendPair(alphabetRev.get(alphabet.get(symbol)), timeDelay);
					}
				}
				words.add(word);
				lineCount++;
			}
			br.close();
		}
	}

	/**
	 * States whether the {@link TimedInput} contains any timed sequences or not.
	 * 
	 * @return {@code true} if and only if the {@link TimedInput} contains at least one timed sequence
	 */
	public boolean isEmpty() {
		return words.isEmpty();
	}

	private boolean cleared = false;
	/**
	 * Removes all {@link TimedWord}s from the {@link TimedInput} to reduce memory consumption.
	 */
	public void clearWords() {
		words.clear();
		cleared = true;
	}

	/**
	 * Returns the {@link TimedWord} at the given index.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The {@link TimedWord} at the given index or {@code null} if the index does not exist
	 */
	public TimedWord getWord(int i) {
		return get(i);
	}

	/**
	 * Returns the {@link TimedWord} at the given index.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The {@link TimedWord} at the given index or {@code null} if the index does not exist
	 */
	public TimedWord get(int i) {
		checkCleared();
		return words.get(i);
	}

	/**
	 * Returns the number of timed sequences contained in the {@link TimedInput} .
	 * 
	 * @return The number of timed sequences
	 */
	public int size() {
		checkCleared();
		return words.size();
	}

	/**
	 * Returns the number of distinct symbol {@link String}s contained in the {@link TimedInput}.
	 * 
	 * @return The number of distinct symbols
	 */
	public int getAlphSize() {
		return alphabet.size();
	}

	/**
	 * Returns the symbol {@link String} with the given index from the {@link TimedInput}.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The symbol with the given index or {@code null} if the index does not exist
	 */
	public String getSymbol(int i) {
		return alphabetRev.get(i);
	}

	/**
	 * Returns the index of a given symbol {@link String} in the {@link TimedInput}.
	 * 
	 * @param s
	 *            The symbol to get the index for
	 * @return The index for the given symbol or {@code -1} if the symbol is not contained in the {@link TimedInput}
	 */
	public int getAlphIndex(String s) {
		if (alphabet.containsKey(s)) {
			return alphabet.get(s);
		}
		return -1;
	}

	public String[] getSymbols() {
		final String[] result = alphabet.keys(new String[0]);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedInput} in standard format with class labels.
	 * 
	 * @return The {@link String} representation of the {@link TimedInput}
	 * @see TimedWord#toString(boolean)
	 * @see TimedInput#parse(Path)
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		try {
			toFile(builder, true);
			return builder.toString();
		} catch (final IOException e) {
			logger.error("Unexpected exception", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes the {@link String} representation of the {@link TimedInput} in standard format as it is required for parsing with {@link TimedInput#parse(Path)}.
	 * 
	 * @param bw
	 *            The writer where to write the {@link String} representation
	 * @param withClassLabel
	 *            If {@code true} the {@link ClassLabel} will be appended at the end of each timed sequence
	 * @see TimedWord#toString(boolean)
	 * @see TimedInput#parse(Path)
	 */
	public void toFile(Appendable bw, boolean withClassLabel) throws IOException {
		toFile(bw, word -> word.toString(withClassLabel));
	}

	private void toFile(Appendable a, Function<TimedWord, String> f) throws IOException {
		checkCleared();
		for (int i = 0; i < words.size(); i++) {
			a.append(f.apply(words.get(i)));
			if (i < (words.size() - 1)) {
				a.append('\n');
			}
		}
	}

	/**
	 * Writes the {@link String} representation of the {@link TimedInput} in alternative format as it is required for parsing with
	 * {@link TimedInput#parseAlt(Path)}.
	 * 
	 * @param bw
	 *            The writer where to write the {@link String} representation
	 * @param withClassLabel
	 *            If {@code true} the {@link ClassLabel} will be appended at the end of each timed sequence
	 * @see TimedWord#toString(boolean)
	 * @see TimedInput#parseAlt(Path)
	 */
	public void toFileAlt(Appendable bw, boolean withClassLabel) throws IOException {
		bw.append(Integer.toString(words.size()));
		bw.append(' ');
		bw.append(Integer.toString(alphabet.size()));
		bw.append('\n');
		toFile(bw, word -> word.toStringAlt(withClassLabel));
	}

	@Override
	public Iterator<TimedWord> iterator() {
		checkCleared();
		return new Iterator<TimedWord>() {
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < size();
			}

			@Override
			public TimedWord next() {
				i++;
				return getWord(i - 1);
			}
		};
	}

	protected void checkCleared() {
		if (cleared) {
			throw new IllegalStateException("Input was cleared before");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alphabet == null) ? 0 : alphabet.hashCode());
		result = prime * result + ((alphabetRev == null) ? 0 : alphabetRev.hashCode());
		result = prime * result + ((words == null) ? 0 : words.hashCode());
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
		final TimedInput other = (TimedInput) obj;
		if (alphabet == null) {
			if (other.alphabet != null) {
				return false;
			}
		} else if (!alphabet.equals(other.alphabet)) {
			return false;
		}
		if (alphabetRev == null) {
			if (other.alphabetRev != null) {
				return false;
			}
		} else if (!alphabetRev.equals(other.alphabetRev)) {
			return false;
		}
		if (words == null) {
			if (other.words != null) {
				return false;
			}
		} else if (!words.equals(other.words)) {
			return false;
		}
		return true;
	}

	public void decreaseSamples(double d) {
		words = words.subList(0, (int) (words.size() * d));
	}

}
