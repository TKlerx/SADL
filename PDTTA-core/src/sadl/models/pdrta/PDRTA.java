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

import gnu.trove.list.TDoubleList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;

import com.google.common.collect.TreeMultimap;

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

	private Map<PDRTAState, PDRTAState> copyMap;
	private Map<PDRTAState, PDRTAState> invCopyMap;

	private Map<Integer, PDRTAState> states;
	private TObjectIntMap<PDRTAState> invStates;
	private PDRTAState root;
	private final PDRTAInput input;

	public static PDRTA parse(File file) throws IOException {

		final TreeMultimap<Integer, String> trans = TreeMultimap.create();
		final TreeMultimap<Integer, String> stats = TreeMultimap.create();
		final List<String> inputData = new ArrayList<>();

		final BufferedReader br = new BufferedReader(new FileReader(file));
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
		br.close();

		final PDRTAInput in = PDRTAInput.parse(inputData);
		return new PDRTA(trans, stats, in);
	}

	public PDRTA(PDRTA a) {

		input = a.input;
		states = new TreeMap<>();
		invStates = new TObjectIntHashMap<>(a.getNumStates());
		copyMap = new HashMap<>();
		invCopyMap = new HashMap<>();
		for (final PDRTAState org : a.states.values()) {
			final PDRTAState copy = new PDRTAState(org, this);
			copyMap.put(org, copy);
			invCopyMap.put(copy, org);
		}
		for (final PDRTAState org : a.states.values()) {
			for (int i = 0; i < input.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = org.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final Interval cIn = copyMap.get(org).getInterval(i, eIn.getKey());
					assert (cIn.getBegin() == eIn.getValue().getBegin());
					assert (cIn.getEnd() == eIn.getValue().getEnd());
					assert (cIn.getTails().size() == eIn.getValue().getTails().size());
					if (eIn.getValue().getTarget() != null) {
						assert (cIn.getTarget().equals(eIn.getValue().getTarget()));
						cIn.setTarget(copyMap.get(eIn.getValue().getTarget()));
						assert (!cIn.getTarget().equals(eIn.getValue().getTarget()));
					} else {
						assert (cIn.getTarget() == null);
					}
				}
			}
		}

		root = copyMap.get(a.root);

		assert (states.size() == a.states.size());

	}

	public double[] testSeqHisto(TimedTail tail) {

		final List<Double> vals = new ArrayList<>();
		TimedTail t = tail;
		PDRTAState s = root;
		while (t != null) {
			if (t.getSymbolAlphIndex() < 0) {
				return new double[] { -1.0 };
			}
			if (t.getHistBarIndex() < 0) {
				return new double[] { -2.0 };
			}
			vals.add(s.getStat().getHistProb(t));
			final Interval in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			s = in.getTarget();
			t = t.getNextTail();
			if (s == null) {
				if (in.getBegin() == input.getMinTimeDelay() && in.getEnd() == input.getMaxTimeDelay()) {
					return new double[] { -3.0 };
				} else {
					return new double[] { -4.0 };
				}
			}
		}
		vals.add(s.getStat().getTailEndProb());

		final double[] res = new double[vals.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = vals.get(i);
		}
		return res;
	}

	public double[] testSeqTrans(TimedTail tail) {

		final List<Double> vals = new ArrayList<>();
		TimedTail t = tail;
		PDRTAState s = root;
		while (t != null) {
			if (t.getSymbolAlphIndex() < 0) {
				return new double[] { -1.0 };
			}
			if (t.getHistBarIndex() < 0) {
				return new double[] { -2.0 };
			}
			final Interval in = s.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay());
			assert (in != null);
			vals.add(s.getStat().getTransProb(in));
			s = in.getTarget();
			t = t.getNextTail();
			if (s == null) {
				if (in.getBegin() == input.getMinTimeDelay() && in.getEnd() == input.getMaxTimeDelay()) {
					return new double[] { -3.0 };
				} else {
					return new double[] { -4.0 };
				}
			}
		}
		vals.add(s.getStat().getTailEndProb());

		final double[] res = new double[vals.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = vals.get(i);
		}
		return res;
	}

	public PDRTAState getCorrespondingCopy(PDRTAState org) {

		if (copyMap == null) {
			throw new IllegalStateException("This automaton is not a copy!");
		} else {
			return copyMap.get(org);
		}
	}

	public Collection<PDRTAState> getStates() {
		return states.values();
	}

	public boolean isConsistent() {

		for (int i = 0; i < input.getAlphSize(); i++) {
			final Set<Entry<Integer, Interval>> ins = root.getIntervals(i).entrySet();
			for (final Entry<Integer, Interval> eIn : ins) {
				final Set<Entry<Integer, TimedTail>> tails = eIn.getValue().getTails().entries();
				for (final Entry<Integer, TimedTail> eTail : tails) {
					TimedTail t = eTail.getValue();
					PDRTAState target = root;
					while (t != null) {
						assert (target.getInterval(t.getSymbolAlphIndex(), t.getTimeDelay()).getTails().containsValue(t));
						target = target.getTarget(t);
						if (target == null) {
							// TODO Throw exception for more debug information
							return false;
						}
						t = t.getNextTail();
					}
				}
			}
		}
		return true;
	}

	public int getNumStates() {
		return states.size();
	}

	public int getSize() {

		int result = 0;
		for (final PDRTAState s : states.values()) {
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
		return states.containsKey(s);
	}

	public int getIndex(PDRTAState s) {

		if (invStates.containsKey(s)) {
			return invStates.get(s);
		}
		return -1;
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
		toString(sb, 0.0, false);
		return sb.toString();
	}

	public void toString(Appendable ap, double minP, boolean withInput) {

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
					final double p = s.getStat().getTransProb(in);
					final PDRTAState t = in.getTarget();
					if (t != null && p >= minP) {
						if (!found.contains(t)) {
							q.add(t);
							found.add(t);
						}
						// Write transition
						sb.append(getIndex(s));
						sb.append(" -> ");
						sb.append(getIndex(t));
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
			for (final Entry<Integer, PDRTAState> eS : states.entrySet()) {
				final PDRTAState s = eS.getValue();
				if (found.contains(s)) {
					ap.append(Integer.toString(getIndex(s)));
					ap.append(" [ xlabel = \"");
					ap.append(Double.toString(s.getStat().getTailEndProb()));
					ap.append("\"");
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

	void addState(PDRTAState s, int idx) {

		assert (!states.containsValue(s));
		while (states.containsKey(idx)) {
			idx++;
		}
		final PDRTAState x1 = states.put(idx, s);
		final int x2 = invStates.put(s, idx);
		assert (x1 == null);
		assert (x2 == invStates.getNoEntryValue());
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
		init();
	}

	private void init() {

		copyMap = null;
		invCopyMap = null;
		states = new TreeMap<>();
		invStates = new TObjectIntHashMap<>();
		root = new PDRTAState(this);
		createTAPTA();
	}

	private PDRTA(TreeMultimap<Integer, String> trans, TreeMultimap<Integer, String> stats, PDRTAInput inp) {

		copyMap = null;
		invCopyMap = null;
		input = inp;
		states = new TreeMap<>();

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
			assert (in != null);
			assert (in.getTarget() == null);
			assert (s.getStat().getTransProb(in) == 0.0);
			assert (in.contains(begin));
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
			s.getStat().addInterval(in, prob);
		}
		root = states.get(0);
	}

	private void createTAPTA() {

		for (int i = 0; i < input.size(); i++) {
			root.addTail(input.getTailChain(i));
		}
		createSubTAPTA(root);
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

	public void removeSubAPTA(PDRTAState s) {

		removeState(s);

		for (int i = 0; i < input.getAlphSize(); i++) {
			final Set<Entry<Integer, Interval>> ins = s.getIntervals(i).entrySet();
			for (final Entry<Integer, Interval> eIn : ins) {
				if (eIn.getValue().getTarget() != null) {
					removeSubAPTA(eIn.getValue().getTarget());
				}
			}
		}
	}

	public static int getMinData() {
		return minData;
	}

	public void removeState(PDRTAState s) {

		if (invStates.containsKey(s)) {
			final int idx = invStates.remove(s);
			PDRTAState s2 = states.remove(idx);
			assert (s.equals(s2));
			if (copyMap != null) {
				s2 = invCopyMap.remove(s);
				s2 = copyMap.remove(s2);
				assert (s.equals(s2));
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
		for (final Entry<Integer, PDRTAState> eS : states.entrySet()) {
			if (found.contains(eS.getValue())) {
				ap.append("// ");
				ap.append(eS.getKey().toString());
				ap.append(" SYM ");
				ap.append(eS.getValue().getStat().getSymbolProbsString());
				ap.append("\n");
				ap.append("// ");
				ap.append(eS.getKey().toString());
				ap.append(" TIME ");
				ap.append(eS.getValue().getStat().getTimeProbsString());
				ap.append("\n");
			}
		}
	}

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord seq) {
		// TODO Auto-generated method stub
		return null;
	}

}
