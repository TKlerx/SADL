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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.modellearner.rtiplus.boolop.AndOperator;
import sadl.modellearner.rtiplus.boolop.BooleanOperator;
import sadl.modellearner.rtiplus.boolop.OrOperator;
import sadl.modellearner.rtiplus.tester.FishersMethodTester;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.LikelihoodValue;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAInput;
import sadl.models.pdrta.PDRTAState;
import sadl.models.pdrta.TimedTail;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
public class SimplePDRTALearner implements ProbabilisticModelLearner {

	public enum OperationTesterType {
		LRT, LRT_ADV, NAIVE_LRT, FM, FM_ADV
	}

	public enum DistributionCheckType {
		DISABLED, STRICT_BORDER, STRICT, MAD_BORDER, MAD, OUTLIER_BORDER, OUTLIER
	}

	public enum SplitPosition {
		LEFT, MIDDLE, RIGHT
	}

	// The boolean operators for the pooling strategy used by Verwer's LRT and FM
	// 0: Operator for pooling (thesis: AND, impl: AND, own: AND)
	// 1: Operator for pool discarding (thesis: missing, impl: [LRT: OR, FM: AND], own: AND)
	// 2: Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
	public static BooleanOperator[] bOp;

	static final Logger logger = LoggerFactory.getLogger(SimplePDRTALearner.class);

	long startTime;

	final double significance;
	final DistributionCheckType distrCheckType;
	final SplitPosition splitPos;
	final String histBinsStr;
	final OperationTester tester;

	Path directory;

	PDRTA mainModel;

