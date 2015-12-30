package sadl.modellearner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import sadl.constants.PTAOrdering;
import sadl.input.TimedInput;
import sadl.models.FTA;
import sadl.models.PDFA;
import sadl.structure.Transition;
import sadl.utils.IoUtils;
import sadl.utils.Settings;

@Deprecated
/**
 * Does not work. Is buggy somewhere.
 * 
 * @author Timo
 *
 */
public class AlergiaRedBlue implements PdfaLearner {
	private final double alpha;
	FTA pta;
	PTAOrdering ordering = PTAOrdering.TopDown;
	int mergeT0 = 0;
	boolean recursiveMergeTest = true;
	private final Logger logger = org.slf4j.LoggerFactory.getLogger(Alergia.class);

	public AlergiaRedBlue(double alpha) {
		this.alpha = alpha;
	}

	int debugStepCounter = 0;
	static final int RED = 1;
	static final int BLUE = 2;
	static final int WHITE = 3;

	@Override
	public PDFA train(TimedInput trainingSequences) {
		logger.info("Starting to learn PDFA with ALERGIA-red-blue (java)...");
		final IntBinaryOperator mergeTest = this::alergiaCompatibilityTest;
		pta = new FTA(trainingSequences);
		final TIntIntMap stateColoring = new TIntIntHashMap();
		final Set<Integer> redStates = new LinkedHashSet<>();
		final Queue<Integer> blueStates = new LinkedList<>();

		stateColoring.put(PDFA.START_STATE, RED);
		redStates.add(PDFA.START_STATE);

		final TIntList startStateSuccs = pta.getSuccessors(PDFA.START_STATE);
		startStateSuccs.sort();
		for (int i = 0; i < startStateSuccs.size(); i++) {
			final int blueState = startStateSuccs.get(i);
			stateColoring.put(blueState, BLUE);
			blueStates.add(blueState);
		}

		while (!blueStates.isEmpty()) {
			final int blueState = blueStates.poll();
			if (!pta.containsState(blueState) || stateColoring.get(blueState) == RED) {
				continue;
			}
			final Iterator<Integer> iterator = redStates.iterator();
			inner: while (iterator.hasNext()) {
				// inner: for (final Integer redState : redStates) {
				final Integer redState = iterator.next();
				if (!pta.containsState(redState)) {
					iterator.remove();
					// redStates.remove(redState);
					continue inner;
				}
				if (compatible(redState, blueState, mergeTest)) {
					final int stepValue = debugStepCounter;
					debugStepCounter++;
					if (Settings.isDebug()) {
						try {
							final String fileName = "pta_" + (stepValue) + "-0";
							final Path graphVizFile = Paths.get(fileName + ".gv");
							pta.toPdfa().toGraphvizFile(graphVizFile, false);
							final Path pngFile = Paths.get(fileName + ".png");
							IoUtils.runGraphviz(graphVizFile, pngFile);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
					pta.merge(redState, blueState);
					if (Settings.isDebug()) {
						try {
							final String fileName = "pta_" + (stepValue) + "-1";
							final Path graphVizFile = Paths.get(fileName + ".gv");
							pta.toPdfa().toGraphvizFile(graphVizFile, false);
							final Path pngFile = Paths.get(fileName + ".png");
							IoUtils.runGraphviz(graphVizFile, pngFile);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
					pta.determinize();
					if (Settings.isDebug()) {
						try {
							final String fileName = "pta_" + (stepValue) + "-2";
							final Path graphVizFile = Paths.get(fileName + ".gv");
							pta.toPdfa().toGraphvizFile(graphVizFile, false);
							final Path pngFile = Paths.get(fileName + ".png");
							IoUtils.runGraphviz(graphVizFile, pngFile);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			redStates.add(blueState);
			stateColoring.put(blueState, RED);

			for (final Integer redState : redStates) {
				final TIntList succsOfRed = pta.getSuccessors(redState);
				for (int i = 0; i < succsOfRed.size(); i++) {
					final int newBlueState = succsOfRed.get(i);
					if (stateColoring.get(newBlueState) != RED) {
						stateColoring.put(newBlueState, BLUE);
						if (!blueStates.contains(newBlueState)) {
							blueStates.add(newBlueState);
						}
					}
				}
			}


		}
		final PDFA result = pta.toPdfa();
		logger.info("Learned PDFA with ALERGIA-red-blue (in java).");
		return result;
	}

	boolean compatible(int qu, int qv, IntBinaryOperator mergeTest) {
		int i;
		if (mergeTest.applyAsInt(qu, qv) == 0) {
			return false;
		}
		if (!recursiveMergeTest) {
			return true;
		}
		for (i = 0; i < pta.getAlphabet().getAlphSize(); i++) {
			final String symbol = pta.getAlphabet().getSymbol(i);
			final Transition t1 = pta.getTransition(qu, symbol);
			final Transition t2 = pta.getTransition(qv, symbol);
			if (t1 != null && t2 != null) {
				final int t1Count = pta.getTransitionCount(t1.toZeroProbTransition());
				final int t2Count = pta.getTransitionCount(t2.toZeroProbTransition());
				if (t1Count > 0 && t2Count > 0) {
					final int t1Succ = t1.getToState();
					final int t2Succ = t2.getToState();
					if (!compatible(t1Succ, t2Succ, mergeTest)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * checks if two states are compatible
	 * 
	 * @param qu
	 * @param qv
	 * @return 1 if states are compatible, 0 if they are not.
	 */
	int alergiaCompatibilityTest(int qu, int qv) {
		int f1, n1, f2, n2;
		double gamma, bound;
		f1 = pta.getFinalStateCount(qu);
		n1 = totalFreq(pta.getTransitionCount(), qu);
		f2 = pta.getFinalStateCount(qv);
		n2 = totalFreq(pta.getTransitionCount(), qv);
		if (n1 < mergeT0 || n2 < mergeT0) {
			return 0;
		}
		gamma = Math.abs(((double) f1) / ((double) n1) - ((double) f2) / ((double) n2));
		bound = ((Math.sqrt(1.0 / n1) + Math.sqrt(1.0 / n2)) * Math.sqrt(Math.log(2.0 / alpha))) / 1.41421356237309504880;
		if (gamma > bound) {
			return 0;
		}

		for (final String a : pta.getAlphabet().getSymbols()) {
			f1 = symbolFreq(pta.getTransitionCount(), qu, a);
			n1 = totalFreq(pta.getTransitionCount(), qu);
			f2 = symbolFreq(pta.getTransitionCount(), qv, a);
			n2 = totalFreq(pta.getTransitionCount(), qv);
			gamma = Math.abs(((double) f1) / ((double) n1) - ((double) f2) / ((double) n2));
			bound = ((Math.sqrt(1.0 / n1) + Math.sqrt(1.0 / n2)) * Math.sqrt(Math.log(2.0 / alpha))) / 1.41421356237309504880;
			if (gamma > bound) {
				return 0;
			}
		}
		return 1;
	}

	/**
	 * frequency of transitions arriving at state qu
	 * 
	 * @param transitionCount
	 * @param qu
	 * @return
	 */
	private int totalFreq(TObjectIntMap<Transition> transitionCount, int qu) {
		int result = 0;
		final Set<Transition> incoming = pta.getAllTransitions().stream().filter(t -> (t.getToState() == qu)).collect(Collectors.toSet());
		for (final Transition t : incoming) {
			result += transitionCount.get(t);
		}
		return result;
	}

	private int symbolFreq(TObjectIntMap<Transition> transitionCount, int fromState, String event) {
		int result = 0;
		final Set<Transition> incoming = pta.getAllTransitions().stream().filter(t -> (t.getFromState() == fromState && t.getSymbol().equals(event)))
				.collect(Collectors.toSet());
		for (final Transition t : incoming) {
			result += transitionCount.get(t);
		}
		return result;
	}
}
