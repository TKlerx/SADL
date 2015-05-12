/**
 * This file is part of SADL, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.models;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jsat.distributions.Distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;
import sadl.input.TimedIntWord;
import sadl.input.TimedWord;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;

/**
 * A Probabilistic Deterministic Timed-Transition Automaton (PDTTA).
 * 
 * @author Timo Klerx
 *
 */
public class PDTTA extends PDFA {

	private static final long serialVersionUID = 3017416753740710943L;

	transient private static Logger logger = LoggerFactory.getLogger(PDTTA.class);


	Map<ZeroProbTransition, Distribution> transitionDistributions = null;

	public Map<ZeroProbTransition, Distribution> getTransitionDistributions() {
		return transitionDistributions;
	}

	public void setTransitionDistributions(Map<ZeroProbTransition, Distribution> transitionDistributions) {
		this.transitionDistributions = transitionDistributions;
		checkAndRestoreConsistency();
	}

	@Override
	protected boolean restoreConsistency() {
		return super.restoreConsistency() | deleteIrrelevantTransitions();
	}

	@Override
	protected boolean isConsistent() {
		if (transitions.size() != transitionDistributions.size()) {
			logger.warn("transitions and transitionDistributions must be of same size! {}!={}", transitions.size(), transitionDistributions.size());
			return false;
		}
		return super.isConsistent();
	}

	/**
	 * 
	 * @return true if irrelevant transitions were removed.
	 */
	private boolean deleteIrrelevantTransitions() {
		logger.debug("There are {} many transitions before removing irrelevant ones", transitions.size());
		// there may be more transitions than transitionDistributions
		final boolean removedTransitions = transitions.removeIf(t -> !transitionDistributions.containsKey(t.toZeroProbTransition()));
		if (removedTransitions) {
			logger.info("Removed some unnecessary transitions");
		}
		if (transitions.size() != transitionDistributions.size()) {
			logger.error("This should never happen because trainsitions.size() and transitionDistributions.size() should be equal now, but are not! {}!={}",
					transitions.size(), transitionDistributions.size());
		}
		logger.debug("There are {} many transitions after removing irrelevant ones", transitions.size());
		return removedTransitions;
	}



	@Override
	protected void changeTransitionProbability(Transition transition, double newProbability) {
		changeTransitionProbability(transition, newProbability, true);
	}

	protected void changeTransitionProbability(Transition transition, double newProbability, boolean bindTimeInformation) {
		// TODO use super.changeTransitionProbability
		if (!transition.isStopTraversingTransition()) {
			Distribution d = null;
			d = removeTimedTransition(transition, bindTimeInformation);
			final Transition t = new Transition(transition.getFromState(), transition.getToState(), transition.getSymbol(), newProbability);
			transitions.add(t);
			if (bindTimeInformation) {
				bindTransitionDistribution(t, d);
			}
			if (d == null && bindTimeInformation) {
				logger.warn("Should incorporate time but there was no time distribution associated with transition {}", t);
			}
		} else {
			final double adjusted = finalStateProbabilities.put(transition.getFromState(), newProbability);
			if (Double.doubleToLongBits(adjusted) == Double.doubleToLongBits(finalStateProbabilities.getNoEntryValue())) {
				logger.warn("Was not possible to adjust final state prob for transition {}",transition);
			}
		}
	}

	protected PDTTA() {
	}

	public PDTTA(Path trebaPath) throws IOException {
		super(trebaPath);
	}


	protected void bindTransitionDistribution(Transition newTransition, Distribution d) {
		if (transitionDistributions != null) {
			transitionDistributions.put(newTransition.toZeroProbTransition(), d);
		} else {
			logger.warn("Trying to add Distribution {} to non existing time transition distributions", d);
		}
	}

	/**
	 * CARE: The distribution to this transition is also removed.
	 * 
	 * @param t
	 */
	protected Distribution removeTimedTransition(Transition t) {
		return removeTimedTransition(t, true);
	}

