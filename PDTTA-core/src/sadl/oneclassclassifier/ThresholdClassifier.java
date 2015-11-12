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

package sadl.oneclassclassifier;

import java.util.List;

import sadl.detectors.featureCreators.SmallFeatureCreator;

public class ThresholdClassifier implements OneClassClassifier {

	double aggEventThreshold, aggTimeThreshold, singleEventThreshold = Double.NaN, singleTimeThreshold = Double.NaN;

	public ThresholdClassifier(double aggEventThreshold, double aggTimeThreshold) {
		super();
		this.aggEventThreshold = aggEventThreshold;
		this.aggTimeThreshold = aggTimeThreshold;
	}

	public ThresholdClassifier(double aggEventThreshold, double aggTimeThreshold, double singleEventThreshold, double singleTimeThreshold) {
		this(aggEventThreshold, aggTimeThreshold);
		this.singleEventThreshold = singleEventThreshold;
		this.singleTimeThreshold = singleTimeThreshold;
	}

	@Override
	public void train(List<double[]> trainingSamples) {
		// Do nothing
	}

	/**
	 * {@link SmallFeatureCreator#createFeatures(gnu.trove.list.TDoubleList, gnu.trove.list.TDoubleList)}
	 */
	@Override
	public boolean isOutlier(double[] testSample) {

		// this is ugly but there is no better way!
		if (testSample.length == 2 && Double.isNaN(singleEventThreshold) && Double.isNaN(singleTimeThreshold)) {
			// only use aggregationThresholds. FeatureCreator should be MinimalFeatureCreator
			return aggDecide(testSample[0],testSample[1]);
		} else if (testSample.length == 6) {
			// use all thresholds
			// FeatureCreator should be smallFeatureCreator (ignore max values at index 0 and 4)
			final boolean aggResult = aggDecide(testSample[2], testSample[5]);
			final boolean singleResult = testSample[1] <= singleEventThreshold || testSample[4] <= singleTimeThreshold;
			return aggResult || singleResult;
		} else {
			String errorMessage = "";
			if (testSample.length != 2 && testSample.length != 6) {
				errorMessage =
						"Can only detect anomalies for feature creators minimal and small (vector has to have length 2 or 6), but this has length="
								+ testSample.length;
			} else if (testSample.length == 2 && (!Double.isNaN(singleEventThreshold) || !Double.isNaN(singleTimeThreshold))) {
				errorMessage = "Specified minimal feature creator but at the same time single event/time thresholds. This is not possible";
			} else {
				errorMessage = "The parameters of the ThresholdClassifier do not match as intended.";
			}
			throw new IllegalArgumentException(errorMessage);
		}
	}

	protected boolean aggDecide(double eventAgg, double timeAgg) {
		if (eventAgg <= aggEventThreshold || timeAgg <= aggTimeThreshold) {
			return true;
		} else {
			return false;
		}
	}

}
