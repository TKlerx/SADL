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
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

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
					final int idx = Integer.parseInt(start);
					stats.put(idx, new String(line));
				} else if (line.matches("^\\d.+")) {
					final String start = line.split(" ", 2)[0];
					final int idx = Integer.parseInt(start);
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
				final Set<Entry<Integer, Interval>> ins = org.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final Interval cIn = getState(org.getIndex()).getInterval(i, eIn.getKey());
					assert(cIn.getBegin() == eIn.getValue().getBegin());
					assert(cIn.getEnd() == eIn.getValue().getEnd());
					assert(cIn.getTails().size() == eIn.getValue().getTails().size());
					if (eIn.getValue().getTarget() != null) {
						assert(cIn.getTarget() == eIn.getValue().getTarget());
						cIn.setTarget(getState(eIn.getValue().getTarget().getIndex()));
						assert(cIn.getTarget() != eIn.getValue().getTarget());
					} else {
						assert(cIn.getTarget() == null);
					}
				}
			}
		}

		root = getState(a.root.getIndex());

		assert(states.size() == a.states.size());

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
			final Interval in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			s = in.getTarget();
			t = t.getNextTail();
			if (s == null) {
				if (in.getBegin() == input.getMinTimeDelay() && in.getEnd() == input.getMaxTimeDelay()) {
					// return Pair.create(new TDoubleArrayList(0), new TDoubleArrayList(new double[] { -2.0 }));
					return Pair.create(new TDoubleArrayList(new double[] { 0.0 }), new TDoubleArrayList(0));
				} else {
					// return Pair.create(new TDoubleArrayList(0), new TDoubleArrayList(new double[] { -3.0 }));
					return Pair.create(new TDoubleArrayList(new double[] { 0.0 }), new TDoubleArrayList(0));
				}
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
			final Interval in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			assert(in != null);
			transP.add(s.getStat().getTransProb(t.getSymbolAlphIndex(), in));
			s = in.getTarget();
			t = t.getNextTail();
			if (s == null) {
				if (in.getBegin() == input.getMinTimeDelay() && in.getEnd() == input.getMaxTimeDelay()) {
					// return new TDoubleArrayList(new double[] { -3.0 });
					return new TDoubleArrayList(new double[] { 0.0 });
				} else {
					// return new TDoubleArrayList(new double[] { -4.0 });
					return new TDoubleArrayList(new double[] { 0.0 });
				}
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
		for (int i = 0; i < input.getAlphSize(); i++) {
			final Set<Entry<Integer, Interval>> ins = root.getIntervals(i).entrySet();
			for (final Entry<Integer, Interval> eIn : ins) {
				final Set<Entry<Integer, TimedTail>> tails = eIn.getValue().getTails().entries();
				for (final Entry<Integer, TimedTail> eTail : tails) {
					TimedTail t = eTail.getValue();
					PDRTAState source = root, target;
					while (t != null) {
						assert(source.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay()).getTails().containsValue(t));
						target = source.getTarget(t);
						if (target == null) {
							throw new IllegalStateException("The tail (" + input.getSymbol(t.getSymbolAlphIndex()) + "," + t.getTimeDelay()
							+ ") has no transition from state ((" + source.getIndex() + "))!");
						}
						source = target;
						t = t.getNextTail();
					}
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
			PDRTAState s2 = states.get(s.getIndex());
			if (s != s2) {
				throw new IllegalStateException("State (" + s.getIndex() + ") is not in map!");
			}
			counter++;
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					s2 = eIn.getValue().getTarget();
					if (s2 != null && !seen.contains(s2)) {
						seen.add(s2);
						q.add(s2);
					}
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
			for (int i = 0; i < input.getAlphSize(); ++i) {
				final Set<Entry<Integer, Interval>> es = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> e : es) {
					final Interval in = e.getValue();
					if (in.getTarget() != null) {
						result++;
					}
				}
			}
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
		toDOTLang(sb);
		return sb.toString();
	}

	public void toDOTLang(Appendable ap) {
		toDOTLang(ap, 0.0, false);
	}

	public void toDOTLang(Appendable ap, double minP, boolean withInput) {
		toDOTLang(ap, minP, withInput, null);
	}

	public void toDOTLang(Appendable ap, double minP, boolean withInput, StateColoring sc) {

		// Write transitions with high probability
		final StringBuilder sb = new StringBuilder();
		final Queue<PDRTAState> q = new ArrayDeque<>();
		final Set<PDRTAState> found = new HashSet<>();
		q.add(root);
		found.add(root);
		while (!q.isEmpty()) {
			final PDRTAState s = q.remove();
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
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

		try {
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
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	int addState(PDRTAState s, int idx) {

		while (states.containsKey(idx)) {
			idx++;
		}
		final PDRTAState x1 = states.put(idx, s);
		assert(x1 == null);
		return idx;
	}

	protected boolean hasInput() {
		return !input.isEmpty();
	}

	protected int getMaxTimeDelay() {
		return input.getMaxTimeDelay();
	}

	protected int getMinTimeDelay() {
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

		for (final Integer idx : trans.keySet()) {
			final StateStatistic s = StateStatistic.reconstructStat(input.getAlphSize(), getHistSizes(), stats.get(idx));
			states.put(idx, new PDRTAState(this, idx, s));
		}
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
				final StateStatistic st = StateStatistic.reconstructStat(getAlphSize(), getHistSizes(), stats.get(target));
				t = new PDRTAState(this, target, st);
				states.put(target, t);
			} else {
				t = states.get(target);
			}
			Interval in = s.getInterval(input.getAlphIndex(sym), end);
			assert(in != null);
			assert(in.getTarget() == null);
			assert(s.getStat().getTransProb(input.getAlphIndex(sym), in) == 0.0);
			assert(in.contains(begin));
			Interval newIn;
			if (end < in.getEnd()) {
				newIn = in.split(end);
				s.getIntervals(input.getAlphIndex(sym)).put(newIn.getEnd(), newIn);
				in = newIn;
			}
			if (begin > in.getBegin()) {
				newIn = in.split(begin - 1);
				s.getIntervals(input.getAlphIndex(sym)).put(newIn.getEnd(), newIn);
			}
			in.setTarget(t);
			s.getStat().addInterval(input.getAlphIndex(sym), in, prob);
		}
		root = states.get(0);
	}

	private void createTAPTA() {

		for (int i = 0; i < input.size(); i++) {
			root.addTail(input.getTailChain(i));
		}
		createSubTAPTA(root);
	}

	public void createSubTAPTA_rec_new(PDRTAState s) {

		for (int i = 0; i < input.getAlphSize(); i++) {
			assert(s.getIntervals(i).size() == 1);
			final Interval interval = s.getIntervals(i).lastEntry().getValue();
			assert(interval.getTarget() == null);
			final Set<Entry<Integer, TimedTail>> tails = interval.getTails().entries();
			if (!tails.isEmpty() && interval.getTarget() == null) {
				final PDRTAState target = new PDRTAState(this);
				interval.setTarget(target);
				for (final Entry<Integer, TimedTail> e : tails) {
					target.addTail(e.getValue());
				}
				createSubTAPTA(target);
			}
		}
	}

	public void createSubTAPTA(PDRTAState s) {

		for (int i = 0; i < input.getAlphSize(); i++) {
			assert (s.getIntervals(i).size() == 1);
			final Interval interval = s.getIntervals(i).lastEntry().getValue();
			final Set<Entry<Integer, TimedTail>> tails = interval.getTails().entries();
			if (!tails.isEmpty() && interval.getTarget() == null) {
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
					in = ts.getInterval(tail.getSymbolAlphIndex(), tail.getTimeDelay());
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

		for (int i = 0; i < input.getAlphSize(); i++) {
			final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
			for (final Entry<Integer, Interval> eIn : ins) {
				if (eIn.getValue().getTarget() != null) {
					removeSubAPTA(eIn.getValue().getTarget(), sc);
				}
			}
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
			System.err.println("states are not equal.");
			System.out.println("This.states.size=" + states.size());
			System.out.println("other.states.size=" + other.states.size());
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
		int result = 0;
		final Queue<PDRTAState> q = new ArrayDeque<>();
		final Set<PDRTAState> found = new HashSet<>();
		q.add(root);
		found.add(root);
		while (!q.isEmpty()) {
			final PDRTAState s = q.remove();
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final Interval in = eIn.getValue();
					final PDRTAState t = in.getTarget();
					if (t != null) {
						result++;
					}
				}
			}
		}
		return result;
	}

}
