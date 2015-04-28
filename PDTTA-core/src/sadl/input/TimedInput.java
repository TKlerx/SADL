package sadl.input;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;

/**
 * Class for reading a set of timed sequences from a file or writing them to a file.
 * 
 * @author Fabian Witter
 * @author Timo Klerx
 *
 */
public class TimedInput {
	private static Logger logger = LoggerFactory.getLogger(TimedInput.class);

	private TObjectIntMap<String> alphabet;
	private List<String> alphabetRev;
	private List<TimedWord> words;

	/**
	 * Parses timed sequences from a file. Each line contains exactly one of those sequences that have the following format:
	 * 
	 * <pre>
	 * {@code (s_1,t_1) (s_2,t_2) ... (s_n,t_n) : label},
	 * </pre>
	 * 
	 * where {@code s_i} is an (event) symbol of type {@link String} and {@code t_i} is the corresponding time delay of type {@code int}. The label at the end
	 * is of type {@link ClassLabel} and is optional.
	 * 
	 * @param in
	 *            A {@link Path} that contains timed sequences in the appropriate format
	 * @return A {@link TimedInput} that represents the timed sequences parsed
	 */
	public static TimedInput parse(Path in) {
		return new TimedInput(in, 0, "^\\(", "\\)$", "\\)\\s+\\(", "\\s*,\\s*", "\\s*:\\s*");
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
	 */
	public static TimedInput parseAlt(Path in) {
		return new TimedInput(in, 1, "^\\d+ ", "$", "\\s{2}", "\\s", "\\s*:\\s*");
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
	 */
	public static TimedInput parseCustom(Path in, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep) {

		final String pre = !seqPrefix.startsWith("^") ? "^" + seqPrefix : seqPrefix;
		final String post = !seqPostfix.endsWith("$") ? seqPostfix + "$" : seqPostfix;

		return new TimedInput(in, lineOffset, pre, post, pairSep, valueSep, classSep);
	}

	private TimedInput(Path input, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep) {

		try (BufferedReader br = Files.newBufferedReader(input)) {
			loadData(br, lineOffset, seqPrefix, seqPostfix, pairSep, valueSep, classSep);
		} catch (final IOException e) {
			logger.error("Unexpected exception occured.");
		}
	}

	private void loadData(BufferedReader in, int lineOffset, String seqPrefix, String seqPostfix, String pairSep, String valueSep, String classSep)
			throws IOException {

		words = new ArrayList<>();
		alphabet = new TObjectIntHashMap<>();
		alphabetRev = new ArrayList<>();

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

			// Parse sequence
			splitWord = line.split(pairSep);
			for (int i = 0; i < splitWord.length; i++) {
				splitPair = splitWord[i].split(valueSep, 2);
				if (splitPair.length < 2) {
					throw new IllegalArgumentException("Pair \"" + splitWord[i] + "\" is in the wrong format. Separator \"" + valueSep + "\" not found!");
				}
				symbol = splitPair[0];
				if (symbol.matches("\\W")) {
					// Only characters, digits and underscores are allowed for
					// event names ([a-zA-Z_0-9])
					throw new IllegalArgumentException("Event name \"" + symbol + "\" contains forbidden characters. " + "Only [a-zA-Z_0-9] are allowed.");
				}
				timeDelay = Integer.parseInt(splitPair[1]);
				if (!alphabet.containsKey(symbol)) {
					alphabet.put(symbol, alphabet.size());
					alphabetRev.add(symbol);
				}
				// Use String in alphabet to avoid redundant event name
				// instances in input
				word.appendPair(alphabetRev.get(alphabet.get(symbol)), timeDelay);
			}
			words.add(word);
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

	/**
	 * Removes all {@link TimedWord}s from the {@link TimedInput} to reduce memory consumption.
	 */
	public void clearWords() {
		words.clear();
	}

	/**
	 * Returns the {@link TimedWord} at the given index.
	 * 
	 * @param i
	 *            The index to get the {@link TimedWord} for
	 * @return The {@link TimedWord} at the given index or {@code null} if the index does not exist
	 */
	public TimedWord getWord(int i) {

		if (i >= 0 && i < words.size()) {
			return words.get(i);
		}
		return null;
	}

	/**
	 * Returns the number of timed sequences contained in the {@link TimedInput} .
	 * 
	 * @return The number of timed sequences
	 */
	public int getNumWords() {
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

		if (i >= 0 && i < alphabetRev.size()) {
			return alphabetRev.get(i);
		}
		return null;
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
		for (int i = 0; i < words.size(); i++) {
			builder.append(words.get(i).toString());
			if (i < (words.size() - 1)) {
				builder.append('\n');
			}
		}
		return builder.toString();
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
	public void toFile(BufferedWriter bw, boolean withClassLabel) throws IOException {
		for (int i = 0; i < words.size(); i++) {
			bw.append(words.get(i).toString(withClassLabel));
			if (i < (words.size() - 1)) {
				bw.append('\n');
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
	public void toFileAlt(BufferedWriter bw, boolean withClassLabel) throws IOException {

		bw.append(Integer.toString(words.size()));
		bw.append(' ');
		bw.append(Integer.toString(alphabet.size()));
		bw.append('\n');
		for (int i = 0; i < words.size(); i++) {
			bw.append(words.get(i).toStringAlt(withClassLabel));
			if (i < (words.size() - 1)) {
				bw.append('\n');
			}
		}
	}

}
