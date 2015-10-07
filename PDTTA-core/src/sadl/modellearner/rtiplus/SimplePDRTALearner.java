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

package sadl.modellearner.rtiplus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import sadl.input.TimedInput;
import sadl.interfaces.Model;
import sadl.interfaces.ModelLearner;
import sadl.modellearner.rtiplus.boolop.AndOperator;
import sadl.modellearner.rtiplus.boolop.BooleanOperator;
import sadl.modellearner.rtiplus.boolop.OrOperator;
import sadl.modellearner.rtiplus.tester.FishersMethodTester;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAInput;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.TimedTail;

/**
 * 
 * @author Fabian Witter
 *
 */
public class SimplePDRTALearner implements ModelLearner {

	public enum RunMode {
		SILENT, NORMAL_CONSOLE, NORMAL_FILE, NORMAL_BATCH, DEBUG, DEBUG_STEPS, DEBUG_DEEP
	}

	public enum OperationTesterType {
		LRT, LRT_ADV, NAIVE_LRT, FM, FM_ADV
	}

	public enum DistributionCheckType {
		DISABLED, ALL_BORDER, ALL, MAD_BORDER, MAD, OUTLIER_BORDER, OUTLIER
	}

	// The boolean operators for the pooling strategy used by Verwer's LRT and FM
	// 0: Operator for pooling (thesis: AND, impl: AND, own: AND)
	// 1: Operator for pool discarding (thesis: missing, impl: [LRT: OR, FM: AND], own: AND)
	// 2: Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
	public static BooleanOperator[] bOp;

	// TODO Use JCommander and remove
	private static final String USAGE = "Usage: java -cp jRTI+.jar de.upb.fw.searcher.Searcher"
			+ " [SIGNIFICANCE] [DISTR_CHECK_TYPE] [HISTOGRAM_BINS] [FILE]\n"
			+ "1.  SIGNIFICANCE  is a decision (float) value between 0.0 and 1.0, default is 0.05 (5% significance)\n"
			+ "2.  DISTR_CHECK_TYPE  is -1 for disable dirtibution check, 0 for split every gap," + " 1 for MAD value, 2 for outlier value\n"
			+ "3.  HISTOGRAM_BINS  can be given as (inner) borders -b1-b2-...-bn- or as the number of bins to use\n"
			+ "4.  FILE  is an input file conaining unlabeled timed strings";

	public RunMode runMode = RunMode.SILENT;

	protected long startTime;

	protected final double significance;
	protected final DistributionCheckType distrCheckType;
	protected final String histBinsStr;
	protected final OperationTester tester;

	protected final String directory;

	public SimplePDRTALearner(double sig, String histBins, OperationTesterType testerType, DistributionCheckType distrCheckType, String boolOps, String dir) {

		if (sig < 0.0 || sig > 1.0) {
			throw new IllegalArgumentException("Wrong parameter: SIGNIFICANCE must be a decision (float) value between 0.0 and 1.0");
		}

		parseBoolOps(boolOps);

		this.significance = sig;
		this.distrCheckType = distrCheckType;
		this.histBinsStr = histBins;
		this.directory = dir;

		switch (testerType) {
		case LRT:
			this.tester = new LikelihoodRatioTester(false);
			break;
		case LRT_ADV:
			this.tester = new LikelihoodRatioTester(true);
			break;
		case NAIVE_LRT:
			this.tester = new NaiveLikelihoodRatioTester();
			break;
		case FM:
			this.tester = new FishersMethodTester(false);
			break;
		case FM_ADV:
			this.tester = new FishersMethodTester(true);
			break;
		default:
			this.tester = new LikelihoodRatioTester(false);
			break;
		}
	}

	private void parseBoolOps(String bOps) {

		final char[] c = bOps.toUpperCase().toCharArray();

		if (c.length != 3) {
			throw new IllegalArgumentException("Boolean operators must be a tripple of 'A' or 'O' symbols!");
		}

		bOp = new BooleanOperator[c.length];
		for (int i = 0; i < c.length; i++) {
			switch (c[i]) {
			case 'A':
				bOp[i] = new AndOperator();
				break;
			case 'O':
				bOp[i] = new OrOperator();
				break;
			default:
				bOp[i] = new AndOperator();
				System.err.println("The symbol '" + c[i] + "' is not appropriate for boolean operator. Using default 'A'.");
				break;
			}
		}
	}

