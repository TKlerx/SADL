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
package sadl.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Distribution;
import sadl.constants.ClassLabel;
import sadl.input.TimedWord;
import sadl.interfaces.TauEstimator;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;
import sadl.tau_estimation.IdentityEstimator;
import sadl.utils.Settings;

/**
 * A Probabilistic Deterministic Timed-Transition Automaton (PDTTA).
 * 
 * @author Timo Klerx
 *
 */
public class PDTTA extends PDFA {

	private static final long serialVersionUID = -5394139607433634347L;

	transient private static Logger logger = LoggerFactory.getLogger(PDTTA.class);


	private TauEstimator tauEstimator;
	Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = null;

	protected PDTTA() {
	}

	protected PDTTA(TauEstimator tauEstimator) {
		this.tauEstimator = tauEstimator;
	}

	public PDTTA(PDFA pdfa, Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions, TauEstimator tauEstimation) {
		super(pdfa);
		this.transitionDistributions = transitionDistributions;
		this.tauEstimator = tauEstimation;
		if (tauEstimator == null) {
			tauEstimator = new IdentityEstimator();
		}
		checkAndRestoreConsistency();
	}


	protected void bindTransitionDistribution(Transition newTransition, ContinuousDistribution d) {
		checkImmutable();
		if (transitionDistributions != null) {
			transitionDistributions.put(newTransition.toZeroProbTransition(), d);
		} else {
			logger.warn("Trying to add Distribution {} to non existing time transition distributions", d);
		}
	}

	@Override
	public Pair<TDoubleList, TDoubleList> calculateProbabilities(TimedWord s) {
		return Pair.create(computeEventLikelihoods(s), computeTimeLikelihoods(s));
	}

	@Override
	protected void changeTransitionProbability(Transition transition, double newProbability) {
		changeTransitionProbability(transition, newProbability, true);
	}

	/**
	 * 
	 * @param transition
	 * @param newProbability
	 * @param bindTimeInformation
	 *            also bind the time distribution to the internally created new transition. Only set this to false if there is no time information present in
	 *            this automaton
	 */
	public void changeTransitionProbability(Transition transition, double newProbability, boolean bindTimeInformation) {
		checkImmutable();
		if (transition.isStopTraversingTransition()) {
			super.changeTransitionProbability(transition, newProbability);
		} else {
			ContinuousDistribution d = null;
			d = removeTimedTransition(transition, bindTimeInformation);
			final Transition t = new Transition(transition.getFromState(), transition.getToState(), transition.getSymbol(), newProbability);
			transitions.add(t);
			if (bindTimeInformation) {
				bindTransitionDistribution(t, d);
			}
			if (d == null && bindTimeInformation) {
				logger.warn("Should incorporate time but there was no time distribution associated with transition {}", t);
			}
		}
	}

	protected TDoubleList computeTimeLikelihoods(TimedWord ts) {
		final TDoubleList list = new TDoubleArrayList(ts.length());
		int currentState = START_STATE;
		//sequential
		// for (int i = 0; i < ts.length(); i++) {
		// final Transition t = getTransition(currentState, ts.getSymbol(i));
		// // DONE this is crap, isnt it? why not return an empty list or null iff there is no transition for the given sequence? or at least put a '0' in the
		// // last slot.
		// if (t == null) {
		// list.add(0);
		// return list;
		// }
		// final ContinuousDistribution d = getTransitionDistributions().get(t.toZeroProbTransition());
		// if (d == null) {
		// logger.warn("Found no time distribution for Transition " + t);
		// list.add(0);
		// } else {
		// list.add(tauEstimator.estimateTau(d, ts.getTimeValue(i)));
		// }
		// currentState = t.getToState();
		// }
		// parallel (does not change determinism)
		final List<Transition> traversedTransitions = new ArrayList<>();
		for (int i = 0; i < ts.length(); i++) {
			final Transition t = getTransition(currentState, ts.getSymbol(i));
			if (t == null) {
				break;
			}
			traversedTransitions.add(t);
			currentState = t.getToState();
		}
		list.fill(0, traversedTransitions.size(), 0);
		final IntConsumer f = i -> {
			final Transition t = traversedTransitions.get(i);
			if (t == null) {
				list.set(i, 0);
				return;
			}
			final ContinuousDistribution d = getTransitionDistributions().get(t.toZeroProbTransition());
			if (d == null) {
				logger.warn("Found no time distribution for Transition " + t);
				list.set(i, 0);
			} else {
				final double timeLikelihood = tauEstimator.estimateTau(d, ts.getTimeValue(i));
				if (timeLikelihood < 0) {
					throw new IllegalStateException("Time likelihood must not be negative");
				}
				list.set(i, timeLikelihood);
			}
		};
		if (Settings.isParallel()) {
			IntStream.range(0, traversedTransitions.size()).parallel().forEach(f);
		} else {
			IntStream.range(0, traversedTransitions.size()).forEach(f);
		}
		return list;
	}

