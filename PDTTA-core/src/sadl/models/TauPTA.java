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

package sadl.models;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Distribution;
import jsat.distributions.MyDistributionSearch;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.empirical.MyKernelDensityEstimator;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;
import sadl.detectors.PdttaDetector;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.structure.Transition;
import sadl.structure.UntimedSequence;
import sadl.structure.ZeroProbTransition;

/**
 * 
 * @author Timo Klerx
 *
 */
public class TauPTA extends PDTTA {
	private static final long serialVersionUID = -7222525536004714236L;
	transient private static Logger logger = LoggerFactory.getLogger(TauPTA.class);
	TObjectIntMap<Transition> transitionCount = new TObjectIntHashMap<>();
	TIntIntMap finalStateCount = new TIntIntHashMap();

	private AnomalyInsertionType anomalyType = AnomalyInsertionType.NONE;

	private static final int SEQUENTIAL_ANOMALY_K = 20;
	private static final double ANOMALY_3_CHANGE_RATE = 0.5;
	private static final double ANOMALY_4_CHANGE_RATE = 0.1;
	public static final double SEQUENCE_OMMIT_THRESHOLD = 0.0001;
	private static final double MAX_TYPE_FIVE_PROBABILITY = 0.2;

	int ommitedSequenceCount = 0;


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((anomalyType == null) ? 0 : anomalyType.hashCode());
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
		final TauPTA other = (TauPTA) obj;
		if (anomalyType != other.anomalyType) {
			return false;
		}
		return true;
	}

	public AnomalyInsertionType getAnomalyType() {
		return anomalyType;
	}

	private void setAnomalyType(AnomalyInsertionType anomalyType) {
		checkImmutable();
		this.anomalyType = anomalyType;
	}


	public TauPTA() {

	}

	/**
	 * WARNING: The input is changed (transformed to TimedIntWords)
	 * 
	 * @param trainingSequences
	 */
	@Deprecated
	public TauPTA(TimedInput trainingSequences) {
		super();
		trainingSequences = SerializationUtils.clone(trainingSequences);
		final TauPTA initialPta = new TauPTA();
		initialPta.addState(START_STATE);

		for (final TimedWord s : trainingSequences) {
			initialPta.addEventSequence(s);
		}

		// remove transitions and ending states with less than X occurences
		final double threshold = SEQUENCE_OMMIT_THRESHOLD * trainingSequences.size();
		for (final int state : initialPta.finalStateProbabilities.keys()) {
			final List<Transition> stateTransitions = initialPta.getTransitions(state, false);
			for (final Transition t : stateTransitions) {
				if (initialPta.transitionCount.get(t) < threshold) {
					initialPta.removeTimedTransition(t, false);
				}
			}
			if (initialPta.finalStateCount.get(state) < threshold) {
				initialPta.finalStateCount.put(state, 0);
			}
		}

		// compute event probabilities from counts
		for (final int state : initialPta.finalStateProbabilities.keys()) {
			final List<Transition> stateTransitions = initialPta.getTransitions(state, false);
			int occurenceCount = 0;
			for (final Transition t : stateTransitions) {
				occurenceCount += initialPta.transitionCount.get(t);
			}
			occurenceCount += initialPta.finalStateCount.get(state);
			for (final Transition t : stateTransitions) {
				initialPta.changeTransitionProbability(t, initialPta.transitionCount.get(t) / (double) occurenceCount, false);
			}
			initialPta.addFinalState(state, initialPta.finalStateCount.get(state) / (double) occurenceCount);
		}
		// now the whole stuff again but only with those sequences that are in the initialPta
		// do not remove any sequences because they should occur more often than the specified threshold
		addState(START_STATE);

		for (final TimedWord s : trainingSequences) {
			if (initialPta.isInAutomaton(s)) {
				addEventSequence(s);
			}
		}

		// compute event probabilities from counts
		for (final int state : finalStateProbabilities.keys()) {
			final List<Transition> stateTransitions = getTransitions(state, false);
			int occurenceCount = 0;
			for (final Transition t : stateTransitions) {
				occurenceCount += transitionCount.get(t);
			}
			occurenceCount += finalStateCount.get(state);
			for (final Transition t : stateTransitions) {
				changeTransitionProbability(t, transitionCount.get(t) / (double) occurenceCount, false);
			}
			addFinalState(state, finalStateCount.get(state) / (double) occurenceCount);
		}

		// compute time probabilities
		final Map<ZeroProbTransition, TDoubleList> timeValueBuckets = new HashMap<>();
		for (final TimedWord s : trainingSequences) {
			if (isInAutomaton(s)) {
				int currentState = START_STATE;
				for (int i = 0; i < s.length(); i++) {
					final String nextEvent = s.getSymbol(i);
					final Transition t = getTransition(currentState, nextEvent);
					if (t == null) {
						// this should never happen!
						throw new IllegalStateException("Did not get a transition, but checked before that there must be transitions for this sequence " + s);
					}
					addTimeValue(timeValueBuckets, t.getFromState(), t.getToState(), t.getSymbol(), s.getTimeValue(i));
					currentState = t.getToState();
				}
			} else {
				ommitedSequenceCount++;
			}
		}
		logger.info("OmmitedSequenceCount={} out of {} sequences at a threshold of less than {} absolute occurences.", ommitedSequenceCount,
				trainingSequences.size(), SEQUENCE_OMMIT_THRESHOLD * trainingSequences.size());
		final Map<ZeroProbTransition, ContinuousDistribution> distributions = fit(timeValueBuckets);
		setTransitionDistributions(distributions);
		if (distributions.size() != getTransitionCount()) {
			final List<Transition> missingDistributions = new ArrayList<>();
			for (final Transition t : transitions) {
				if (distributions.get(t.toZeroProbTransition()) == null) {
					missingDistributions.add(t);
				}
			}
			System.out.println(missingDistributions);
			throw new IllegalStateException("It is not possible to more/less distributions than transitions (" + distributions.size() + "/"
					+ getTransitionCount() + ").");
			// compute what is missing in the distribution set
		}
		setAlphabet(trainingSequences);
	}

	private void addTimeValue(Map<ZeroProbTransition, TDoubleList> result, int currentState, int followingState, String event, double timeValue) {
		final ZeroProbTransition t = new ZeroProbTransition(currentState, followingState, event);
		final TDoubleList list = result.get(t);
		if (list == null) {
			final TDoubleList tempList = new TDoubleArrayList();
			tempList.add(timeValue);
			result.put(t, tempList);
		} else {
			list.add(timeValue);
		}
	}

	private Map<ZeroProbTransition, ContinuousDistribution> fit(Map<ZeroProbTransition, TDoubleList> timeValueBuckets) {
		final Map<ZeroProbTransition, ContinuousDistribution> result = new HashMap<>();
		logger.debug("timevalueBuckets.size={}", timeValueBuckets.size());
		for (final ZeroProbTransition t : timeValueBuckets.keySet()) {
			result.put(t, fitDistribution(timeValueBuckets.get(t)));
		}
		return result;
	}

	@SuppressWarnings("boxing")
	private ContinuousDistribution fitDistribution(TDoubleList transitionTimes) {
		final Vec v = new DenseVector(transitionTimes.toArray());
		final jsat.utils.Pair<Boolean, Double> sameValues = MyDistributionSearch.checkForDifferentValues(v);
		if (sameValues.getFirstItem()) {
			final ContinuousDistribution d = new SingleValueDistribution(sameValues.getSecondItem());
			return d;
		} else {
			final MyKernelDensityEstimator kde = new MyKernelDensityEstimator(v);
			return kde;
		}
	}

	private void addEventSequence(TimedWord s) {
		int currentState = START_STATE;

		for (int i = 0; i < s.length(); i++) {
			final String nextEvent = s.getSymbol(i);
			Transition t = getTransition(currentState, nextEvent);
			if (t == null) {
				t = addTransition(currentState, getStateCount(), nextEvent, NO_TRANSITION_PROBABILITY);
				transitionCount.put(t, 0);
			}
			transitionCount.increment(t);
			currentState = t.getToState();
		}
		// add final state count
		finalStateCount.adjustOrPutValue(currentState, 1, 1);
	}

	// now change the pta to generate anomalies of type 1-4
	// type 1: Auf jeder Ebene des Baumes: Wähle einen zufälligen Zustand und ändere bei einer zufälligen Ausgangstransition das Symbol in ein zufälliges
	// anderes
	// Sysmbol des Alphabets für das keine andere Ausgangstranition gibt.
	// type 2: Unwahrscheinlichste Sequenzen aus PTA auswählen. nach Wahrscheinlichkeiten aller Sequenzen sortieren und die $k$ unwahrscheinlichsten Sequenzen
	// als Anomalien labeln (bzw. die Transitionen auf dem Weg der Sequenzen).
	// type 3: auf jeder Ebene des Baumes: heavily increase or decrease the outcome of one single PDF. 50%
	// type 4: Auf $k$ wahrscheinlichsten Sequenzen des PTA (damit Anomalien von Typ 2 und 4 sich nicht überlappen): slightly increase or decrease (also mixed!)
	// the outcome of ALL values. 10%
	// type 5: increase or create random stop transitions? Do not increase, because it is not detectable. Only add new stopping transitions

	// We only insert one type of anomaly into a TauPTA and generate anomalies of the chosen type. The testSet containing all types of anomalies is created by
	// merging the output of different sets that only contain one type of anomaly.

	// Event-Rauschen entfernen
	// now change the pta to generate anomalies of type 1-4
	// type 1: Auf jeder Ebene des Baumes: Wähle einen zufälligen Zustand und ändere bei einer zufälligen Ausgangstransition das Symbol in ein zufälliges
	// anderes
	// Sysmbol des Alphabets für das keine andere Ausgangstranition gibt.
	// type 2: Unwahrscheinlichste Sequenzen aus PTA auswählen. (Alle Sequenzen nach Wahrschienlichkeiten sortieren und die k unwahrscheinlichsten als
	// Anomalie labeln.)
	// type 3: auf jeder Ebene des Baumes: heavily increase or decrease the outcome of one single PDF. 50%
	// type 4: Auf wahrscheinlichen Sequenzen des PTA (damit Anomalien von Typ 2 und 4 sich nicht überlappen): slightly increase or decrease (also mixed!)
	// the outcome of ALL values. 10%

	public void makeAbnormal(AnomalyInsertionType newAnomalyType) {
		if (this.anomalyType != AnomalyInsertionType.NONE) {
			logger.error("A TauPTA can only have one type of anomaly. This one already has AnomalyInsertionType {}, which should be overridden with {}",
					this.anomalyType, anomalyType);
			return;
		}
		immutable = false;
		setAnomalyType(newAnomalyType);
		if (anomalyType == AnomalyInsertionType.TYPE_ONE) {
			logger.debug("TransitionCount before inserting {} anomalies={}", anomalyType, getTransitionCount());
			insertPerLevelAnomaly(this::computeTransitionCandicatesType13, this::changeTransitionEvent);
			logger.debug("TransitionCount after inserting {} anomalies={}", anomalyType, getTransitionCount());
		} else if (anomalyType == AnomalyInsertionType.TYPE_TWO) {
			insertSequentialAnomaly(this::insertAnomaly2);
		} else if (anomalyType == AnomalyInsertionType.TYPE_THREE) {
			insertPerLevelAnomaly(this::computeTransitionCandicatesType13, this::changeTimeProbability);
		} else if (anomalyType == AnomalyInsertionType.TYPE_FOUR) {
			insertSequentialAnomaly(this::insertAnomaly4);
		} else if (anomalyType == AnomalyInsertionType.TYPE_FIVE) {
			insertPerLevelAnomaly(this::computeTransitionCandicatesType5, this::addFinalStateProbability);
		} else {
			throw new IllegalArgumentException("the AnomalyInsertionType " + newAnomalyType + " is not supported.");
		}
		this.checkAndRestoreConsistency();
		immutable = true;
	}

	private void insertSequentialAnomaly(IntUnaryOperator f) {
		final Set<UntimedSequence> allSequences = getAllSequences();
		final TObjectDoubleMap<UntimedSequence> sequenceProbabilities = computeEventProbabilities(allSequences);
		// this function may be one lamba with the streaming interface
		// List<UntimedSequence> abnormalSequences = getAllSequences().stream().sort(one way or the other depending on the type of anomaly).take First $K$
		// elements.collect(as List)
		logger.debug("AllSequences.size()={}", allSequences.size());
		logger.debug("Transitions.size()={}", getTransitionCount());
		final Comparator<UntimedSequence> c = (s1, s2) -> {
			final int probCompare = Double.compare(sequenceProbabilities.get(s1), sequenceProbabilities.get(s2));
			if (probCompare != 0) {
				return f.applyAsInt(probCompare);
			} else {
				return f.applyAsInt(s1.toString().compareTo(s2.toString()));
			}
		};
		allSequences.stream().sorted(c).limit(SEQUENTIAL_ANOMALY_K).map(s -> labelWithAnomaly(s, getAnomalyType())).collect(Collectors.toList());
		logger.debug("Transitions.size()={}", transitions.size());
		// allSequences.sort((t1, t2) -> Double.compare(sequenceProbabilities.get(t1), sequenceProbabilities.get(t2)));
		// final List<UntimedSequence> abnormalSequences = function.apply(allSequences);
		// abnormalSequences.forEach(s -> labelWithAnomaly(s,getAnomalyType()));
	}

	private UntimedSequence labelWithAnomaly(UntimedSequence s, AnomalyInsertionType anomalyinsertionType) {
		logger.debug("Labeling sequence {} with anomaly of type {}", s, anomalyinsertionType.getTypeIndex());
		logger.debug("Prob of sequence is {}", computeProbability(s));
		// traverse the TauPTA and label every transition with the anomalyType
		int currentState = START_STATE;
		final List<String> events = s.getEvents();
		for (int i = 0; i < events.size(); i++) {
			final String event = events.get(i);
			final Transition t = getTransition(currentState, event);
			if (t == null) {
				logger.warn("Transition for state {} and event {} is null while processing sequence {}", currentState, event, s);
				logger.warn("Transitions.size={}", getTransitionCount());
				throw new NullPointerException();
			} else {
				changeAnomalyType(t, anomalyinsertionType);
				currentState = t.getToState();
			}
		}
		return s;
	}

	private void changeAnomalyType(Transition t, @SuppressWarnings("hiding") AnomalyInsertionType anomalyType) {
		if ((t.getAnomalyInsertionType() != anomalyType)) {
			final Transition newTransition = addAbnormalTransition(t, anomalyType);
			final ContinuousDistribution d = removeTimedTransition(t);
			bindTransitionDistribution(newTransition, d);
		}
	}

	private TObjectDoubleMap<UntimedSequence> computeEventProbabilities(Set<UntimedSequence> allSequences) {
		final TObjectDoubleMap<UntimedSequence> result = new TObjectDoubleHashMap<>();
		for (final UntimedSequence timedSequence : allSequences) {
			result.put(timedSequence, computeProbability(timedSequence));
		}
		return result;
	}

	private double computeProbability(final UntimedSequence untimedSequence) {
		final List<String> events = untimedSequence.getEvents();
		int currentState = getStartState();
		final TDoubleList probabilities = new TDoubleArrayList(events.size());
		for (int i = 0; i < events.size(); i++) {
			final String event = events.get(i);
			final Transition t = getTransition(currentState, event);
			final double probability = t.getProbability();
			probabilities.add(probability);
			currentState = t.getToState();
		}
		return PdttaDetector.aggregate(probabilities);
		// return product(probabilities);
	}



	private int insertAnomaly2(int i) {
		// take the least probable $k$ sequences, traverse the TauPTA with those sequences and set every transition on its way to anomalyType2
		return i;
	}

	private int insertAnomaly4(int i) {
		// take the most probable $k$ sequences, traverse the TauPTA with those sequences and set every transition on its way to anomalyType4
		return -i;
	}

	private void insertPerLevelAnomaly(IntFunction<List<Transition>> possibleTransitionFunction, ToIntFunction<List<Transition>> insertAnomaly) {
		for (int height = 0; height < getTreeHeight(); height++) {
			final TIntList states = getStates(height);
			final List<Transition> allLevelTransitions = new ArrayList<>();
			for (int i = 0; i < states.size(); i++) {
				allLevelTransitions.addAll(getTransitions(states.get(i), true));
			}
			int result = 0;
			final List<Transition> possibleTransitions = possibleTransitionFunction.apply(height);
			if (possibleTransitions.size() > 0) {
				// sort for determinism
				Collections.sort(possibleTransitions);
				result = insertAnomaly.applyAsInt(possibleTransitions);
			}
			if (possibleTransitions.size() == 0 || result != 1) {
				logger.warn("It is not possible to insert anomalies on height {}", height);
			}
		}
	}

	private int changeTimeProbability(List<Transition> possibleTransitions) {
		if (possibleTransitions.size() == 0) {
			logger.warn("Chose states on which are leaf states. Inserting a anomalies is not possible.");
			return -1;
		}
		final Transition chosenTransition = chooseRandomObject(possibleTransitions, r);
		logger.debug("Chose transition {} for inserting an anomaly of type 3", chosenTransition);
		final ContinuousDistribution d = removeTimedTransition(chosenTransition);
		final Transition newTransition = addAbnormalTransition(chosenTransition.getFromState(), chosenTransition.getToState(), chosenTransition.getSymbol(),
				chosenTransition.getProbability(), AnomalyInsertionType.TYPE_THREE);
		bindTransitionDistribution(newTransition.toZeroProbTransition(), d);
		return 1;
	}

	private int addFinalStateProbability(List<Transition> possibleTransitions) {
		if (possibleTransitions.size() == 0) {
			logger.warn("Chose states which do not have . Inserting a time anomaly is not possible.Transitions:{}", possibleTransitions);
			return -1;
		}
		// only add if there was no final state transition before
		// restore probability sum afterwards
		final Transition t = chooseRandomObject(possibleTransitions, r);
		// only do so if there is no stopping transition in the possibleTransitions
		final double probability = r.nextDouble() * MAX_TYPE_FIVE_PROBABILITY;
		addAbnormalFinalState(t.getFromState(), probability);
		// now fix probs that they sum up to one
		fixProbability(t.getFromState());
		return 1;
	}

	private List<Transition> computeTransitionCandicatesType5(int height) {
		final List<Transition> result = new ArrayList<>();
		final TIntList states = getStates(height);
		for (int i = 0; i < states.size(); i++) {
			final int state = states.get(i);
			final List<Transition> possibleTransitions = getTransitions(state, true);
			if (!possibleTransitions.stream().anyMatch(t -> t.isStopTraversingTransition() && t.getProbability() > 0)) {
				// just add one transition which contains the state
				result.add(possibleTransitions.get(0));
			} else {
				logger.debug("Filtered the state {} that already has a final state", state);
			}
		}
		if (result.size() == 0) {
			logger.warn("Chose states on height {} which all have final states. Inserting a stopping anomaly is not possible.", height);
		}
		return result;
	}

	private List<Transition> computeTransitionCandicatesType13(int height) {
		final List<Transition> result = new ArrayList<>();
		final TIntList states = getStates(height);
		for (int i = 0; i < states.size(); i++) {
			final int state = states.get(i);
			final List<Transition> possibleTransitions = getTransitions(state, false);
			result.addAll(possibleTransitions);
		}
		if (result.size() == 0) {
			logger.warn("Chose states on height {} which are leaf states. Inserting a anomalies is not possible.", height);
		}
		if (result.size() == 1) {
			return Collections.EMPTY_LIST;
		}
		return result;
	}

	private int changeTransitionEvent(List<Transition> possibleTransitions) {
		final TIntSet currentStates = new TIntHashSet(possibleTransitions.stream().mapToInt(t -> t.getFromState()).distinct().toArray());
		while (currentStates.size() > 0) {
			final Transition chosenTransition = chooseRandomObject(possibleTransitions, r);
			final int chosenState = chosenTransition.getFromState();
			final List<Transition> stateTransitions = possibleTransitions.stream().filter(t -> t.getFromState() == chosenState).collect(Collectors.toList());
			final List<String> notOccuringEvents = new ArrayList<>(Arrays.asList(alphabet.getSymbols()));
			for (final Transition t : stateTransitions) {
				notOccuringEvents.remove(t.getSymbol());
			}
			if (notOccuringEvents.size() == 0 || stateTransitions.size() == 0) {
				logger.warn("Not possible to change an event in state {}", chosenState);
				currentStates.remove(chosenState);
				continue;
			} else {
				final String chosenEvent = notOccuringEvents.get(r.nextInt(notOccuringEvents.size()));
				logger.debug("Chose event {} from {}", chosenEvent, notOccuringEvents);
				final ContinuousDistribution d = removeTimedTransition(chosenTransition);
				final Transition newTransition = addAbnormalTransition(chosenTransition.getFromState(), chosenTransition.getToState(), chosenEvent,
						chosenTransition.getProbability(), AnomalyInsertionType.TYPE_ONE);
				bindTransitionDistribution(newTransition.toZeroProbTransition(), d);
				logger.debug("possibleTransitions={}", possibleTransitions);
				logger.debug("Changed {} to {} for inserting an anomaly of type 1", chosenTransition, newTransition);
				return 1;
			}
		}
		return 0;
	}

	public static <T> T chooseRandomObject(List<T> list, Random rnd) {
		return list.get(rnd.nextInt(list.size()));
	}

	/**
	 * returns the maximum tree height
	 * 
	 */
	public int getTreeHeight() {
		return getTreeHeight(0, 0);
	}

	private int getTreeHeight(int currentState, int currentDepth) {
		final TIntList result = new TIntArrayList();
		final List<Transition> deeperTransitions = getTransitions(currentState, false);
		if (deeperTransitions.size() == 0) {
			return currentDepth;
		}
		for (final Transition t : deeperTransitions) {
			result.add(getTreeHeight(t.getToState(), currentDepth + 1));
		}
		return result.max();

	}

	/**
	 * returns all the states on the given tree height / tree level
	 * 
	 * @param treeHeight
	 */
	public TIntList getStates(int treeHeight) {
		return getStates(treeHeight, 0, 0);
	}

	private TIntList getStates(int treeHeight, int currentState, int currentDepth) {
		final TIntList result = new TIntArrayList();
		if (currentDepth < treeHeight) {
			final List<Transition> deeperTransitions = getTransitions(currentState, false);
			for (final Transition t : deeperTransitions) {
				result.addAll(getStates(treeHeight, t.getToState(), currentDepth + 1));
			}
		} else {
			result.add(currentState);
		}
		return result;
	}

	@Override
	public TimedWord sampleSequence() {
		if (getAnomalyType() == AnomalyInsertionType.NONE) {
			return super.sampleSequence();
		}
		// this TauPTA should sample anomalies of the one specified type
		int currentState = START_STATE;

		final List<String> eventList = new ArrayList<>();
		final TIntList timeList = new TIntArrayList();
		boolean choseFinalState = false;
		@SuppressWarnings("hiding")
		AnomalyInsertionType anomalyType = AnomalyInsertionType.NONE;
		while (!choseFinalState) {
			List<Transition> possibleTransitions = getTransitions(currentState, true);
			double random = r.nextDouble();
			double newProbSum = -1;
			if (getAnomalyType() == AnomalyInsertionType.TYPE_TWO || getAnomalyType() == AnomalyInsertionType.TYPE_FOUR) {
				// Filter out all transitions that do not belong to the sequential anomaly type and are no stopping transitions
				// The TauPTA should have a field containing its anomaly type. So if the TauPTA is of anomaly type 2, then only transitions with anomaly type 2
				// are allowed to be chosen.
				possibleTransitions = possibleTransitions.stream()
						.filter(t -> (t.getAnomalyInsertionType() == getAnomalyType() || t.isStopTraversingTransition())).collect(Collectors.toList());
				// after that normalize s.t. the remaining transition probs sum up to one (or make the random value smaller)
				newProbSum = possibleTransitions.stream().mapToDouble(t -> t.getProbability()).sum();
				if (!Precision.equals(newProbSum, 1)) {
					logger.debug("New ProbSum={}, so decreasing random value from {} to {}", newProbSum, random, random * newProbSum);
					random *= newProbSum;
				}
			}
			// the most probable transition (with the highest probability) should be at index 0
			// should be right in this way
			Collections.sort(possibleTransitions, (t1, t2) -> -Double.compare(t1.getProbability(), t2.getProbability()));
			if (possibleTransitions.size() <= 0) {
				logger.error("There are no transitions for state {} with newProbSum={} and randomValue={}. This is not possible.", currentState, newProbSum,
						random);
			}
			double summedProbs = 0;
			int index = -1;
			for (int i = 0; i < possibleTransitions.size(); i++) {
				summedProbs += possibleTransitions.get(i).getProbability();
				if (random < summedProbs) {
					index = i;
					break;
				}
			}
			if (index == -1) {
				logger.error("Found no possible transition from {}", possibleTransitions);
			}
			final Transition chosenTransition = possibleTransitions.get(index);
			if (chosenTransition.isAbnormal()) {
				if (getAnomalyType() != chosenTransition.getAnomalyInsertionType()) {
					// This is a conflict because the anomalyType was already set to anomaly. This should never happen!
					throw new IllegalStateException("Two anomalies are mixed in this special case. This should never happen.");
				}
				anomalyType = chosenTransition.getAnomalyInsertionType();
			}
			if (chosenTransition.isStopTraversingTransition()) {
				choseFinalState = true;
				// what happens if an abnormal stopping transiton (type 5) was chosen?
				// Nothing should happen because we label the sequence as type 5 anomaly
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
				if (anomalyType == AnomalyInsertionType.TYPE_THREE) {
					timeValue = changeTimeValue(timeValue, ANOMALY_3_CHANGE_RATE);
				} else if (anomalyType == AnomalyInsertionType.TYPE_FOUR) {
					timeValue = changeTimeValue(timeValue, ANOMALY_4_CHANGE_RATE);
				}
				eventList.add(chosenTransition.getSymbol());
				timeList.add(timeValue);
			}
		}
		if (anomalyType != AnomalyInsertionType.NONE) {
			return new TimedWord(eventList, timeList, ClassLabel.ANOMALY);
		} else {
			return new TimedWord(eventList, timeList, ClassLabel.NORMAL);
		}
	}

	private int changeTimeValue(int value, double factor) {
		if (r.nextBoolean()) {
			return (int) (value * -factor);
		} else {
			return (int) (value * factor);
		}
	}

	public Set<Transition> getAllTransitions() {
		return transitions;
	}

}
