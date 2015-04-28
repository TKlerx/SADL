package de.upb.fwitter.input;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.upb.timok.constants.ClassLabel;


/**
 * Class for reading a set of timed sequences from a file or writing them to a
 * file.
 * 
 * @author Fabian Witter
 *
 */
public class TimedInput {

	private TObjectIntMap<String> alphabet;
	private List<String> alphabetRev;
	private List<TimedWord> words;

	/**
	 * Parses timed sequences from a file. Each line contains exactly one of
	 * those sequences that have the following format:
	 * 
	 * <pre>
	 * {@code (s_1,t_1) (s_2,t_2) ... (s_n,t_n) : label},
	 * </pre>
	 * 
	 * where {@code s_i} is an (event) symbol of type {@link String} and
	 * {@code t_i} is the corresponding time delay of type {@link int}. The
	 * label at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link File} that contains timed sequences in the
	 *            appropriate format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 */
	public static TimedInput parse(File in) {
		return new TimedInput(in, 0, "^\\(", "\\)$", "\\) \\(", ",", " : ");
	}

	/**
	 * Parses timed sequences from a file that has the following alternative
	 * format:
	 * 
	 * <pre>
	 * {@code x y}
	 * {@code n s_1 t_1  s_2 t_2 ... s_n t_n : label}
	 * :
	 * :
	 * </pre>
	 * 
	 * where @{@code x} is the number of sequences contained in that file and @
	 * {@code y} the number of different symbols contained. Each of the
	 * following lines contains exactly one sequence, where {@code s_i} is an
	 * (event) symbol of type {@link String} and {@code t_i} is the
	 * corresponding time delay of type {@link int}. The {@code n} at the
	 * beginning of the line is the number of symbols in that sequence. Note
	 * that between {@code t_i} and {@code s_(i+1)} is a double space.The label
	 * at the end is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link File} that contains timed sequences in the
	 *            appropriate alternative format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 */
	public static TimedInput parseAlt(File in) {
		return new TimedInput(in, 1, "^\\d+ ", "$", "  ", " ", " : ");
	}

	/**
	 * Parses timed sequences from a file in a custom format:
	 * 
	 * @param in
	 *            A {@link File} that contains timed sequences in the
	 *            appropriate alternative format
	 * @param lineOffset
	 *            The number of lines that will be skipped at the beginning of
	 *            the file because they contain a header with meta data
	 * @param seqPrefix
	 *            A regular expression that matches the prefix of each sequence;
	 *            after removing the prefix the line must begin with the first
	 *            symbol {@link String}
	 * @param seqPostfix
	 *            A regular expression that matches the postfix of each sequence
	 *            (until the regular expression in {@link classSep} appears);
	 *            after removing the postfix the line must end with the last
	 *            time delay.
	 * @param pairSep
	 *            A regular expression that matches the separator between two
	 *            value pairs in a sequence; must not be a substring of
	 *            {@link valueSep}
	 * @param valueSep
	 *            A regular expression that matches the separator between two
	 *            values of a pair in a sequence
	 * @param classSep
	 *            A regular expression that matches the separator between the
	 *            sequence and the optional class label of a sequence; must not
	 *            be a substring of {@link pairSep}
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 */
	public static TimedInput parseCustom(File in, int lineOffset,
			String seqPrefix, String seqPostfix, String pairSep,
			String valueSep, String classSep) {

		String pre = !seqPrefix.startsWith("^") ? "^" + seqPrefix : seqPrefix;
		String post = !seqPostfix.endsWith("$") ? seqPostfix + "$" : seqPostfix;

		return new TimedInput(in, lineOffset, pre, post, pairSep, valueSep,
				classSep);
	}