	@Override
	public Model train(TimedInput trainingSequences) {

		System.out.println("RTI+: Building automaton from input sequences");

		final boolean expand = distrCheckType.compareTo(DistributionCheckType.ALL) > 0;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histBinsStr, expand);
		final PDRTA a = new PDRTA(in);

		System.out.println("Parameters are: significance=" + significance + " distrCheckType=" + distrCheckType);
		System.out.println("Histogram Bins are: " + a.getHistBinsString());
		if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
			System.out.println("Log Level: " + runMode);
		}

		System.out.println("*** Performing simple RTI+ ***");
		startTime = System.currentTimeMillis();
		final StateColoring sc = new StateColoring(a);
		sc.setRed(a.getRoot());
		tester.setColoring(sc);
		complete(a, sc);
		a.cleanUp();
		persistFinalResult(a);

		System.out.println("Time: " + getDuration(startTime, System.currentTimeMillis()));
		System.out.println("END");

		return a;
	}

	protected Transition getMostVisitedTrans(PDRTA a, StateColoring sc) {

		int maxVisit = 0;
		Transition trans = null;
		for (final PDRTAState r : sc) {
			for (int i = 0; i < a.getAlphSize(); i++) {
				final Set<Entry<Integer, Interval>> ins = r.getIntervals(i).entrySet();
				for (final Entry<Integer, Interval> eIn : ins) {
					final Interval in = eIn.getValue();
					assert (in.getTarget() == null || sc.isBlue(in.getTarget()) || sc.isRed(in.getTarget()));
					if (sc.isBlue(in.getTarget())) {
						if (maxVisit < in.getTails().size()) {
							maxVisit = in.getTails().size();
							trans = new Transition(a, r, i, in, in.getTarget());
						} else if (maxVisit == in.getTails().size() && trans != null) {
							if (trans.source.getIndex() >= r.getIndex()) {
								if (trans.source.getIndex() > r.getIndex()) {
									trans = new Transition(a, r, i, in, in.getTarget());
								} else if (trans.target.getIndex() >= in.getTarget().getIndex()) {
									if (trans.target.getIndex() > in.getTarget().getIndex()) {
										trans = new Transition(a, r, i, in, in.getTarget());
									} else if (trans.symAlphIdx >= i) {
										if (trans.symAlphIdx > i) {
											trans = new Transition(a, r, i, in, in.getTarget());
										} else if (trans.in.getBegin() > in.getBegin()) {
											trans = new Transition(a, r, i, in, in.getTarget());
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return trans;
	}

	@SuppressWarnings("null")
	protected NavigableSet<Refinement> getMergeRefs(Transition t, StateColoring sc) {

		ProgressBarPrinter pbp = null;
		if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
			pbp = new ProgressBarPrinter(sc.getNumRedStates());
		}

		final NavigableSet<Refinement> refs = new TreeSet<>();
		for (final PDRTAState r : sc) {
			double score = tester.testMerge(r, t.target);
			if (runMode.compareTo(RunMode.DEBUG_DEEP) >= 0) {
				System.out.println("Score: " + score + " (MERGE " + r.getIndex() + " with " + t.target.getIndex() + ")");
			}
			if (score > significance && score <= 1.0) {
				score = (score - significance) / (1.0 - significance);
				final Refinement ref = new Refinement(r, t.target, score, sc);
				refs.add(ref);
			}
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				pbp.inc();
			}
		}
		return refs;
	}

	@SuppressWarnings("null")
	protected NavigableSet<Refinement> getSplitRefs(Transition t, StateColoring sc) {

		ProgressBarPrinter pbp = null;
		if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
			pbp = new ProgressBarPrinter(t.in.getTails().keySet().size() - 1);
		}

		final NavigableSet<Refinement> refs = new TreeSet<>();
		final Iterator<Integer> it = t.in.getTails().keySet().iterator();
		if (it.hasNext()) {
			int last = it.next();
			while (it.hasNext()) {
				final int cur = it.next();
				final int splitTime = (int) Math.rint(((cur - last) - 1) / 2.0) + last;
				double score = tester.testSplit(t.source, t.symAlphIdx, splitTime);
				if (runMode.compareTo(RunMode.DEBUG_DEEP) >= 0) {
					System.out.println("Score: " + score + " (SPLIT " + t.source.getIndex() + " @ (" + t.ta.getSymbol(t.symAlphIdx) + "," + splitTime + "))");
				}
				if (score < significance && score >= 0) {
					score = (significance - score) / significance;
					final Refinement ref = new Refinement(t.source, t.symAlphIdx, splitTime, score, sc);
					refs.add(ref);
				}
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					pbp.inc();
				}
				last = cur;
			}
		}
		return refs;
	}

	protected void complete(PDRTA a, StateColoring sc) {

		int counter = 0;
		Transition t;
		while ((t = getMostVisitedTrans(a, sc)) != null) {
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				if (runMode.compareTo(RunMode.DEBUG_STEPS) >= 0) {
					try {
						draw(a, true, directory + "steps/step_" + counter + ".png");
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Automaton contains " + a.getNumStates() + " states and " + a.getSize() + " transitions");
				System.out.println("Found most visited transition  " + t.toString() + "  containing " + t.in.getTails().size() + " tails");
			}
			counter++;

			if (distrCheckType.compareTo(DistributionCheckType.DISABLED) > 0) {
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.print("Checking data distribution... ");
				}
				if (checkDistribution(t.source, t.symAlphIdx, distrCheckType, sc)) {
					if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
						System.out.print("Splited interval because of data distribution into:  ");
						final NavigableMap<Integer, Interval> ins = t.source.getIntervals(t.symAlphIdx);
						for (final Entry<Integer, Interval> eIn : ins.entrySet()) {
							if (!eIn.getValue().isEmpty()) {
								System.out.print(eIn.getValue().toString() + "  ");
							}
						}
						System.out.println();
					}
					t = getMostVisitedTrans(a, sc);
					continue;
				} else if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.println("No splits because of data distributuion were perfomed in:  " + t.in.toString());
				}
			}

			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.print("Testing splits...");
			}
			final SortedSet<Refinement> splits = getSplitRefs(t, sc);
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.println("\nFound " + splits.size() + " possible splits.");
			}
			if (splits.size() > 0) {
				final Refinement r = splits.last();
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.println("#" + counter + " DO: " + r.toString());
				}
				r.refine();
			} else {
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.print("Testing merges...");
				}
				final SortedSet<Refinement> merges = getMergeRefs(t, sc);
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.println("\nFound " + merges.size() + " possible merges.");
				}
				// TODO remove
				a.checkConsistency();
				if (merges.size() > 0) {
					final Refinement r = merges.last();
					if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
						System.out.println("#" + counter + " DO: " + r.toString());
					}
					r.refine();
				} else {
					if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
						System.out.println("#" + counter + " DO: Color state " + t.target.getIndex() + " red");
					}
					sc.setRed(t.target);
				}
			}

			if (runMode.compareTo(RunMode.DEBUG) >= 0) {
				a.checkConsistency();
			}
		}

		assert (a.getNumStates() == sc.getNumRedStates());

		a.checkConsistency();
		if (runMode.compareTo(RunMode.DEBUG_STEPS) >= 0) {
			try {
				draw(a, true, directory + "steps/step_" + counter + ".png");
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected void draw(PDRTA a, boolean withInp, String path) throws IOException, InterruptedException {

		final File f = new File(directory + "tmp.aut");
		final File p = new File(path);
		if (p.exists()) {
			p.delete();
		}
		write(a, withInp, f.getAbsolutePath());
		final String[] args = { "dot", "-Tpng", f.getAbsolutePath(), "-o", p.getAbsolutePath() };
		final Process pr = Runtime.getRuntime().exec(args);
		pr.waitFor();
		f.delete();
	}

	private void write(PDRTA a, boolean withInp, String path) throws IOException {

		final File f = new File(path);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
			a.toDOTLang(bw, 0.0, withInp);
		}
	}

	protected void persistFinalResult(PDRTA a) {

		try {
			final NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(3);
			// TODO Change filename
			if (directory != null) {
				write(a, false, directory + "result" + ".aut");
				draw(a, false, directory + "result" + ".png");
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	protected String getDuration(long start, long end) {

		final NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(3);

		double dur = (end - start) / 1000.0;
		if (dur > 120.0) {
			dur = dur / 60.0;
			if (dur > 120.0) {
				dur = dur / 60.0;
				if (dur > 48.0) {
					dur = dur / 24.0;
					return nf.format(dur) + "d";
				} else {
					return nf.format(dur) + "h";
				}
			} else {
				return nf.format(dur) + "m";
			}
		} else {
			return nf.format(dur) + "s";
		}
	}

	public boolean checkDistribution(PDRTAState s, int alphIdx, DistributionCheckType type, StateColoring sc) {

		final NavigableMap<Integer, Interval> ins = s.getIntervals(alphIdx);
		if (ins.size() != 1) {
			return false;
		}

		final Interval in = ins.firstEntry().getValue();
		if (in.isEmpty()) {
			return false;
		}

		int tolerance;
		if (type.equals(DistributionCheckType.DISABLED)) {
			return false;
		} else if (type.equals(DistributionCheckType.ALL_BORDER) || type.equals(DistributionCheckType.ALL)) {
			tolerance = 0;
		} else if (type.equals(DistributionCheckType.MAD_BORDER) || type.equals(DistributionCheckType.MAD)) {
			tolerance = getToleranceMAD(in, PDRTA.getMinData());
		} else if (type.equals(DistributionCheckType.OUTLIER_BORDER) || type.equals(DistributionCheckType.OUTLIER)) {
			tolerance = getToleranceOutliers(in, PDRTA.getMinData());
		} else {
			throw new IllegalArgumentException("Nonexistent type used!");
		}

		final NavigableMap<Integer, Collection<TimedTail>> tails = in.getTails().asMap();
		final List<Integer> splits = new ArrayList<>();

		if ((type.ordinal() - 1) % 2 != 0) {
			// The types without border
			final Iterator<Entry<Integer, Collection<TimedTail>>> it = tails.entrySet().iterator();
			if (it.hasNext()) {
				Entry<Integer, Collection<TimedTail>> ePrev = it.next();
				int t = ePrev.getKey();
				if (in.getBegin() <= t - tolerance - 1) {
					splits.add(t - tolerance - 1);
				}
				while (it.hasNext()) {
					final Entry<Integer, Collection<TimedTail>> eCurr = it.next();
					t = ePrev.getKey();
					final int t2 = eCurr.getKey();
					final int diff = t2 - t - 1;
					if (diff > 2 * tolerance) {
						splits.add(t + tolerance);
						splits.add(t2 - tolerance - 1);
					}
					ePrev = eCurr;
				}
				t = ePrev.getKey();
				if (in.getEnd() > t + tolerance) {
					splits.add(t + tolerance);
				}
			}
		} else {
			int t = tails.firstKey();
			if (in.getBegin() <= t - tolerance - 1) {
				splits.add(t - tolerance - 1);
			}
			t = tails.lastKey();
			if (in.getEnd() > t + tolerance) {
				splits.add(t + tolerance);
			}
		}

		// Interval cIn = new Interval(in);
		// for (int i = 0; i < splits.size(); i++) {
		// cIn.split(splits.get(i));
		// // TODO test resulting intervals for containing more than minData
		// // tails otherwise remove split
		// }

		if (splits.size() == 0) {
			return false;
		}

		for (int i = 0; i < splits.size(); i++) {
			OperationUtil.split(s, alphIdx, splits.get(i), sc);
		}

		return true;
	}

	class Transition {
		PDRTA ta;
		PDRTAState source, target;
		Interval in;
		int symAlphIdx;

		Transition(PDRTA a, PDRTAState s, int alphIdx, Interval i, PDRTAState t) {
			assert (i.getTarget() == t);
			assert (s.getInterval(alphIdx, i.getEnd()) == i);
			ta = a;
			source = s;
			symAlphIdx = alphIdx;
			in = i;
			target = t;
		}

		@Override
		public String toString() {
			final String s = "((" + source.getIndex() + "))---" + ta.getSymbol(symAlphIdx) + "-[" + in.getBegin() + "," + in.getEnd() + "]--->(("
					+ target.getIndex() + "))";
			return s;
		}
	}

	/**
	 * Calculates the maximum allowed size for an empty interval part depending on the MAD measure and the {@link TimedTail}s using this interval.
	 * 
	 * @param minData
	 *            The minimum amount of {@link TimedTail}s needed for calculation with very few slots in the interval occupied by {@link TimedTail}s
	 * @return The maximum allowed size for an empty interval part
	 */
	private int getToleranceMAD(Interval in, int minData) {

		final NavigableSet<Integer> times = in.getTails().keySet();
		if (times.size() <= 2) {
			return getToleranceFewSlots(in, minData);
		}
		final TDoubleList diffs = new TDoubleArrayList(times.size() - 1);
		final Iterator<Integer> it = times.iterator();
		if (it.hasNext()) {
			int prev = it.next();
			while (it.hasNext()) {
				final int curr = it.next();
				diffs.add(curr - prev - 1);
				prev = curr;
			}
		}
		final double median = StatisticsUtil.calculateMedian(diffs, true);
		final double mad = StatisticsUtil.calculateMAD(diffs, median);
		return (int) Math.floor(((median + 2.5 * mad) / 2.0) + 1.0);
	}

	/**
	 * Calculates the maximum allowed size for an empty interval part depending on the IQR outlier measure and the {@link TimedTail}s using this interval.
	 * 
	 * @param minData
	 *            The minimum amount of {@link TimedTail}s needed for calculation with very few slots in the interval occupied by {@link TimedTail}s
	 * @return The maximum allowed size for an empty interval part
	 */
	private int getToleranceOutliers(Interval in, int minData) {

		final NavigableSet<Integer> times = in.getTails().keySet();
		if (times.size() <= 2) {
			return getToleranceFewSlots(in, minData);
		}
		final TDoubleList diffs = new TDoubleArrayList(times.size() - 1);
		final Iterator<Integer> it = times.iterator();
		if (it.hasNext()) {
			int prev = it.next();
			while (it.hasNext()) {
				final int curr = it.next();
				diffs.add(curr - prev - 1);
				prev = curr;
			}
		}
		diffs.sort();
		final double q1 = StatisticsUtil.calculateQ1(diffs, false);
		final double q3 = StatisticsUtil.calculateQ3(diffs, false);
		return (int) Math.floor(((q3 + (q3 - q1) * 1.5) / 2.0) + 1.0);
	}

	/**
	 * Calculates the maximum allowed size for an empty interval part when only few {@link TimedTail}s use this interval. The allowed size depends on the
	 * parameter for the minimum amount of {@link TimedTail}s and the distance between the occupied slots.
	 * 
	 * @param minData
	 *            The minimum amount of {@link TimedTail}s
	 * @return The maximum allowed size for an empty interval part
	 */
	private int getToleranceFewSlots(Interval in, int minData) {

		final NavigableMap<Integer, Collection<TimedTail>> tails = in.getTails().asMap();
		final int slots = tails.size();
		assert (slots > 0 && slots <= 2);
		if (slots == 1) {
			final int size = tails.firstEntry().getValue().size();
			if (size < (minData / 2.0)) {
				return (int) Math.floor((in.getEnd() - in.getBegin() + 1) * 0.05 + 1.0);
			} else {
				return 0;
			}
		} else {
			final int t1 = tails.firstKey();
			final int s1 = tails.get(t1).size();
			final int t2 = tails.lastKey();
			final int s2 = tails.get(t2).size();
			final double perc = (double) (t2 - t1 - 1) / (double) (in.getEnd() - in.getBegin() - 1);
			if (s1 >= minData && s2 >= minData && perc >= 0.2) {
				return (int) Math.floor((in.getEnd() - in.getBegin() + 1) * 0.05 + 1.0);
			} else if ((s1 >= minData || s2 >= minData) && perc >= 0.2) {
				return (int) Math.floor((in.getEnd() - in.getBegin() + 1) * 0.075 + 1.0);
			} else {
				return (int) Math.floor((t2 - t1 - 1) / 2.0 + 1.0);
			}
		}
	}

}
