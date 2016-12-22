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
package sadl.modellearner.rtiplus;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TIntList;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.modellearner.rtiplus.analysis.DistributionAnalysis;
import sadl.modellearner.rtiplus.analysis.QuantileAnalysis;
import sadl.modellearner.rtiplus.boolop.AndOperator;
import sadl.modellearner.rtiplus.boolop.BooleanOperator;
import sadl.modellearner.rtiplus.boolop.OrOperator;
import sadl.modellearner.rtiplus.tester.LikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.LikelihoodValue;
import sadl.modellearner.rtiplus.tester.NaiveLikelihoodRatioTester;
import sadl.modellearner.rtiplus.tester.OperationTester;
import sadl.models.pdrta.Interval;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAInput;
import sadl.models.pdrta.PDRTAState;
import sadl.utils.IoUtils;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
public class SimplePDRTALearner implements ProbabilisticModelLearner {

	public enum SplitPosition {
		LEFT, MIDDLE, RIGHT
	}

	static final Logger logger = LoggerFactory.getLogger(SimplePDRTALearner.class);

	long startTime;

	final double significance;
	final DistributionAnalysis histoBinDistriAnalysis;

	// The boolean operators for the pooling strategy used by Verwer's LRT and FM
	// 0: Operator for pooling (thesis: AND, impl: AND, own: AND)
	// 1: Operator for pool discarding (thesis: missing, impl: [LRT: OR, FM: AND], own: AND)
	// 2: Operator for calculation interruption (thesis: AND, impl: OR, own: AND)
	public static BooleanOperator[] bOp;
	final OperationTester tester;
	final SplitPosition splitPos;
	final boolean doNotMergeWithRoot;
	final boolean testParallel;

	final DistributionAnalysis intervalDistriAnalysis;
	final boolean removeBorderGapsOnly;
	final boolean performIDAActively;
	final double intervalExpRate;

	Path directory;

	PDRTA mainModel;

	/**
	 * Creates a RTI+ learner as it was implemented by Verwer
	 * 
	 * @param sig
	 * @param numQuantiles
	 * @param dir
	 */
	public SimplePDRTALearner(double sig, int numQuantiles, boolean doNotMergeWithRoot, boolean testParallel, Path dir) {
		this(sig, new QuantileAnalysis(numQuantiles), new LikelihoodRatioTester(false), SplitPosition.LEFT, doNotMergeWithRoot, testParallel, "AOO", dir);
	}

	/**
	 * Creates a RTI+ learner without IDA
	 * 
	 * @param sig
	 * @param histoBinDistritutionAnalysis
	 * @param operationTester
	 * @param splitPos
	 * @param boolOps
	 * @param dir
	 */
	public SimplePDRTALearner(double sig, DistributionAnalysis histoBinDistritutionAnalysis, OperationTester operationTester, SplitPosition splitPos,
			boolean doNotMergeWithRoot, boolean testParallel, String boolOps, Path dir) {
		this(sig, histoBinDistritutionAnalysis, operationTester, splitPos, doNotMergeWithRoot, testParallel, null, true, false, 0.0, boolOps, dir);
	}

	public SimplePDRTALearner(double sig, DistributionAnalysis histoBinDistritutionAnalysis, OperationTester operationTester, SplitPosition splitPos,
			boolean doNotMergeWithRoot, boolean testParallel, DistributionAnalysis intervalDistributionAnalysis, boolean removeBorderGapsOnly,
			boolean performIDAActively, double intervalExpansionRate, String boolOps, Path dir) {

		if (sig < 0.0 || sig > 1.0) {
			throw new IllegalArgumentException("Wrong parameter: SIGNIFICANCE must be a decision (float) value between 0.0 and 1.0");
		}

		if (histoBinDistritutionAnalysis == null) {
			throw new IllegalArgumentException("The distribution analysis for the hisogram bins must not be null");
		}

		if (operationTester == null) {
			throw new IllegalArgumentException("The operation tester must not be null");
		}

		if (intervalDistributionAnalysis != null && intervalExpansionRate < 0.0) {
			throw new IllegalArgumentException("The interval expansion rate must be positive");
		}

		this.significance = sig;
		this.histoBinDistriAnalysis = histoBinDistritutionAnalysis;

		this.tester = operationTester;
		this.splitPos = splitPos;
		this.doNotMergeWithRoot = doNotMergeWithRoot;
		this.testParallel = testParallel;
		parseBoolOps(boolOps);

		this.intervalDistriAnalysis = intervalDistributionAnalysis;
		this.removeBorderGapsOnly = removeBorderGapsOnly;
		this.performIDAActively = performIDAActively;
		this.intervalExpRate = intervalExpansionRate;

		try {
			this.directory = initStepsDir(dir);
		} catch (final IOException e) {
			logger.warn("Error when preparing steps directory: ", e.getMessage());
			directory = null;
		}
	}

