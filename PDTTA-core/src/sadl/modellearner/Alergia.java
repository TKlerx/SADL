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

package sadl.modellearner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import gnu.trove.map.TObjectIntMap;
import sadl.constants.PTAOrdering;
import sadl.input.TimedInput;
import sadl.models.FTA;
import sadl.models.PDFA;
import sadl.structure.Transition;
import sadl.utils.IoUtils;
import sadl.utils.Settings;

/**
 * 
 * @author Timo Klerx
 *
 */
public class Alergia implements PdfaLearner {

	private final double alpha;
	FTA pta;
	PTAOrdering ordering = PTAOrdering.TopDown;
	int mergeT0 = 0;
	boolean recursiveMergeTest = true;
	private final Logger logger = org.slf4j.LoggerFactory.getLogger(Alergia.class);


	public Alergia(double alpha) {
		this.alpha = alpha;
	}

	int debugStepCounter = 0;

	@Override
	public PDFA train(TimedInput trainingSequences) {
		logger.info("Starting to learn PDFA with ALERGIA (java)...");
		int iterationCounter = 0;
		final IntBinaryOperator mergeTest = this::alergiaCompatibilityTest;
		pta = new FTA(trainingSequences);
		if (ordering == PTAOrdering.TopDown) {
			for (int j = PDFA.START_STATE + 1; j < pta.getStateCount(); j++) {
				logger.debug("starting Alergia iteration {}...", iterationCounter);
				if (!pta.containsState(j)) {
					iterationCounter++;
					continue;
				}
				iloop: for (int i = PDFA.START_STATE; i < j; i++) {
					if (!pta.containsState(i)) {
						continue;
					}
					if (compatible(i, j, mergeTest)) {
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
						pta.merge(i, j);
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
						break iloop;
					}
				}
				logger.debug("Ended Alergia iteration {}.", iterationCounter);
				iterationCounter++;
			}
		}
		final PDFA result = pta.toPdfa();
		logger.info("Learned PDFA with ALERGIA (in java).");
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