	public SimplePDRTALearner(double sig, String histBins, OperationTesterType testerType, DistributionCheckType distrCheckType, SplitPosition splitPos,
			String boolOps, String dir) {

		if (sig < 0.0 || sig > 1.0) {
			throw new IllegalArgumentException("Wrong parameter: SIGNIFICANCE must be a decision (float) value between 0.0 and 1.0");
		}

		parseBoolOps(boolOps);

		this.significance = sig;
		this.distrCheckType = distrCheckType;
		this.histBinsStr = histBins;
		this.splitPos = splitPos;
		try {
			this.directory = initStepsDir(dir);
		} catch (final IOException e) {
			logger.warn("Error when preparing steps directory: ", e.getMessage());
			directory = null;
		}

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

	private Path initStepsDir(String dir) throws IOException {

		if (dir != null) {
			final Path p = Paths.get(dir, "steps");
			if (Files.exists(directory) && Files.isDirectory(directory)) {
				Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
						if (e == null) {
							Files.delete(d);
							return FileVisitResult.CONTINUE;
						} else {
							throw e;
						}
					}
				});
			}
			Files.createDirectories(directory);
			return p;
		} else {
			return null;
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
					logger.error("The symbol '{}' is not appropriate for boolean operator. Using default 'A'.", c[i]);
					break;
			}
		}
	}

	@Override
	public ProbabilisticModel train(TimedInput trainingSequences) {

		logger.info("RTI+: Building automaton from input sequences");

		final boolean expand = distrCheckType.compareTo(DistributionCheckType.STRICT) > 0;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histBinsStr, expand);
		final PDRTA a = new PDRTA(in);

		// TODO log new params
		logger.info("Parameters are: significance={} distrCheckType={}", significance, distrCheckType);
		logger.info("Histogram Bins are: {}", a.getHistBinsString());

		logger.info("*** Performing simple RTI+ ***");
		startTime = System.currentTimeMillis();
		final StateColoring sc = new StateColoring(a);
		sc.setRed(a.getRoot());
		tester.setColoring(sc);
		mainModel = a;
		complete(a, sc);

		logger.info("Final PDRTA contains {} states and {} transitions", a.getNumberOfStates(), a.getSize());
		// TODO Check why Likelihood is 0.0 here
		logger.info("Trained PDRTA with quality: Likelihood={} and AIC={}", Math.exp(NaiveLikelihoodRatioTester.calcLikelihood(a).getRatio()), calcAIC(a));

		a.cleanUp();

		logger.info("Time: {}", getDuration(startTime, System.currentTimeMillis()));
		logger.info("END");

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

	protected NavigableSet<Refinement> getMergeRefs(Transition t, StateColoring sc) {

		final NavigableSet<Refinement> refs = new TreeSet<>();
		//sequential
		for (final PDRTAState r : sc) {
			double score = tester.testMerge(r, t.target);
			if (mainModel == t.ta) {
				logger.trace("Score: {} (MERGE {} with {})", score, r.getIndex(), t.target.getIndex());
			}
			if (score > significance && score <= 1.0) {
				score = (score - significance) / (1.0 - significance);
				final Refinement ref = new Refinement(r, t.target, score, sc);
				refs.add(ref);
			}
		}
		//parallel (not yet checked for determinism)
		// // final NavigableSet<Refinement> safeRefs = Collections.synchronizedNavigableSet(refs);
		// sc.getRedStates().parallelStream().forEach(red -> {
		// double score = tester.testMerge(red, t.target);
		// if (mainModel == t.ta) {
		// logger.trace("Score: {} (MERGE {} with {})", score, red.getIndex(), t.target.getIndex());
		// }
		// if (score > significance && score <= 1.0) {
		// score = (score - significance) / (1.0 - significance);
		// final Refinement ref = new Refinement(red, t.target, score, sc);
		// l1.lock();
		// refs.add(ref);
		// l1.unlock();
		// // safeRefs.add(ref);
		// }
		// });
		return refs;
	}

	protected NavigableSet<Refinement> getSplitRefs(Transition t, StateColoring sc) {

		final NavigableSet<Refinement> refs = new TreeSet<>();
		//sequential
		final Iterator<Integer> it = t.in.getTails().keySet().iterator();
		if (it.hasNext()) {
			int last = it.next();
			while (it.hasNext()) {
				final int cur = it.next();
				int splitTime = -1;
				switch (splitPos) {
					case LEFT:
						splitTime = last;
						break;
					case MIDDLE:
						splitTime = (int) Math.rint(((cur - last) - 1) / 2.0) + last;
						break;
					case RIGHT:
						splitTime = cur - 1;
						break;
					default:
						splitTime = (int) Math.rint(((cur - last) - 1) / 2.0) + last;
						break;
				}
				double score = tester.testSplit(t.source, t.symAlphIdx, splitTime);
				if (mainModel == t.ta) {
					logger.trace("Score: {} (SPLIT {} @ ({},{}))", score, t.source.getIndex(), t.ta.getSymbol(t.symAlphIdx), splitTime);
				}
				if (score < significance && score >= 0) {
					score = (significance - score) / significance;
					final Refinement ref = new Refinement(t.source, t.symAlphIdx, splitTime, score, sc);
					refs.add(ref);
				}
				last = cur;
			}
			//parallel (not yet checked for determinism)
			// final TIntList splitTimes = new TIntArrayList();
			// if (it.hasNext()) {
			// int last = it.next();
			// while (it.hasNext()) {
			// final int cur = it.next();
			// int splitTime = -1;
			// switch (splitPos) {
			// case LEFT:
			// splitTime = last;
			// break;
			// case MIDDLE:
			// splitTime = (int) Math.rint(((cur - last) - 1) / 2.0) + last;
			// break;
			// case RIGHT:
			// splitTime = cur - 1;
			// break;
			// default:
			// splitTime = (int) Math.rint(((cur - last) - 1) / 2.0) + last;
			// break;
			// }
			// splitTimes.add(splitTime);
			// last = cur;
			// }
			// // final NavigableSet<Refinement> safeRefs = Collections.synchronizedNavigableSet(refs);
			// Arrays.stream(splitTimes.toArray()).parallel().forEach(splitTime -> {
			// double score = tester.testSplit(t.source, t.symAlphIdx, splitTime);
			// if (mainModel == t.ta) {
			// logger.trace("Score: {} (SPLIT {} @ ({},{}))", score, t.source.getIndex(), t.ta.getSymbol(t.symAlphIdx), splitTime);
			// }
			// if (score < significance && score >= 0) {
			// score = (significance - score) / significance;
			// final Refinement ref = new Refinement(t.source, t.symAlphIdx, splitTime, score, sc);
			// l2.lock();
			// refs.add(ref);
			// l2.unlock();
			// // safeRefs.add(ref);
			// }
			// });
		}
		return refs;
	}

	void complete(PDRTA a, StateColoring sc) {

		final boolean preExit = (bOp[2] instanceof OrOperator) && distrCheckType.equals(DistributionCheckType.DISABLED);
		if (mainModel == a && preExit) {
			logger.info("Pre-Exiting algorithm when number of tails falls below minData");
		}

		int counter = 0;
		Transition t;
		while ((t = getMostVisitedTrans(a, sc)) != null && !(preExit && t.in.getTails().size() < PDRTA.getMinData())) {
			if (mainModel == a) {
				if (directory != null) {
					draw(a, true, directory, counter);
				}
				logger.debug("Automaton contains {} states and {} transitions", a.getNumberOfStates(), a.getSize());
				logger.debug("Found most visited transition  {}  containing {} tails", t.toString(), t.in.getTails().size());
			}
			counter++;

			if (!distrCheckType.equals(DistributionCheckType.DISABLED)) {
				if (mainModel == a) {
					logger.debug("Checking data distribution");
				}
				final List<Interval> idaIns = checkDistribution(t.source, t.symAlphIdx, distrCheckType, sc);
				if (idaIns.size() > 0) {
					if (mainModel == a) {
						logger.debug("#{} DO: Split interval due to IDA into {} intervals", counter, idaIns.size());
						// TODO Printing the intervals may be to expensive just for logging
						final StringBuilder sb = new StringBuilder();
						for (final Interval in : idaIns) {
							sb.append("  ");
							sb.append(in.toString());
						}
						logger.trace("Resulting intervals are:{}", sb.toString());
					}
					continue;
				} else {
					if (mainModel == a) {
						logger.debug("No splits because of data distributuion were perfomed in:  {}", t.in.toString());
					}
					if (bOp[2] instanceof OrOperator && t.in.getTails().size() < PDRTA.getMinData()) {
						// Shortcut for skipping merges and splits when OR is selected
						if (mainModel == a) {
							logger.debug("#{} DO: Color state {} red", counter, t.target.getIndex());
						}
						sc.setRed(t.target);
						continue;
					}
				}
			}

			if (mainModel == a) {
				logger.debug("Testing splits");
			}
			final SortedSet<Refinement> splits = getSplitRefs(t, sc);
			if (mainModel == a) {
				logger.debug("Found {} possible splits", splits.size());
			}
			if (splits.size() > 0) {
				final Refinement r = splits.last();
				if (mainModel == a) {
					logger.debug("#{} DO: {}", counter, r.toString());
				}
				r.refine();
			} else {
				if (mainModel == a) {
					logger.debug("Testing merges");
				}
				final SortedSet<Refinement> merges = getMergeRefs(t, sc);
				if (mainModel == a) {
					logger.debug("Found {} possible merges", merges.size());
				}
				if (merges.size() > 0) {
					final Refinement r = merges.last();
					if (mainModel == a) {
						logger.debug("#{} DO: {}", counter, r.toString());
					}
					r.refine();
				} else {
					if (mainModel == a) {
						logger.debug("#{} DO: Color state {} red", counter, t.target.getIndex());
					}
					sc.setRed(t.target);
				}
			}

			if (Settings.isDebug()) {
				a.checkConsistency();
			}
		}

		assert (a.getNumberOfStates() == sc.getNumRedStates());

		a.checkConsistency();
		if (directory != null) {
			draw(a, true, directory, counter);
		}
	}

	void draw(PDRTA a, boolean withInp, Path path, int counter) {

		final String fileName = "step_" + counter;
		final Path gvFile = path.resolve(fileName + ".gv");
		final Path pngFile = path.resolve(fileName + ".png");
		try (final BufferedWriter bw = Files.newBufferedWriter(gvFile)) {
			a.toDOTLang(bw, 0.0, withInp);
		} catch (final Exception e) {
			logger.error("Not able to store PDRTA in Graphviz format: {}", e.getMessage());
		}
		final String[] args = { "dot", "-Tpng", gvFile.toAbsolutePath().toString(), "-o", pngFile.toAbsolutePath().toString() };
		Process pr = null;
		try {
			pr = Runtime.getRuntime().exec(args);
		} catch (final Exception e) {
			logger.warn("Could not create plot of PDRTA: {}", e.getMessage());
		} finally {
			if (pr != null) {
				try {
					pr.waitFor();
				} catch (final InterruptedException e) {
				}
			}
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

	public List<Interval> checkDistribution(PDRTAState s, int alphIdx, DistributionCheckType type, StateColoring sc) {

		final NavigableMap<Integer, Interval> ins = s.getIntervals(alphIdx);
		if (ins.size() != 1) {
			return Collections.emptyList();
		}

		final Interval in = ins.firstEntry().getValue();
		if (in.isEmpty()) {
			return Collections.emptyList();
		}

		int tolerance;
		if (type.equals(DistributionCheckType.DISABLED)) {
			return Collections.emptyList();
		} else if (type.equals(DistributionCheckType.STRICT_BORDER) || type.equals(DistributionCheckType.STRICT)) {
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
			return Collections.emptyList();
		}

		final List<Interval> resultingIns = new ArrayList<>(splits.size() + 1);
		Pair<Interval, Interval> splittedIns = null;
		for (int i = 0; i < splits.size(); i++) {
			splittedIns = OperationUtil.split(s, alphIdx, splits.get(i), sc);
			if (!splittedIns.getLeft().isEmpty()) {
				resultingIns.add(splittedIns.getLeft());
			}
		}
		if (splittedIns != null && !splittedIns.getRight().isEmpty()) {
			resultingIns.add(splittedIns.getRight());
		}

		return resultingIns;
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
		return (int) Math.ceil(((median + 2.5 * mad) / 2.0));
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
		return (int) Math.ceil(((q3 + (q3 - q1) * 1.5) / 2.0));
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
				return (int) Math.ceil((in.getEnd() - in.getBegin() + 1) * 0.05);
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
				return (int) Math.ceil((in.getEnd() - in.getBegin() + 1) * 0.05);
			} else if ((s1 >= minData || s2 >= minData) && perc >= 0.2) {
				return (int) Math.ceil((in.getEnd() - in.getBegin() + 1) * 0.075);
			} else {
				return (int) Math.ceil((t2 - t1 - 1) / 2.0);
			}
		}
	}

	double calcAIC(PDRTA a) {

		final LikelihoodValue lv = NaiveLikelihoodRatioTester.calcLikelihood(a);
		return (2.0 * lv.getParam()) - (2.0 * lv.getRatio());
	}

}