	private Path initStepsDir(Path dir) throws IOException {

		if (dir != null) {
			final Path p = dir.resolve("steps");
			if (Files.exists(p) && Files.isDirectory(p)) {
				Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
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
			Files.createDirectories(p);
			return p;
		} else {
			return null;
		}
	}

	@SuppressWarnings("boxing")
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
	@SuppressWarnings("boxing")
	public ProbabilisticModel train(TimedInput trainingSequences) {

		logger.info("RTI+: Building automaton from input sequences");

		final boolean expand = intervalDistriAnalysis != null;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histoBinDistriAnalysis, expand ? intervalExpRate : 0.0);
		final PDRTA a = new PDRTA(in);

		// TODO log new params
		logger.info("Parameters are: significance={} distrCheckType={}", significance);
		logger.info("Histogram Bins are: {}", a.getHistBinsString());

		logger.info("*** Performing simple RTI+ ***");
		startTime = System.currentTimeMillis();
		final StateColoring sc = new StateColoring(a);
		sc.setRed(a.getRoot());
		tester.setColoring(sc);
		mainModel = a;
		complete(a, sc);

		if (intervalDistriAnalysis != null && !performIDAActively) {
			logger.info("Running IDA passively after training");
			runIDAPassively(a);
		}

		logger.info("Final PDRTA contains {} states and {} transitions", a.getStateCount(), a.getSize());
		// TODO Check why Likelihood is 0.0 here
		logger.info("Trained PDRTA with quality: Likelihood={} and AIC={}", Math.exp(NaiveLikelihoodRatioTester.calcLikelihood(a).getRatio()), calcAIC(a));

		a.cleanUp();

		logger.info("Time: {}", getDuration(startTime, System.currentTimeMillis()));
		logger.info("END");

		return a;
	}

	protected void runIDAPassively(PDRTA a) {

		for (final PDRTAState s : a.getStates()) {
			for (int i = 0; i < a.getAlphSize(); i++) {
				// Get all present intervals for s and symbol i
				final Optional<List<Interval>> ins = s.getIntervals(i).map(m -> m.values().stream().filter(in -> in != null).collect(Collectors.toList()));
				if (ins.isPresent()) {
					for (final Interval in : ins.get()) {
						assert (!in.isEmpty());
						final PDRTAState target = in.getTarget();
						final List<Interval> newIntervals = perfomIDA(s, i, in.getEnd(), null);
						for (final Interval newIn : newIntervals) {
							newIn.setTarget(target);
						}
					}
				}
			}
		}
	}

	protected Transition getMostVisitedTrans(PDRTA a, StateColoring sc) {

		int maxVisit = 0;
		Transition trans = null;
		for (final PDRTAState r : sc) {
			for (int i = 0; i < a.getAlphSize(); i++) {
				final Optional<Collection<Interval>> ins = r.getIntervals(i).map(m -> m.values());
				if (ins.isPresent()) {
					for (final Interval in : ins.get()) {
						if (in != null) {
							assert (!in.isEmpty());
							assert (sc.isBlue(in.getTarget()) || sc.isRed(in.getTarget()));
							if (sc.isBlue(in.getTarget())) {
								final Transition candidate = new Transition(a, r, i, in, in.getTarget());
								if (maxVisit < in.getTails().size() || (maxVisit == in.getTails().size() && candidate.compareTo(trans) > 0)) {
									maxVisit = in.getTails().size();
									trans = candidate;
								}
							}
						}
					}
				}
			}
		}
		return trans;
	}

	@SuppressWarnings("boxing")
	protected NavigableSet<Refinement> getMergeRefs(Transition t, StateColoring sc) {

		final Function<PDRTAState, Optional<Refinement>> testMerge = red -> {
			if (!doNotMergeWithRoot || !red.equals(red.getPDRTA().getRoot())) {
				double score = tester.testMerge(red, t.target);
				if (mainModel == t.ta) {
					logger.trace("Score: {} (MERGE {} with {})", score, red.getIndex(), t.target.getIndex());
				}
				if (score > significance && score <= 1.0) {
					score = (score - significance) / (1.0 - significance);
					final Refinement ref = new Refinement(red, t.target, score, sc);
					return Optional.of(ref);
				}
			}
			return Optional.empty();
		};

		Stream<PDRTAState> stream;
		if (testParallel) {
			stream = sc.getRedStates().parallelStream();
		} else {
			stream = sc.getRedStates().stream();
		}

		return stream.map(testMerge).filter(o -> o.isPresent()).map(o -> o.get()).collect(Collectors.toCollection(TreeSet::new));
	}