	@Override
	protected boolean removeTransition(Transition t) {
		return removeTimedTransition(t) != null;
	}

	protected Distribution removeTimedTransition(Transition t, boolean removeTimeDistribution) {
		super.removeTransition(t);
		if (removeTimeDistribution) {
			if (transitionDistributions != null) {
				return transitionDistributions.remove(t.toZeroProbTransition());
			} else {
				logger.warn("Trying to remove from non existing transition distributions and transition {}", t);
				return null;
			}
		} else {
			return null;
		}
	}


	@Override
	public TimedWord sampleSequence() {
		// TODO use the method of PDFA and/or externalize the first part! then go on here with adding the time information
		int currentState = START_STATE;

		final TIntList eventList = new TIntArrayList();
		final TIntList timeList = new TIntArrayList();
		boolean choseFinalState = false;
		while (!choseFinalState) {
			final List<Transition> possibleTransitions = getTransitions(currentState, true);
			Collections.sort(possibleTransitions, (t1, t2) -> -Double.compare(t2.getProbability(), t1.getProbability()));
			final double random = r.nextDouble();
			double summedProbs = 0;
			int index = -1;
			for (int i = 0; i < possibleTransitions.size(); i++) {
				summedProbs += possibleTransitions.get(i).getProbability();
				if (random < summedProbs) {
					index = i;
					break;
				}
			}

			final Transition chosenTransition = possibleTransitions.get(index);
			if (chosenTransition.isStopTraversingTransition()) {
				choseFinalState = true;
			} else if (eventList.size() > MAX_SEQUENCE_LENGTH) {
				throw new IllegalStateException("A sequence longer than " + MAX_SEQUENCE_LENGTH + " events should have been generated");
			} else {
				currentState = chosenTransition.getToState();
				final Distribution d = transitionDistributions.get(chosenTransition.toZeroProbTransition());
				if (d == null) {
					// maybe this happens because the automaton is more general than the data. So not every possible path in the automaton is represented in
					// the training data.
					throw new IllegalStateException("This should never happen for transition " + chosenTransition);
				}
				final int timeValue = (int) d.sample(1, r)[0];
				eventList.add(chosenTransition.getSymbol());
				timeList.add(timeValue);
			}
		}
		return new TimedIntWord(eventList, timeList, ClassLabel.NORMAL);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((transitionDistributions == null) ? 0 : transitionDistributions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PDTTA other = (PDTTA) obj;
		if (transitionDistributions == null) {
			if (other.transitionDistributions != null) {
				return false;
			}
		} else if (!transitionDistributions.equals(other.transitionDistributions)) {
			final Set<Entry<ZeroProbTransition, Distribution>> e1 = transitionDistributions.entrySet();
			final Set<Entry<ZeroProbTransition, Distribution>> e2 = other.transitionDistributions.entrySet();
			int count = 0;
			for (final Entry<ZeroProbTransition, Distribution> e : e1) {
				if (!e2.contains(e)) {
					logger.error("Entry {} not contained in e2", e);
					final Distribution result = other.transitionDistributions.get(e.getKey());
					if (result != null) {
						final boolean compare = e.getValue().equals(result);
						logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
						logger.info("d1: {}, d2: {}", e.getValue(), result);
					}
					count++;
				}
			}
			logger.error("");
			for (final Entry<ZeroProbTransition, Distribution> e : e2) {
				if (!e1.contains(e)) {
					logger.error("Entry {} not contained in e1", e);
					final Distribution result = transitionDistributions.get(e.getKey());
					if (result != null) {
						final boolean compare = e.getValue().equals(result);
						logger.info("Both maps contain a distribution for key {}; distributions are equal: {}", e.getKey(), compare);
						logger.info("d1: {}, d2: {}", e.getValue(), result);
					}
					count++;
				}
			}
			if (count > 0) {
				logger.error("{} out of {} entries did not match", count, transitionDistributions.size());
			}
			return false;
		}
		return true;
	}

}
