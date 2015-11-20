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

package sadl.detectors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.ProbabilisticModel;
import sadl.models.PDTTA;
import sadl.utils.Settings;

/**
 * 
 * @author Timo Klerx
 *
 */
public abstract class AnomalyDetector {
	private static Logger logger = LoggerFactory.getLogger(AnomalyDetector.class);

	protected ProbabilityAggregationMethod aggType;
	ProbabilisticModel model;

	public boolean isAnomaly(ProbabilisticModel newModel, TimedWord s) {
		setModel(newModel);
		return isAnomaly(s);
	}

	public boolean[] areAnomalies(ProbabilisticModel newModel, TimedInput testSequences) {
		setModel(newModel);
		return areAnomalies(testSequences);

	}

	public AnomalyDetector(ProbabilityAggregationMethod aggType) {
		super();
		this.aggType = aggType;
	}

	public AnomalyDetector(ProbabilityAggregationMethod aggType, PDTTA model) {
		super();
		this.aggType = aggType;
		this.model = model;
	}

	/**
	 * returns two double values for every timed sequence. The first value is the event likelihood, the second the time likelihood
	 * 
	 * @param testTimedSequences
	 */
	public List<double[]> computeAggregatedLikelihoods(TimedInput testTimedSequences) {
		final List<double[]> result = new ArrayList<>();
		for (final TimedWord ts : testTimedSequences) {
			final Pair<TDoubleList, TDoubleList> p = model.calculateProbabilities(ts);
			final double eventProb = aggregate(p.getKey(), aggType);
			final double timeProb = aggregate(p.getValue(), aggType);
			result.add(new double[] { eventProb, timeProb });
		}
		return result;
	}

	public Pair<TDoubleList, TDoubleList> computeAggregatedTrendLikelihood(TimedWord ts) {
		final Pair<TDoubleList, TDoubleList> p = model.calculateProbabilities(ts);
		return computeAggregatedTrendLikelihood(p.getKey(), p.getValue());
	}

	public Pair<TDoubleList, TDoubleList> computeAggregatedTrendLikelihood(TDoubleList eventLHs, TDoubleList timeLHs) {
		final TDoubleList partialEventLHs = new TDoubleArrayList();
		final TDoubleList partialTimeLHs = new TDoubleArrayList();
		for (int i = 1; i <= eventLHs.size(); i++) {
			final TDoubleList subList = eventLHs.subList(0, i);
			partialEventLHs.add(aggregate(subList, aggType));
		}
		for (int i = 1; i <= timeLHs.size(); i++) {
			final TDoubleList subList = timeLHs.subList(0, i);
			partialTimeLHs.add(aggregate(subList, aggType));
		}
		return Pair.create(partialEventLHs, partialTimeLHs);
	}

	public boolean isAnomaly(TimedWord s) {
		final Pair<TDoubleList, TDoubleList> p = model.calculateProbabilities(s);
		final TDoubleList eventLikelihoods = p.getKey();
		final TDoubleList timeLikelihoods = p.getValue();
		if (eventLikelihoods.size() < timeLikelihoods.size()) {
			throw new IllegalStateException("There must be at least as many event likelihoods as time likelihoods, but there are not: "
					+ eventLikelihoods.size() + "(events) vs. " + timeLikelihoods.size() + "(time values)");
		}
		return decide(eventLikelihoods, timeLikelihoods);
	}

	/**
	 * Decides whether the likelihoods indicate an anomaly
	 * 
	 * @param eventLikelihoods
	 * @param timeLikelihoods
	 * @return true for anomaly, false otherwise
	 */
	protected abstract boolean decide(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods);

	public boolean[] areAnomalies(TimedInput testSequences) {
		if (Settings.isDebug()) {
			final Path testLabelFile = Paths.get("testLabels.csv");
			try {
				Files.deleteIfExists(testLabelFile);
				Files.createFile(testLabelFile);
			} catch (final IOException e1) {
				logger.error("Unexpected exception occured", e1);
			}
			try (BufferedWriter bw = Files.newBufferedWriter(testLabelFile, StandardCharsets.UTF_8)) {
				for (final TimedWord s : testSequences) {
					bw.append(s.getLabel().toString());
					bw.append('\n');
				}
			} catch (final IOException e) {
				logger.error("Unexpected exception occured", e);
			}
		}
		final boolean[] result = new boolean[testSequences.size()];

		// parallelism does not destroy determinism
		IntStream.range(0, testSequences.size()).parallel().forEach(i -> {
			final TimedWord s = testSequences.get(i);
			result[i] = isAnomaly(s);
		});
		return result;
	}

	public void setModel(ProbabilisticModel model) {
		this.model = model;
	}

	public static double aggregate(TDoubleList list, ProbabilityAggregationMethod aggType) {
		if (list.isEmpty()) {
			return Double.POSITIVE_INFINITY;
		}
		double result = -1;
		if (aggType == ProbabilityAggregationMethod.MULTIPLY) {
			result = 0;
			for (int i = 0; i < list.size(); i++) {
				result += Math.log(list.get(i));
			}
		} else if (aggType == ProbabilityAggregationMethod.LUK_T) {
			result = list.get(0);
			for (int i = 1; i < list.size(); i++) {
				result = Math.max(0, list.get(i) + result - 1);
			}
		} else if (aggType == ProbabilityAggregationMethod.LUK_STRONG_DISJUNCTION) {
			result = list.get(0);
			for (int i = 1; i < list.size(); i++) {
				result = Math.min(1, list.get(i) + result);
			}
		} else if (aggType == ProbabilityAggregationMethod.NORMALIZED_MULTIPLY) {
			result = 0;
			for (int i = 0; i < list.size(); i++) {
				result += Math.log(list.get(i));
			}
			result /= list.size();
			result = Math.exp(result);
		} else if (aggType == ProbabilityAggregationMethod.NORMALIZED_MULTIPLY_UNSTABLE) {
			result = 1;
			for (int i = 0; i < list.size(); i++) {
				result *= list.get(i);
			}
			result = Math.pow(result, 1.0 / list.size());
		}
		return result;
	}

	/**
	 * Computes the product of the probabilities in log space. @param probabilities the probabilities @return the product of the probabilities in log space
	 */
	public static double aggregate(TDoubleList probabilities) {
		return aggregate(probabilities, ProbabilityAggregationMethod.NORMALIZED_MULTIPLY);
	}
}
