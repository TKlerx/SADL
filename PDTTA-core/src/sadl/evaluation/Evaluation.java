package sadl.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;
import sadl.detectors.PdttaDetector;
import sadl.experiments.PdttaExperimentResult;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.interfaces.Model;

public class Evaluation {
	PdttaDetector detector;
	Model model;
	private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

	public Evaluation(PdttaDetector detector, Model model) {
		super();
		this.detector = detector;
		this.model = model;
	}

	public PdttaExperimentResult evaluate(TimedInput testSet) {
		detector.setModel(model);
		detector.areAnomalies(testSet);
		logger.info("Testing with {} sequences", testSet.size());
		detector.setModel(model);
		final boolean[] detectorResult = detector.areAnomalies(testSet);
		int truePos = 0;
		int trueNeg = 0;
		int falsePos = 0;
		int falseNeg = 0;
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
		final PdttaExperimentResult expResult = new PdttaExperimentResult(truePos, trueNeg, falsePos, falseNeg);

		return expResult;

	}

}
