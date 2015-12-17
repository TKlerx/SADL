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

package sadl.detectors.featureCreators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jsat.math.OnLineStatistics;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.AnomalyDetector;

public class UberFeatureCreator extends FullFeatureCreator {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(FullFeatureCreator.class);

	@Override
	public double[] createFeatures(TDoubleList eventLikelihoods, TDoubleList timeLikelihoods, ProbabilityAggregationMethod aggType) {
		final TDoubleList result = new TDoubleArrayList(super.createFeatures(eventLikelihoods, timeLikelihoods, aggType));

		final OnLineStatistics eventStat = getStatistics(eventLikelihoods);
		final OnLineStatistics timeStat = getStatistics(timeLikelihoods);
		final TDoubleList eventDiffs = calcDiffs(eventLikelihoods);
		final TDoubleList timeDiffs = calcDiffs(timeLikelihoods);

		double eventStdDev = eventStat.getStandardDeviation();
		if (Double.isNaN(eventStdDev) || Double.isInfinite(eventStdDev)) {
			eventStdDev = AnomalyDetector.ILLEGAL_VALUE;
		}
		result.add(eventStdDev);
		result.add(eventLikelihoods.size());
		result.add(eventDiffs.min());
		result.add(eventDiffs.max());

		double timeStdDev = timeStat.getStandardDeviation();
		if (Double.isNaN(timeStdDev) || Double.isInfinite(timeStdDev)) {
			timeStdDev = AnomalyDetector.ILLEGAL_VALUE;
		}
		result.add(timeStdDev);
		result.add(timeLikelihoods.size());
		result.add(timeDiffs.min());
		result.add(timeDiffs.max());

		return result.toArray();
	}

	private TDoubleList calcDiffs(TDoubleList likelihoods) {
		if (likelihoods.size() <= 1) {
			return new TDoubleArrayList(new double[] { AnomalyDetector.ILLEGAL_VALUE });
		} else {
			final TDoubleList result = new TDoubleArrayList(likelihoods.size());
			double last = likelihoods.get(0);
			for (int i = 1; i < likelihoods.size(); i++) {
				final double temp = likelihoods.get(i);
				result.add(Math.abs(temp - last));
				last = temp;
			}
			return result;
		}
	}

	private OnLineStatistics getStatistics(TDoubleList timeLikelihoods) {
		final OnLineStatistics stat = new OnLineStatistics();
		for (int i = 0; i < timeLikelihoods.size(); i++) {
			final double d = timeLikelihoods.get(i);
			stat.add(d);
		}
		return stat;
	}
}