	@SuppressWarnings("boxing")
	protected NavigableSet<Refinement> getSplitRefs(Transition t, StateColoring sc) {

		final Set<Integer> splitTimes = new HashSet<>();

		final Iterator<Integer> it = t.in.getTails().keySet().iterator();
		if (it.hasNext()) {
			int last = it.next().intValue();
			while (it.hasNext()) {
				final int cur = it.next().intValue();
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
				splitTimes.add(splitTime);
				last = cur;
			}
		}

		final Function<Integer, Optional<Refinement>> testSplit = splitTime -> {
			double score = tester.testSplit(t.source, t.symAlphIdx, splitTime);
			if (mainModel == t.ta) {
				logger.trace("Score: {} (SPLIT {} @ ({},{}))", score, t.source.getIndex(), t.ta.getSymbol(t.symAlphIdx), splitTime);
			}
			if (score < significance && score >= 0) {
				score = (significance - score) / significance;
				final Refinement ref = new Refinement(t.source, t.symAlphIdx, splitTime, score, sc);
				return Optional.of(ref);
			}
			return Optional.empty();
		};

		Stream<Integer> stream;
		if (testParallel) {
			stream = splitTimes.parallelStream();
		} else {
			stream = splitTimes.stream();
		}

		return stream.map(testSplit).filter(o -> o.isPresent()).map(o -> o.get()).collect(Collectors.toCollection(TreeSet::new));
	}