	private TimedInput(File input, int lineOffset, String seqPrefix,
			String seqPostfix, String pairSep, String valueSep, String classSep) {

		try {
			BufferedReader br = new BufferedReader(new FileReader(input));
			loadData(br, lineOffset, seqPrefix, seqPostfix, pairSep, valueSep,
					classSep);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadData(BufferedReader in, int lineOffset, String seqPrefix,
			String seqPostfix, String pairSep, String valueSep, String classSep)
			throws IOException {

		words = new ArrayList<TimedWord>();
		alphabet = new TObjectIntHashMap<String>();
		alphabetRev = new ArrayList<String>();

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
		while ((line = in.readLine()) != null) {
			word = new TimedWord();

			// Split and parse class label (if it exists)
			splitWord = line.split(classSep, 2);
			line = splitWord[0];
			if (splitWord.length == 2) {
				word.setLabel(ClassLabel.valueOf(splitWord[1]));
			}

			// Remove sequence prefix
			line = line.replaceAll(seqPrefix, "");

			// Remove sequence postfix
			line = line.replaceAll(seqPostfix, "");

			// Parse sequence
			splitWord = line.split(pairSep);
			for (int i = 0; i < splitWord.length; i++) {
				splitPair = splitWord[i].split(valueSep, 2);
				if (splitPair.length < 2) {
					throw new IllegalArgumentException("Pair \"" + splitWord[i]
							+ "\" is in the wrong format. Separator \""
							+ valueSep + "\" not found!");
				}
				symbol = splitPair[0];
				if (symbol.matches("\\W")) {
					// Only characters, digits and underscores are allowed for
					// event names ([a-zA-Z_0-9])
					throw new IllegalArgumentException("Event name \"" + symbol
							+ "\" contains forbidden characters. "
							+ "Only [a-zA-Z_0-9] are allowed.");
				}
				timeDelay = Integer.parseInt(splitPair[1]);
				if (!alphabet.containsKey(symbol)) {
					alphabet.put(symbol, alphabet.size());
					alphabetRev.add(symbol);
				}
				// Use String in alphabet to avoid redundant event name
				// instances in input
				word.appendPair(alphabetRev.get(alphabet.get(symbol)),
						timeDelay);
			}
			words.add(word);
		}
	}

	/**
	 * States whether the {@link TimedInput} contains any timed sequences or
	 * not.
	 * 
	 * @return {@link true} if and only if the {@link TimedInput} contains at
	 *         least one timed sequence
	 */
	public boolean isEmpty() {
		return words.isEmpty();
	}

	/**
	 * Removes all {@link TimedWord}s from the {@link TimedInput} to reduce
	 * memory consumption.
	 */
	public void clearWords() {
		words.clear();
	}

	/**
	 * Returns the {@link TimedWord} at the given index.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The {@link TimedWord} at the given index or {@link null} if the
	 *         index does not exist
	 */
	public TimedWord getWord(int i) {

		if (i >= 0 && i < words.size()) {
			return words.get(i);
		}
		return null;
	}

	/**
	 * Returns the number of timed sequences contained in the {@link TimedInput}
	 * .
	 * 
	 * @return The number of timed sequences
	 */
	public int getNumWords() {
		return words.size();
	}

	/**
	 * Returns the number of distinct symbol {@link String}s contained in the
	 * {@link TimedInput}.
	 * 
	 * @return The number of distinct symbols
	 */
	public int getAlphSize() {
		return alphabet.size();
	}

	/**
	 * Returns the symbol {@link String} with the given index from the
	 * {@link TimedInput}.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The symbol with the given index or {@link null} if the index does
	 *         not exist
	 */
	public String getSymbol(int i) {

		if (i >= 0 && i < alphabetRev.size()) {
			return alphabetRev.get(i);
		}
		return null;
	}

	/**
	 * Returns the index of a given symbol {@link String} in the
	 * {@link TimedInput}.
	 * 
	 * @param s
	 *            The symbol to get the index for
	 * @return The index for the given symbol or {@code -1} if the symbol is not
	 *         contained in the {@link TimedInput}
	 */
	public int getAlphIndex(String s) {

		if (alphabet.containsKey(s)) {
			return alphabet.get(s);
		}
		return -1;
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedInput} in
	 * standard format with class labels.
	 * 
	 * @return The {@link String} representation of the {@link TimedInput}
	 * @see TimedInput#toString(boolean)
	 * @see TimedInput#parse(File)
	 */
	@Override
	public String toString() {
		return toString(true);
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedInput} in
	 * standard format as it is required for parsing with
	 * {@link TimedInput#parse(File)}.
	 * 
	 * @param withClassLabel
	 *            If {@link true} the {@link ClassLabel} will be appended at the
	 *            end of each timed sequence
	 * @return The {@link String} representation of the {@link TimedInput}
	 * @see TimedInput#parse(File)
	 */
	public String toString(boolean withClassLabel) {

		StringBuilder builder = new StringBuilder();
		TimedWord word;
		for (int i = 0; i < words.size(); i++) {
			word = words.get(i);
			builder.append("(");
			for (int j = 0; j < word.getLength(); j++) {
				builder.append(word.getSymbol(j) + ",");
				builder.append(word.getTimeValue(j) + ")");
				if (j < (word.getLength() - 1)) {
					builder.append(" (");
				} else if (withClassLabel) {
					builder.append(" : " + word.getLabel());
				}
			}
			if (i < (words.size() - 1)) {
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * Returns the {@link String} representation of the {@link TimedInput} in
	 * alternative format as it is required for parsing with
	 * {@link TimedInput#parseAlt(File)}.
	 * 
	 * @param withClassLabel
	 *            If {@link true} the {@link ClassLabel} will be appended at the
	 *            end of each timed sequence
	 * @return The {@link String} representation of the {@link TimedInput}
	 * @see TimedInput#parseAlt(File)
	 */
	public String toStringAlt(boolean withClassLabel) {

		StringBuilder builder = new StringBuilder();
		builder.append(words.size() + " " + alphabet.size() + "\n");
		TimedWord word;
		for (int i = 0; i < words.size(); i++) {
			word = words.get(i);
			builder.append(word.getLength() + " ");
			for (int j = 0; j < word.getLength(); j++) {
				builder.append(word.getSymbol(j) + " ");
				builder.append(word.getTimeValue(j));
				if (j < (word.getLength() - 1)) {
					builder.append("  ");
				} else if (withClassLabel) {
					builder.append(" : " + word.getLabel());
				}
			}
			if (i < (words.size() - 1)) {
				builder.append("\n");
			}
		}
		return builder.toString();
	}

}
