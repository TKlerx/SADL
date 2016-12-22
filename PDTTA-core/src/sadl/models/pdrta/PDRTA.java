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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.TreeMultimap;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.modellearner.rtiplus.StateColoring;

/**
 * This class represents a Probabilistic Deterministic Real Time Automaton (PDRTA). It provides methods for training (Split and Merge) and for anomaly
 * detection.
 * 
 * @author Fabian Witter
 * 
 */
public class PDRTA implements AutomatonModel, Serializable {

	private static final long serialVersionUID = 20150504L;

	private static final int minData = 10;

	private final TIntObjectMap<PDRTAState> states;
	private final PDRTAState root;
	private final PDRTAInput input;

	public static PDRTA parse(File file) throws IOException {

		final TreeMultimap<Integer, String> trans = TreeMultimap.create();
		final TreeMultimap<Integer, String> stats = TreeMultimap.create();
		final List<String> inputData = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = br.readLine()) != null) {
				if (line.matches("^// \\D.+")) {
					inputData.add(line.substring(3));
				} else if (line.matches("^// \\d.+")) {
					line = line.substring(3);
					final String start = line.split(" ", 2)[0];
					final Integer idx = Integer.valueOf(start);
					stats.put(idx, new String(line));
				} else if (line.matches("^\\d.+")) {
					final String start = line.split(" ", 2)[0];
					final Integer idx = Integer.valueOf(start);
					if (line.matches("^\\d+ \\[.+")) {
						stats.put(idx, new String(line));
					} else {
						trans.put(idx, new String(line));
					}
				}
			}
		}

		final PDRTAInput in = PDRTAInput.parse(inputData);
		return new PDRTA(trans, stats, in);
	}

	public PDRTAState getState(int index) {
		return states.get(index);
	}

	public PDRTA(PDRTA a) {

		input = a.input;
		states = new TIntObjectHashMap<>();
		for (final PDRTAState org : a.states.valueCollection()) {
			new PDRTAState(org, this);
		}
		for (final PDRTAState org : a.states.valueCollection()) {
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Optional<Set<Entry<Integer, Interval>>> ins = org.getIntervals(i).map(m -> m.entrySet());
				if (ins.isPresent()) {
					for (final Entry<Integer, Interval> eIn : ins.get()) {
						final Optional<Interval> cIn = getState(org.getIndex()).getInterval(i, eIn.getKey().intValue());
						if (eIn.getValue() != null) {
							assert (cIn.isPresent());
							assert (cIn.get().getBegin() == eIn.getValue().getBegin());
							assert (cIn.get().getEnd() == eIn.getValue().getEnd());
							assert (cIn.get().getTails().size() == eIn.getValue().getTails().size());
							assert (cIn.get().getTarget() == eIn.getValue().getTarget());
							cIn.get().setTarget(getState(eIn.getValue().getTarget().getIndex()));
							assert (cIn.get().getTarget() != eIn.getValue().getTarget());
						} else {
							assert (!cIn.isPresent());
						}
					}
				}
			}
		}

		root = getState(a.root.getIndex());

		assert (states.size() == a.states.size());

	}

	private Pair<TDoubleList, TDoubleList> testSeqHisto(TimedTail tail) {

		final TDoubleList symP = new TDoubleArrayList();
		final TDoubleList timeP = new TDoubleArrayList();
		double[] p;
		TimedTail t = tail.getNextTail();
		PDRTAState s = root;
		while (t != null) {
			if (t.getSymbolAlphIndex() < 0) {
				// return Pair.create(new TDoubleArrayList(new double[] { -1.0 }), new TDoubleArrayList(0));
				return Pair.create(new TDoubleArrayList(new double[] { 0.0 }), new TDoubleArrayList(0));
			}
			if (t.getHistBarIndex() < 0) {
				// return Pair.create(new TDoubleArrayList(new double[] { -2.0 }), new TDoubleArrayList(0));
				return Pair.create(new TDoubleArrayList(new double[] { 0.0 }), new TDoubleArrayList(0));
			}
			p = s.getStat().getHistProb(t);
			symP.add(p[0]);
			timeP.add(p[1]);
			final Optional<Interval> in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			if (in.isPresent()) {
				s = in.get().getTarget();
				t = t.getNextTail();
			} else {
				// return Pair.create(new TDoubleArrayList(0), new TDoubleArrayList(new double[] { -3.0 }));
				return Pair.create(new TDoubleArrayList(new double[] { 0.0 }), new TDoubleArrayList(0));
			}
		}
		symP.add(s.getStat().getTailEndProb());

		return Pair.create(symP, timeP);
	}

	private TDoubleList testSeqTrans(TimedTail tail) {

		final TDoubleList transP = new TDoubleArrayList();
		TimedTail t = tail.getNextTail();
		PDRTAState s = root;
		while (t != null) {
			if (t.getSymbolAlphIndex() < 0) {
				// return new TDoubleArrayList(new double[] { -1.0 });
				return new TDoubleArrayList(new double[] { 0.0 });
			}
			if (t.getHistBarIndex() < 0) {
				// return new TDoubleArrayList(new double[] { -2.0 });
				return new TDoubleArrayList(new double[] { 0.0 });
			}
			final Optional<Interval> in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			transP.add(s.getStat().getTransProb(t.getSymbolAlphIndex(), in));
			if (in.isPresent()) {
				s = in.get().getTarget();
				t = t.getNextTail();
			} else {
				// return new TDoubleArrayList(new double[] { -3.0 });
				return new TDoubleArrayList(new double[] { 0.0 });
			}
		}
		transP.add(s.getStat().getTailEndProb());

		return transP;
	}

	public Collection<PDRTAState> getStates() {
		return states.valueCollection();
	}

	public void checkConsistency() {

		// Checking that a path for each sequence exists
		// Get all tails that are leaving root
		final List<TimedTail> rootTails = root.getIntervals().stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null)
				.flatMap(in -> in.getTails().entries().stream()).map(e -> e.getValue()).collect(Collectors.toList());

		if (rootTails.size() < input.size()) {
			throw new IllegalStateException("Sequences are missing in the root state");
		}

		for (TimedTail t : rootTails) {
			PDRTAState source = root;
			while (t != null) {
				final Optional<Interval> in = source.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
				if (in.isPresent()) {
					final NavigableSet<TimedTail> inTails = in.get().getTails().get(new Integer(t.getTimeDelay()));
					if (inTails == null || !inTails.contains(t)) {
						throw new IllegalStateException("The tail (" + input.getSymbol(t.getSymbolAlphIndex()) + "," + t.getTimeDelay()
						+ ") was not found in transition ((" + source.getIndex() + "))--" + input.getSymbol(t.getSymbolAlphIndex()) + "-["
						+ in.get().getBegin() + "," + in.get().getEnd() + "]-->((" + in.get().getTarget().getIndex() + "))");
					}
					source = in.get().getTarget();
					t = t.getNextTail();
				} else {
					throw new IllegalStateException("The tail (" + input.getSymbol(t.getSymbolAlphIndex()) + "," + t.getTimeDelay()
					+ ") has no transition from state ((" + source.getIndex() + "))!");
				}
			}
		}

		// Checking that number of states in structure is equal to number of states in map
		int counter = 0;
		final Set<PDRTAState> seen = new HashSet<>();
		seen.add(root);
		final Queue<PDRTAState> q = new LinkedList<>();
		q.add(root);
		while (!q.isEmpty()) {
			final PDRTAState s = q.poll();
			final PDRTAState s2 = states.get(s.getIndex());
			if (s != s2) {
				throw new IllegalStateException("State (" + s.getIndex() + ") is not in map!");
			}
			counter++;
			for (final PDRTAState t : s.getTargets()) {
				if (!seen.contains(t)) {
					seen.add(t);
					q.add(t);
				}
			}
		}
		if (counter != states.size()) {
			throw new IllegalStateException("Found " + counter + " sates in structure but " + states.size() + " states are in map!");
		}
	}

	@Override
	public int getStateCount() {
		return states.size();
	}

	public int getSize() {

		int result = 0;
		for (final PDRTAState s : states.valueCollection()) {
			// Count intervals for state
			result += s.getIntervals().stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null).mapToInt(in -> 1).sum();
		}
		return result;
	}

	public PDRTAState getRoot() {
		return root;
	}

	public boolean containsState(PDRTAState s) {

		final PDRTAState s2 = states.get(s.getIndex());
		if (s2 != null && s == s2) {
			return true;
		} else {
			return false;
		}
	}

	public String getSymbol(int idx) {
		return input.getSymbol(idx);
	}

	public int getAlphSize() {
		return input.getAlphSize();
	}

	public String getHistBinsString() {

		final int[] b = input.getHistBorders();
		String s;
		if (b.length == 0) {
			s = "[" + input.getMinTimeDelay() + "," + input.getMaxTimeDelay() + "]";
		} else {
			s = "[" + input.getMinTimeDelay() + "," + b[0] + "] , ";
			for (int i = 0; i < b.length - 1; i++) {
				s += "[" + (b[i] + 1) + "," + b[i + 1] + "] , ";
			}
			s += "[" + (b[b.length - 1] + 1) + "," + input.getMaxTimeDelay() + "]";
		}
		return s;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		try {
			toDOTLang(sb);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public void toDOTLang(Appendable ap) throws IOException {
		toDOTLang(ap, 0.0, false);
	}

	public void toDOTLang(Appendable ap, double minP, boolean withInput) throws IOException {
		toDOTLang(ap, minP, withInput, null);
	}

	public void toDOTLang(Appendable ap, double minP, boolean withInput, StateColoring sc) throws IOException {

		// Write transitions with high probability
		final StringBuilder sb = new StringBuilder();
		final Queue<PDRTAState> q = new ArrayDeque<>();
		final Set<PDRTAState> found = new HashSet<>();
		q.add(root);
		found.add(root);
		while (!q.isEmpty()) {
			final PDRTAState s = q.remove();
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Optional<Set<Entry<Integer, Interval>>> ins = s.getIntervals(i).map(m -> m.entrySet());
				if (ins.isPresent()) {
					for (final Entry<Integer, Interval> eIn : ins.get()) {
						if (eIn.getValue() != null) {
							final Interval in = eIn.getValue();
							final double p = s.getStat().getTransProb(i, in);
							final PDRTAState t = in.getTarget();
							if (t != null && p >= minP) {
								if (!found.contains(t)) {
									q.add(t);
									found.add(t);
								}
								// Write transition
								sb.append(s.getIndex());
								sb.append(" -> ");
								sb.append(t.getIndex());
								sb.append(" [ label = \"");
								sb.append(getSymbol(i));
								sb.append(" [");
								sb.append(in.getBegin());
								sb.append(", ");
								sb.append(in.getEnd());
								sb.append("] p=");
								sb.append(p);
								if (withInput) {
									sb.append(" n=");
									sb.append(in.getTails().size());
								}
								sb.append("\" ];\n");
							}
						}
					}
				}
			}
		}

		writeStatData(ap, found);

		// Write automaton in DOT language
		ap.append("digraph PDRTA {\n");
		ap.append("rankdir=LR;\n");
		ap.append("node[style = filled, fillcolor = white, shape = circle];\n");
		ap.append("\"\"[style = invis, shape = none, margin = 0, width = 0, heigth = 0];\n");
		ap.append("\"\" -> 0;\n");

		// Write states
		for (final PDRTAState s : states.valueCollection()) {
			if (found.contains(s)) {
				ap.append(Integer.toString(s.getIndex()));
				ap.append(" [ xlabel = \"");
				ap.append(Double.toString(s.getStat().getTailEndProb()));
				ap.append("\"");
				if (sc != null) {
					if (sc.isRed(s)) {
						ap.append(", fillcolor = \"#FFA9A9\"");
					} else if (sc.isBlue(s)) {
						ap.append(", fillcolor = \"#A9D1FF\"");
					}
				}
				ap.append(" ];\n");
			}
		}

		// Add transitions
		ap.append(sb.toString());

		ap.append("}");
	}

	int addState(PDRTAState s, int idx) {

		while (states.containsKey(idx)) {
			idx++;
		}
		final PDRTAState x1 = states.put(idx, s);
		assert (x1 == null);
		return idx;
	}

	protected boolean hasInput() {
		return !input.isEmpty();
	}

	public int getMaxTimeDelay() {
		return input.getMaxTimeDelay();
	}

	public int getMinTimeDelay() {
		return input.getMinTimeDelay();
	}

	protected int getNumHistogramBars() {
		return input.getNumHistogramBars();
	}

	protected int[] getHistSizes() {
		return input.getHistSizes();
	}

	protected PDRTAInput getInput() {
		return input;
	}

	public PDRTA(PDRTAInput in) {

		input = in;
		states = new TIntObjectHashMap<>();
		root = new PDRTAState(this);
		createTAPTA();
	}

	private PDRTA(TreeMultimap<Integer, String> trans, TreeMultimap<Integer, String> stats, PDRTAInput inp) {

		input = inp;
		states = new TIntObjectHashMap<>();

		// Create states with statistic
		for (final Integer sourceIdx : trans.keySet()) {
			final StateStatistic s = StateStatistic.reconstructStat(input.getAlphSize(), getHistSizes(), stats.get(sourceIdx));
			new PDRTAState(this, sourceIdx.intValue(), s);
		}
		// Create transitions
		for (final Entry<Integer, String> eT : trans.entries()) {
			// 75 -> 76 [ label = "0 [3, 989] p=1.0" ];
			final String[] split = eT.getValue().split(" ");
			final int source = Integer.parseInt(split[0]);
			final int target = Integer.parseInt(split[2]);
			final String sym = split[6].substring(1);
			final int begin = Integer.parseInt(split[7].substring(1, split[7].length() - 1));
			final int end = Integer.parseInt(split[8].substring(0, split[8].length() - 1));
			double prob;
			if (split[9].endsWith("\"")) {
				prob = Double.parseDouble(split[9].substring(2, split[9].length() - 1));
			} else {
				prob = Double.parseDouble(split[9].substring(2));
			}
			final PDRTAState s = states.get(source);
			PDRTAState t;
			if (!states.containsKey(target)) {
				final StateStatistic st = StateStatistic.reconstructStat(getAlphSize(), getHistSizes(), stats.get(new Integer(target)));
				t = new PDRTAState(this, target, st);
			} else {
				t = states.get(target);
			}
			// Create interval for source state
			Optional<NavigableMap<Integer, Interval>> ins = s.getIntervals(input.getAlphIndex(sym));
			if (!ins.isPresent()) {
				final NavigableMap<Integer, Interval> m = new TreeMap<>();
				m.put(new Integer(input.getMaxTimeDelay()), null);
				ins = Optional.of(m);
				s.getIntervals().set(input.getAlphIndex(sym), ins.get());
			}
			assert (ins.get().ceilingEntry(new Integer(end)).getValue() == null);
			final Interval newIn = new Interval(begin, end);
			ins.get().put(new Integer(end), newIn);
			if (!ins.get().containsKey(new Integer(begin - 1)) && begin > input.getMinTimeDelay()) {
				// Put empty dummy below new interval
				ins.get().put(new Integer(begin - 1), null);
			}
			assert (newIn.getTarget() == null);
			assert (s.getStat().getTransProb(input.getAlphIndex(sym), newIn) == 0.0);
			newIn.setTarget(t);
			s.getStat().addInterval(input.getAlphIndex(sym), newIn, prob);
		}
		root = states.get(0);
	}

	private void createTAPTA() {

		for (int i = 0; i < input.size(); i++) {
			root.addTail(input.getTailChain(i));
		}
		createSubTAPTA(root);
	}

	// TODO Get this working
	public void createSubTAPTA_rec_new(PDRTAState s) {

		// Get all present intervals of s
		final List<Interval> ins = s.getIntervals().stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null)
				.collect(Collectors.toList());

		for (final Interval in : ins) {
			assert (in.getTarget() == null);
			final Set<Entry<Integer, TimedTail>> tails = in.getTails().entries();
			if (!tails.isEmpty()) {
				final PDRTAState target = new PDRTAState(this);
				in.setTarget(target);
				for (final Entry<Integer, TimedTail> e : tails) {
					target.addTail(e.getValue());
				}
				createSubTAPTA_rec_new(target);
			}
		}
	}

	public void createSubTAPTA(PDRTAState s) {

		// Get all present intervals of s
		final List<Interval> ins = s.getIntervals().stream().filter(m -> m != null).flatMap(m -> m.values().stream()).filter(in -> in != null)
				.collect(Collectors.toList());

		for (final Interval interval : ins) {
			final Set<Entry<Integer, TimedTail>> tails = interval.getTails().entries();
			assert (interval.getTarget() == null);
			if (!tails.isEmpty()) {
				interval.setTarget(new PDRTAState(this));
			}
			for (final Entry<Integer, TimedTail> e : tails) {
				TimedTail tail = e.getValue();
				PDRTAState ts = interval.getTarget();
				Interval in = interval;
				while (tail.getNextTail() != null) {
					ts.addTail(tail);
					assert (in.containsTail(tail));
					tail = tail.getNextTail();
					// Interval has to be present, was created within the addTail method
					in = ts.getInterval(tail.getSymbolAlphIndex(), tail.getTimeDelay()).get();
					if (in.getTarget() == null) {
						in.setTarget(new PDRTAState(this));
					}
					ts = in.getTarget();
				}
				ts.addTail(tail);
			}

		}
	}

	public PDRTAState createState() {
		return new PDRTAState(this);
	}

	public void removeSubAPTA(PDRTAState s, StateColoring sc) {

		removeState(s, sc);
		for (final PDRTAState t : s.getTargets()) {
			removeSubAPTA(t, sc);
		}
	}

	public static int getMinData() {
		return minData;
	}

	public void removeState(PDRTAState s, StateColoring sc) {

		if (states.containsKey(s.getIndex()) && states.get(s.getIndex()) == s) {
			states.remove(s.getIndex());
			if (sc.isBlue(s)) {
				sc.remove(s);
			}
		} else {
			throw new IllegalArgumentException("The given state is not part of the PDRTA!");
		}
	}

	private void writeStatData(Appendable ap, Set<PDRTAState> found) throws IOException {

		// Write min/max time delays as comment
		ap.append("// minTimeDelay=");
		ap.append(Integer.toString(input.getMinTimeDelay()));
		ap.append("\n");
		ap.append("// maxTimeDelay=");
		ap.append(Integer.toString(input.getMaxTimeDelay()));
		ap.append("\n");
		// Write alphabet as comment
		ap.append("// alphabet={");
		ap.append(input.getSymbol(0));
		for (int i = 1; i < input.getAlphSize(); i++) {
			ap.append(",");
			ap.append(input.getSymbol(i));
		}
		ap.append("}\n");
		// Write histogram borders as comment
		ap.append("// histoborders={");
		if (input.getHistBorders().length > 0) {
			ap.append(Integer.toString(input.getHistBorders()[0]));
			for (int i = 1; i < input.getNumHistogramBars() - 1; i++) {
				ap.append(",");
				ap.append(Integer.toString(input.getHistBorders()[i]));
			}
		}
		ap.append("}\n");
		// Write symbol and time distributions for each state as comment
		for (final int key : states.keys()) {
			final PDRTAState s = states.get(key);
			if (found.contains(s)) {
				ap.append("// ");
				ap.append(Integer.toString(key));
				ap.append(" SYM ");
				ap.append(s.getStat().getSymbolProbsString());
				ap.append("\n");
				ap.append("// ");
				ap.append(Integer.toString(key));
				ap.append(" TIME ");
				ap.append(s.getStat().getTimeProbsString());
				ap.append("\n");
			}
		}
	}

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord seq) {

		final TimedTail t = input.toTestTailChain(seq);
		return testSeqHisto(t);
	}

	public Pair<TDoubleList, TDoubleList> calculateProbsTrans(TimedWord seq) {

		final TimedTail t = input.toTestTailChain(seq);
		return Pair.create(testSeqTrans(t), new TDoubleArrayList(0));
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + ((states == null) ? 0 : states.hashCode());
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
		final PDRTA other = (PDRTA) obj;
		if (input == null) {
			if (other.input != null) {
				return false;
			}
		} else if (!input.equals(other.input)) {
			return false;
		}
		if (root == null) {
			if (other.root != null) {
				return false;
			}
		} else if (!root.equals(other.root)) {
			return false;
		}
		if (states == null) {
			if (other.states != null) {
				return false;
			}
		} else if (!states.equals(other.states)) {
			return false;
		}
		return true;
	}

	public void cleanUp() {

		input.clear();
		for (final PDRTAState s : states.valueCollection()) {
			s.cleanUp();
		}
	}

	@Override
	public Map<String, Function<TimedWord, Pair<TDoubleList, TDoubleList>>> getAvailableCalcMethods() {

		final Map<String, Function<TimedWord, Pair<TDoubleList, TDoubleList>>> m = AutomatonModel.super.getAvailableCalcMethods();
		m.put("histogramPobs", this::calculateProbabilities);
		m.put("transitionProbs", this::calculateProbsTrans);
		return m;
	}

	@Override
	public int getTransitionCount() {
		return getSize();
	}

}