	@SuppressWarnings("boxing")
	void complete(PDRTA a, StateColoring sc) {

		final boolean preExit = (bOp[2] instanceof OrOperator) && (intervalDistriAnalysis == null);
		if (mainModel == a && preExit) {
			logger.info("Pre-Exiting algorithm when number of tails falls below minData");
		}

		int counter = 0;
		Transition t;
		while ((t = getMostVisitedTrans(a, sc)) != null && !(preExit && t.in.getTails().size() < PDRTA.getMinData())) {
			if (mainModel == a) {
				if (directory != null) {
					draw(a, sc, true, directory, counter);
				}
				logger.debug("Automaton contains {} states and {} transitions", a.getStateCount(), a.getSize());
				logger.debug("Found most visited transition  {}  containing {} tails", t.toString(), t.in.getTails().size());
			}
			counter++;

			if (intervalDistriAnalysis != null && performIDAActively) {
				if (mainModel == a) {
					logger.debug("Checking data distribution");
				}
				final List<Interval> idaIns = perfomIDA(t.source, t.symAlphIdx, t.in.getEnd(), sc);
				if (idaIns.size() > 0) {
					if (mainModel == a) {
						logger.debug("#{} DO: Split interval due to IDA into {} intervals", counter, idaIns.size());
						if (logger.isTraceEnabled()) {
							final StringBuilder sb = new StringBuilder();
							for (final Interval in : idaIns) {
								sb.append("  ");
								sb.append(in.toString());
							}
							logger.trace("Resulting intervals are:{}", sb.toString());
						}
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

		if (preExit) {
			assert (a.getStateCount() >= sc.getNumRedStates());
		} else {
			assert (a.getStateCount() == sc.getNumRedStates());
		}

		a.checkConsistency();
		if (directory != null) {
			draw(a, sc, true, directory, counter);
		}
	}

	void draw(PDRTA a, StateColoring sc, boolean withInp, Path path, int counter) {

		final String fileName = "step_" + counter;
		final Path gvFile = path.resolve(fileName + ".gv");
		final Path pngFile = path.resolve(fileName + ".png");
		try (final BufferedWriter bw = Files.newBufferedWriter(gvFile)) {
			a.toDOTLang(bw, 0.0, withInp, sc);
		} catch (final Exception e) {
			logger.error("Not able to store PDRTA in Graphviz format: {}", e.getMessage());
		}
		IoUtils.runGraphviz(gvFile, pngFile);
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

	public List<Interval> perfomIDA(PDRTAState s, int alphIdx, int timeDelay, StateColoring sc) {

		final Optional<NavigableMap<Integer, Interval>> ins = s.getIntervals(alphIdx);
		if (!ins.isPresent()) {
			throw new IllegalArgumentException("No intervals exist for ((" + s.getIndex() + "))--" + s.getPDRTA().getSymbol(alphIdx) + "--");
		}

		if (sc != null && ins.get().size() != 1) {
			return Collections.emptyList();
		}

		final Interval in = ins.get().ceilingEntry(new Integer(timeDelay)).getValue();
		if (in == null) {
			throw new IllegalArgumentException(
					"Tansition ((" + s.getIndex() + "))--" + s.getPDRTA().getSymbol(alphIdx) + "-[..., " + timeDelay + ", ...]--> does not exist");
		}

		assert (!in.isEmpty());

		final SortedMap<Integer, Integer> tails = in.getTails().asMap().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> new Integer(e.getValue().size()), (e1, e2) -> {
					throw new RuntimeException();
				}, TreeMap::new));
		final Pair<TIntList, TIntList> timeDistr = OperationUtil.distributionsMapToLists(tails);
		final TIntList splits = intervalDistriAnalysis.performAnalysis(timeDistr.getLeft(), timeDistr.getRight(), in.getBegin(), in.getEnd());

		// Interval cIn = new Interval(in);
		// for (int i = 0; i < splits.size(); i++) {
		// cIn.split(splits.get(i));
		// // TODO test resulting intervals for containing more than minData
		// // tails otherwise remove split
		// }

		if (splits.size() == 0) {
			return Collections.emptyList();
		}

		final int minTime = tails.firstKey().intValue();
		final int maxTime = tails.lastKey().intValue();
		final List<Interval> resultingIns = new ArrayList<>(removeBorderGapsOnly ? 3 : (splits.size() + 1));
		Pair<Optional<Interval>, Optional<Interval>> splittedIns = null;
		for (int i = 0; i < splits.size(); i++) {
			if (removeBorderGapsOnly && i > 0 && i < (splits.size() - 1) && (splits.get(i) < minTime || splits.get(i) >= maxTime)) {
				// If only border gaps should be removed, only the first an last split will be performed if they are smaller the min time delay or greater equal
				// the max time delay. The rest will be skipped.
				continue;
			}
			splittedIns = OperationUtil.split(s, alphIdx, splits.get(i), sc);
			if (splittedIns.getLeft().isPresent()) {
				resultingIns.add(splittedIns.getLeft().get());
			}
		}
		if (splittedIns != null && splittedIns.getRight().isPresent()) {
			resultingIns.add(splittedIns.getRight().get());
		}

		return resultingIns;
	}

	double calcAIC(PDRTA a) {

		final LikelihoodValue lv = NaiveLikelihoodRatioTester.calcLikelihood(a);
		return (2.0 * lv.getParam()) - (2.0 * lv.getRatio());
	}

	static class Transition implements Comparable<Transition> {
		PDRTA ta;
		PDRTAState source, target;
		Interval in;
		int symAlphIdx;

		Transition(PDRTA a, PDRTAState s, int alphIdx, Interval i, PDRTAState t) {
			assert (i.getTarget() == t);
			assert (s.getInterval(alphIdx, i.getEnd()).get() == i);
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

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;
			result = prime * result + ((in == null) ? 0 : in.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + symAlphIdx;
			result = prime * result + ((target == null) ? 0 : target.hashCode());
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
			final Transition other = (Transition) obj;
			if (in == null) {
				if (other.in != null) {
					return false;
				}
			} else if (!in.equals(other.in)) {
				return false;
			}
			if (source == null) {
				if (other.source != null) {
					return false;
				}
			} else if (!source.equals(other.source)) {
				return false;
			}
			if (symAlphIdx != other.symAlphIdx) {
				return false;
			}
			if (target == null) {
				if (other.target != null) {
					return false;
				}
			} else if (!target.equals(other.target)) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(Transition t) {

			if (this.source.getIndex() > t.source.getIndex()) {
				return 1;
			} else if (this.source.getIndex() < t.source.getIndex()) {
				return -1;
			}

			if (this.target.getIndex() > t.target.getIndex()) {
				return 1;
			} else if (this.target.getIndex() < t.target.getIndex()) {
				return -1;
			}

			if (this.symAlphIdx > t.symAlphIdx) {
				return 1;
			} else if (this.symAlphIdx < t.symAlphIdx) {
				return -1;
			}

			if (this.in.getBegin() > t.in.getBegin()) {
				return 1;
			} else if (this.in.getBegin() < t.in.getBegin()) {
				return -1;
			}

			return 0;
		}
	}

}
