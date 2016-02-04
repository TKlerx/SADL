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
package sadl.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;
import sadl.detectors.AnomalyDetector;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.AutomatonModel;
import sadl.interfaces.ProbabilisticModel;

public class Evaluation {
	AnomalyDetector detector;
	ProbabilisticModel model;
	private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

	public Evaluation(AnomalyDetector detector, ProbabilisticModel model) {
		super();
		this.detector = detector;
		this.model = model;
	}

	public ExperimentResult evaluate(TimedInput testSet) {
		logger.info("Testing with {} sequences", testSet.size());
		detector.setModel(model);
		final boolean[] detectorResult = detector.areAnomalies(testSet);
		long truePos = 0;
		long trueNeg = 0;
		long falsePos = 0;
		long falseNeg = 0;
		// prec = tp/(tp +fp)
		// The precision is the ratio between correctly detected anomalies and
		// all detected anomalies
		// rec = tp/(tp+fn)
		// The recall is the ratio between detected anomalies and all anomalies

		for (int i = 0; i < testSet.size(); i++) {
			final TimedWord s = testSet.get(i);
			if (s.getLabel() == ClassLabel.NORMAL) {
				if (detectorResult[i]) {
					// detector said anomaly
					falsePos++;
				} else {
					// detector said normal
					trueNeg++;
				}
			} else if (s.getLabel() == ClassLabel.ANOMALY) {
				if (detectorResult[i]) {
					// detector said anomaly
					truePos++;
				} else {
					// detector said normal
					falseNeg++;
				}
			}

		}
		final ExperimentResult expResult = new ExperimentResult(truePos, trueNeg, falsePos, falseNeg);

		if (model instanceof AutomatonModel) {
			expResult.setNumberOfStates(((AutomatonModel) model).getStateCount());
		}

		return expResult;

	}

}
