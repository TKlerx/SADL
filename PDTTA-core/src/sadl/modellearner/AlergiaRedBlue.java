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
package sadl.modellearner;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import sadl.constants.MergeMethod;
import sadl.input.TimedInput;
import sadl.models.FTA;
import sadl.models.PDFA;
import sadl.structure.Transition;
import sadl.utils.Settings;

/**
 * 
 * @author Timo
 *
 */
public class AlergiaRedBlue extends Alergia {
	private final Logger logger = LoggerFactory.getLogger(AlergiaRedBlue.class);

	public AlergiaRedBlue(double alpha) {
		super(alpha);
	}

	public AlergiaRedBlue(double alpha, boolean recursiveMergeTest) {
		super(alpha, recursiveMergeTest);
	}

	public AlergiaRedBlue(double alpha, boolean recursiveMergeTest, MergeMethod mergeMethod) {
		super(alpha, recursiveMergeTest, mergeMethod);
	}

	public AlergiaRedBlue(double alpha, boolean recursiveMergeTest, MergeMethod mergeMethod, int mergeT0) {
		super(alpha, recursiveMergeTest, mergeMethod, mergeT0);
	}

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
		redStates.add(new Integer(PDFA.START_STATE));

		final List<Transition> startStateSuccs = pta.getTransitionsToSucc(PDFA.START_STATE);
		for (int i = 0; i < startStateSuccs.size(); i++) {
			final int blueState = startStateSuccs.get(i).getToState();
			stateColoring.put(blueState, BLUE);
			blueStates.add(new Integer(blueState));
		}

		while (!blueStates.isEmpty()) {
			final Integer blueStateInt = blueStates.poll();
			final int blueState = blueStateInt.intValue();
			if (!pta.containsState(blueState) || stateColoring.get(blueState) == RED) {
				continue;
			}
			final Iterator<Integer> iterator = redStates.iterator();
			inner: while (iterator.hasNext()) {
				// inner: for (final Integer redState : redStates) {
				final int redState = iterator.next().intValue();
				if (!pta.containsState(redState)) {
					iterator.remove();
					// redStates.remove(redState);
					continue inner;
				}
				if (compatible(redState, blueState, mergeTest, new TIntHashSet())) {
					final int stepValue = debugStepCounter;
					debugStepCounter++;
					if (Settings.isDebug()) {
						printPta(stepValue, 0);
					}
					pta.merge(redState, blueState);
					if (Settings.isDebug()) {
						printPta(stepValue, 1);
					}
					pta.determinize();
					if (Settings.isDebug()) {
						printPta(stepValue, 2);
					}
				}
			}
			redStates.add(blueStateInt);
			stateColoring.put(blueState, RED);

			for (final Integer redState : redStates) {
				final List<Transition> succsOfRed = pta.getTransitionsToSucc(redState.intValue());
				for (int i = 0; i < succsOfRed.size(); i++) {
					final int newBlueState = succsOfRed.get(i).getToState();
					if (stateColoring.get(newBlueState) != RED && pta.getTransitionCount(succsOfRed.get(i)) > getMergeT0()) {
						stateColoring.put(newBlueState, BLUE);
						final Integer newBlueStateInt = new Integer(newBlueState);
						if (!blueStates.contains(newBlueStateInt)) {
							blueStates.add(newBlueStateInt);
						}
					}
				}
			}


		}
		final PDFA result = pta.toPdfa();
		logger.info("Learned PDFA with ALERGIA-red-blue ({} states).", result.getStateCount());
		return result;
	}

}