	/**
	 * 
	 * @return true if irrelevant transitions were removed.
	 */
	private boolean deleteIrrelevantTransitions() {
		logger.debug("There are {} many transitions before removing irrelevant ones", getTransitionCount());
		// there may be more transitions than transitionDistributions
		final boolean removedTransitions = transitions.removeIf(t -> !transitionDistributions.containsKey(t.toZeroProbTransition()));
		if (removedTransitions) {
			logger.info("Removed some unnecessary transitions");
		}
		if (getTransitionCount() != transitionDistributions.size()) {
			logger.error("This should never happen because trainsitions.size() and transitionDistributions.size() should be equal now, but are not! {}!={}",
					getTransitionCount(), transitionDistributions.size());
		}
		logger.debug("There are {} many transitions after removing irrelevant ones", getTransitionCount());
		return removedTransitions;
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
			final Set<Entry<ZeroProbTransition, ContinuousDistribution>> e1 = transitionDistributions.entrySet();
			final Set<Entry<ZeroProbTransition, ContinuousDistribution>> e2 = other.transitionDistributions.entrySet();
			int count = 0;
			for (final Entry<ZeroProbTransition, ContinuousDistribution> e : e1) {
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
			for (final Entry<ZeroProbTransition, ContinuousDistribution> e : e2) {
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
		if (tauEstimator == null) {
			if (other.tauEstimator != null) {
				return false;
			}
		} else if (!tauEstimator.equals(other.tauEstimator)) {
			return false;
		}
		return true;
	}

	public Map<ZeroProbTransition, ContinuousDistribution> getTransitionDistributions() {
		return transitionDistributions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((transitionDistributions == null) ? 0 : transitionDistributions.hashCode());
		result = prime * result + ((tauEstimator == null) ? 0 : tauEstimator.hashCode());
		return result;
	}

	@Override
	protected boolean isConsistent() {
		if (getTransitionCount() != transitionDistributions.size()) {
			logger.warn("transitions and transitionDistributions must be of same size! {}!={}", getTransitionCount(), transitionDistributions.size());
			return false;
		}
		return super.isConsistent();
	}

	/**
	 * CARE: The distribution to this transition is also removed.
	 * 
	 * @param t
	 */
	protected ContinuousDistribution removeTimedTransition(Transition t) {
		return removeTimedTransition(t, true);
	}

	public ContinuousDistribution removeTimedTransition(Transition t, boolean removeTimeDistribution) {
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
	public boolean removeTransition(Transition t) {
		return removeTimedTransition(t) != null;
	}

	@Override
	public boolean restoreConsistency() {
		return deleteIrrelevantTransitions() | super.restoreConsistency();
	}

	@Override
	public TimedWord sampleSequence() {
		int currentState = START_STATE;
		final List<String> eventList = new ArrayList<>();
		final TIntList timeList = new TIntArrayList();
		boolean choseFinalState = false;
		while (!choseFinalState) {
			final Transition chosenTransition = chooseNextTransition(currentState);
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
				int timeValue = (int) d.sample(1, r)[0];
				eventList.add(chosenTransition.getSymbol());
				if (timeValue < 0) {
					timeValue = 0;
				}
				timeList.add(timeValue);
			}
		}
		// TODO add the capability to create abnormal sequences with a PDTTA
		return new TimedWord(eventList, timeList, ClassLabel.NORMAL);
	}

	public void setTransitionDistributions(Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions) {
		checkImmutable();
		this.transitionDistributions = transitionDistributions;
		checkAndRestoreConsistency();
	}

	public void preprocess() {
		tauEstimator.preprocess(transitionDistributions.values());

	}
}
