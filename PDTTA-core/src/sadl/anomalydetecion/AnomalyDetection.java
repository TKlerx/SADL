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
package sadl.anomalydetecion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.detectors.AnomalyDetector;
import sadl.evaluation.Evaluation;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.interfaces.TrainableDetector;
import sadl.utils.IoUtils;

public class AnomalyDetection {


	private static Logger logger = LoggerFactory.getLogger(AnomalyDetection.class);
	private final AnomalyDetector anomalyDetector;
	ProbabilisticModelLearner learner;
	ProbabilisticModel learnedModel;


	public AnomalyDetector getAnomalyDetector() {
		return anomalyDetector;
	}

	public ProbabilisticModelLearner getLearner() {
		return learner;
	}

	public ProbabilisticModel getLearnedModel() {
		return learnedModel;
	}
	public AnomalyDetection(AnomalyDetector anomalyDetector, ProbabilisticModelLearner learner) {
		super();
		this.anomalyDetector = anomalyDetector;
		this.learner = learner;
	}

	public AnomalyDetection(AnomalyDetector anomalyDetector, ProbabilisticModel model) {
		super();
		this.anomalyDetector = anomalyDetector;
		this.learnedModel = model;
	}

	/**
	 * 
	 * @param dataFile
	 *            The file containing train and testset
	 * @return the result of training with the train and testing on the test set
	 * @throws IOException
	 */
	public ExperimentResult trainTest(Path dataFile) throws IOException {
		return trainTest(dataFile, false);
	}

	public ExperimentResult trainTest(TimedInput train, TimedInput test) throws IOException {
		final StopWatch sw = new StopWatch();
		sw.start();
		train(train);
		sw.stop();
		final long trainTime = sw.getTime();
		sw.reset();
		sw.start();
		final ExperimentResult testResult = test(test);
		sw.stop();
		final long testTime = sw.getTime();
		testResult.setExecutionTimeTraining(trainTime);
		testResult.setExecutionTimeTesting(testTime);
		return testResult;
	}

	public ExperimentResult trainTest(Path dataFile, boolean skipFirstElement) throws IOException {
		checkFileExistance(dataFile);

		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(dataFile, skipFirstElement);
		return trainTest(trainTest.getKey(), trainTest.getValue());
	}

	/**
	 * 
	 * @param dataFile
	 *            File containing the train data
	 * @return the learned model (in this case a PDTTA)
	 * @throws IOException
	 */
	public ProbabilisticModel train(Path dataFile) throws IOException {
		if (Files.notExists(dataFile)) {
			final IOException e = new IOException("Path with input data does not exist(" + dataFile + ")");
			logger.error("Input data file does not exist", e);
			throw new RuntimeException(e);
		}
		return train(TimedInput.parse(dataFile));
	}

	public ProbabilisticModel train(TimedInput trainingInput) {

		learnedModel = learner.train(trainingInput);
		if (anomalyDetector instanceof TrainableDetector) {
			anomalyDetector.setModel(learnedModel);
			((TrainableDetector) anomalyDetector).train(trainingInput);
		}
		trainingInput.clearWords();
		return learnedModel;

	}
	/**
	 * 
	 * @param dataFile
	 *            File containing the test data
	 * @return the experiment result
	 * @throws IOException
	 */
	public ExperimentResult test(Path dataFile) throws IOException {
		checkFileExistance(dataFile);
		return test(TimedInput.parse(dataFile));
	}

	private void checkFileExistance(Path dataFile) throws IOException {
		if (Files.notExists(dataFile)) {
			final IOException e = new IOException("Path with input data does not exist(" + dataFile + ")");
			logger.error("Input data file does not exist", e);
			throw e;
		}
	}

	public ExperimentResult test(TimedInput testInput) {

		final Evaluation eval = new Evaluation(anomalyDetector, learnedModel);
		final ExperimentResult result = eval.evaluate(testInput);
		testInput.clearWords();
		logger.info("F-Measure={}", result.getFMeasure());
		return result;
	}

	/**
	 * 
	 * @param dataFile
	 *            The file containing train and testset
	 * @return the result of training with the train and testing on the test set
	 * @throws IOException
	 */
	public ExperimentResult trainTest(String dataFile) throws IOException {
		return trainTest(Paths.get(dataFile));
	}

}
