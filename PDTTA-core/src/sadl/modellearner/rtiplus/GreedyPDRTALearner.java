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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;

import sadl.input.TimedInput;
import sadl.interfaces.Model;
import sadl.models.pdrta.PDRTA;
import sadl.models.pdrta.PDRTAInput;
import sadl.models.pdrta.PDRTAState;

import com.google.common.collect.TreeMultimap;

/**
 * 
 * @author Fabian Witter
 *
 */
public class GreedyPDRTALearner extends SimplePDRTALearner {

	private final int maxMergesToSearch = 10;
	private final int maxSplitsToSearch = 10;

	public GreedyPDRTALearner(float sig, String histBins, int testerType, int distrCheckType, RunMode runMode, String dir) {
		super(sig, histBins, testerType, distrCheckType, runMode, dir);
	}

	@Override
	public Model train(TimedInput trainingSequences) {

		System.out.println("RTI+: Building automaton from input sequences");

		final boolean expand = distrCheckType >= 1;
		final PDRTAInput in = new PDRTAInput(trainingSequences, histBinsStr, expand);
		final PDRTA a = new PDRTA(in);

		System.out.println("Parameters are: significance=" + significance + " distrCheckType=" + distrCheckType);
		System.out.println("Histogram Bins are: " + a.getHistBinsString());
		if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
			System.out.println("Log Level: " + runMode);
		}

		System.out.println("*** Performing greedy RTI+ ***");
		startTime = System.currentTimeMillis();
		final Set<PDRTAState> redStates = new HashSet<>();
		final Set<PDRTAState> blueStates = new HashSet<>();
		tester.setStateSets(redStates, blueStates);
		greedyRTIplus(a, redStates, blueStates);
		in.clear();
		persistFinalResult(a);

		System.out.println("Time: " + getDuration(startTime, System.currentTimeMillis()));
		System.out.println("END");

		return a;
	}

	@SuppressWarnings("null")
	private void greedyRTIplus(PDRTA a, Collection<PDRTAState> redStates, Collection<PDRTAState> blueStates) {

		int counter = 0;
		Transition t = getMostVisitedTrans(a, redStates, blueStates);
		while (t != null) {
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
				System.out.print("Testing splits...");
			}
			counter++;

			final NavigableSet<Refinement> splits = getSplitRefs(t, redStates, blueStates).descendingSet();
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.println("\nFound " + splits.size() + " possible splits.");
				System.out.print("Testing merges...");
			}
			final NavigableSet<Refinement> merges = getMergeRefs(t, redStates, blueStates).descendingSet();
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.println("\nFound " + merges.size() + " possible merges.");
				System.out.print("Calculating sizes for splits...");
			}

			final TreeMultimap<Double, Refinement> all = TreeMultimap.create();
			ProgressBarPrinter pbp = null;
			if (splits.size() > maxSplitsToSearch && runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				pbp = new ProgressBarPrinter(maxSplitsToSearch);
			} else if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				pbp = new ProgressBarPrinter(splits.size());
			}
			int c = 0;
			for (final Refinement r : splits) {
				if (c >= maxSplitsToSearch) {
					break;
				}
				final Collection<PDRTAState> redsC = new HashSet<>(redStates);
				final Collection<PDRTAState> bluesC = new HashSet<>(blueStates);
				final PDRTA copy = new PDRTA(a);
				final Refinement cR = new Refinement(copy, r, redsC, bluesC);
				cR.refine();
				complete(copy, redsC, bluesC);
				// TODO Use AIC
				final int size = copy.getSize();
				all.put((double) size, r);
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					pbp.inc();
				}
				c++;
			}
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.println();
				System.out.print("Calculating sizes for merges...");
			}
			if (merges.size() > maxMergesToSearch && runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				pbp = new ProgressBarPrinter(maxMergesToSearch);
			} else if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				pbp = new ProgressBarPrinter(merges.size());
			}
			c = 0;
			for (final Refinement r : merges) {
				if (c >= maxMergesToSearch) {
					break;
				}
				final Collection<PDRTAState> redsC = new HashSet<>(redStates);
				final Collection<PDRTAState> bluesC = new HashSet<>(blueStates);
				final PDRTA copy = new PDRTA(a);
				final Refinement cR = new Refinement(copy, r, redsC, bluesC);
				cR.refine();
				complete(copy, redsC, bluesC);
				// TODO Use AIC
				final int size = copy.getSize();
				all.put((double) size, r);
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					pbp.inc();
				}
				c++;
			}
			if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
				System.out.println();
			}
			assert (all.size() <= (maxMergesToSearch + maxSplitsToSearch));
			if (!all.isEmpty()) {
				final double minSize = all.keySet().first();
				final Refinement r = all.get(minSize).last();
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.println("DO: " + r.toString() + " Size: " + minSize);
				}
				r.refine();
			} else {
				if (runMode.compareTo(RunMode.NORMAL_CONSOLE) >= 0) {
					System.out.println("DO: Color state " + a.getIndex(t.target) + " red");
				}
				setRed(t.target, redStates, blueStates);
			}
			if (runMode.compareTo(RunMode.DEBUG) >= 0) {
				if (!a.isConsistent()) {
					throw new IllegalStateException("Automaton not consistent!");
				}
			}
			t = getMostVisitedTrans(a, redStates, blueStates);
		}

		if (!a.isConsistent()) {
			throw new IllegalStateException("Automaton not consistent!");
		}
		assert (a.getNumStates() == redStates.size());
		if (runMode.compareTo(RunMode.DEBUG_STEPS) >= 0) {
			try {
				draw(a, true, directory + "steps/step_" + counter + ".png");
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		persistFinalResult(a);
	}

	// TODO Implement AIC
	// public double calcAIC(boolean def) {
	//
	// final double result = 0.0;
	// final double defaultLog = Math.log(1.0 / ((double) (input.getNumHistogramBars() + input.getAlphSize())));
	//
	// final LikelihoodValue lv = getLikelihood();
	//
	// // TODO int params = ((input.getNumHistogramBars() - 1) *
	// // TA->num_states()) + getSize();
	//
	// return 2.0 * ((double) lv.additionalParam - lv.ratio);
	// }
	//
	// private LikelihoodValue getLikelihood() {
	//
	// final LikelihoodValue lv = new LikelihoodValue(0.0, 0);
	// for (final TimedState s : states.values()) {
	// lv.add(s.calculateLikelihoodPropSym());
	// lv.add(s.calculateLikelihoodPropTime());
	// }
	// return lv;
	// }

}
