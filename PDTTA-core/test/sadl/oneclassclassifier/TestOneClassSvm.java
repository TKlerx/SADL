/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package sadl.oneclassclassifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import sadl.constants.ScalingMethod;
import sadl.oneclassclassifier.LibSvmClassifier;
import sadl.utils.Normalizer;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class TestOneClassSvm {
	public static final int TRAIN_SIZE = 1000;
	public static final int TEST_SIZE = 10;
	static List<double[]> train = new ArrayList<>(TRAIN_SIZE);
	static List<double[]> test = new ArrayList<>(TEST_SIZE);

	/**
	 * comparing weka LibSVM and normal libSVM result
	 * 
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void main(final String[] args) {
		// feature should be at index 0
		final TestOneClassSvm tester = new TestOneClassSvm();
		// final WekaSvmClassifier wekaSvm = new WekaSvmClassifier(0, 0.2, 0.01, 1, 2, 0.001, 3, ScalingMethod.NORMALIZE);
		final LibSvmClassifier libSvm = new LibSvmClassifier(0, 0.2, 0.01, 1, 2, 0.001, 3, ScalingMethod.NORMALIZE);
		tester.createData();
		// wekaSvm.train(train);
		libSvm.train(train);
		final Normalizer norm = new Normalizer(train.get(0).length);
		final List<double[]> normalizedTrain = norm.train(train);
		// List<double[]> normalizedTrain = train;

		final svm_model model = tester.svmTrain(normalizedTrain, 0, 0.2, 0.01, 1, 2, 0.001, 3);
		System.out.println("testset");
		int libSvmRightAnswer = 0;
		int libSvmWrongAnswer = 0;
		// int wekaRightAnswer = 0;
		// int wekaWrongAnswer = 0;
		int wrongAnswer = 0;
		int rightAnswer = 0;
		// final boolean[] wekaTrainResult = wekaSvm.areAnomalies(train);
		// final boolean[] wekaTestResult = wekaSvm.areAnomalies(test);
		final boolean[] libSvmTrainResult = libSvm.areAnomalies(train);
		countAnomalies(libSvmTrainResult);
		// final boolean[] libSvmTestResult = libSvm.areAnomalies(test);

		final List<double[]> normalizedTest = norm.normalize(test);
		// List<double[]> normalizedTest = test;

		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("train_svm_normalized.csv"), StandardCharsets.UTF_8)) {
			for (final double[] ds : normalizedTrain) {
				for (int i = 0; i < ds.length; i++) {

					bw.append(Double.toString(ds[i]));
					if (i < ds.length - 1) {
						bw.append(',');
					}
				}
				bw.append('\n');
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("test_svm_normalized.csv"), StandardCharsets.UTF_8)) {
			for (final double[] ds : normalizedTest) {
				for (int i = 0; i < ds.length; i++) {

					bw.append(Double.toString(ds[i]));
					if (i < ds.length - 1) {
						bw.append(',');
					}
				}
				bw.append('\n');
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < test.size(); i++) {
			// all of the test data are outliers
			final boolean isAnomaly = libSvm.isOutlier(test.get(i));
			final double v = tester.evaluate(normalizedTest.get(i), model);
			if (-1 == v) {
				// libsvm says outlier
				rightAnswer++;
			} else {
				// libsvm says normal
				wrongAnswer++;
			}
			// if (wekaSvm.isOutlier(test.get(i))) {
			// weka says outlier
			// wekaRightAnswer++;
			// } else {
			// wekaWrongAnswer++;
			// }
			if (isAnomaly) {
				// libsvm says outlier
				libSvmRightAnswer++;
			} else {
				// libsvm says normal
				libSvmWrongAnswer++;
			}
		}
		System.out.println("Expected Result: 10/0");
		System.out.println("SvmResult: RightAnswers=" + rightAnswer + "; WrongAnswers=" + wrongAnswer);
		System.out.println("LibSvmResult: RightAnswers=" + libSvmRightAnswer + "; WrongAnswers=" + libSvmWrongAnswer);
		// System.out.println("WekaResult: RightAnswers=" + wekaRightAnswer + "; WrongAnswers=" + wekaWrongAnswer);

		libSvmRightAnswer = libSvmWrongAnswer = rightAnswer = wrongAnswer = 0;
		System.out.println("\ntrainset again");
		for (int i = 0; i < train.size(); i++) {
			// all the data should be normal!
			final boolean isAnomaly = libSvm.isOutlier(train.get(i));
			final double v = tester.evaluate(normalizedTrain.get(i), model);

			if (1 == v) {
				// libsvm says normal
				rightAnswer++;
			} else {
				// libsvm says outlier
				wrongAnswer++;
			}
			// if (wekaSvm.isOutlier(train.get(i))) {
			// // System.err.println("weka says something different");
			// wekaWrongAnswer++;
			// } else {
			// wekaRightAnswer++;
			// }
			if (!isAnomaly) {
				// libsvm says normal
				libSvmRightAnswer++;
			} else {
				// libsvm says outlier
				libSvmWrongAnswer++;
			}
		}
		System.out.println("Expected Result with this parameter setting: 600/400");
		System.out.println("SvmResult: RightAnswers=" + rightAnswer + "; WrongAnswers=" + wrongAnswer);
		System.out.println("LibSvmResult: RightAnswers=" + libSvmRightAnswer + "; WrongAnswers=" + libSvmWrongAnswer);
		// System.out.println("WekaResult: RightAnswers=" + wekaRightAnswer + "; WrongAnswers=" + wekaWrongAnswer);

		// will return 800/200 with this parameter setting

		// if (!Arrays.equals(libSvmTrainResult, wekaTrainResult) || !Arrays.equals(libSvmTestResult, wekaTestResult)) {
		// System.err.println("Weka and libsvm do not result in same output");
		// }
	}

	private static void countAnomalies(boolean[] libSvmTrainResult) {
		int anomalies = 0;
		int normals = 0;
		for (int i = 0; i < libSvmTrainResult.length; i++) {
			if (libSvmTrainResult[i]) {
				anomalies++;
			} else {
				normals++;
			}
		}
		System.out.println("Normals=" + normals + "; Anomalies=" + anomalies);
	}

	public void createData() {

		for (int i = 0; i < TRAIN_SIZE; i++) {
			final double[] vals = { 0, ((i + i) % 10) };
			train.add(vals);
		}
		for (int i = 0; i < TEST_SIZE; i++) {
			final double[] vals = { 0, -i - 2 }; // 50% negative
			test.add(vals);
		}

	}

	@SuppressWarnings("hiding")
	private svm_model svmTrain(final List<double[]> train, int useProbability, double gamma, double nu, double costs, int kernelType, double eps, int degree) {

		final svm_problem prob = new svm_problem();
		final int dataCount = train.size();
		prob.y = new double[dataCount];
		prob.l = dataCount;
		prob.x = new svm_node[dataCount][];

		for (int i = 0; i < dataCount; i++) {
			final double[] features = train.get(i);
			prob.x[i] = new svm_node[features.length - 1];
			for (int j = 1; j < features.length; j++) {
				final svm_node node = new svm_node();
				node.index = j;
				node.value = features[j];
				prob.x[i][j - 1] = node;
			}
			prob.y[i] = +1;
		}

		final svm_parameter param = new svm_parameter();
		param.probability = useProbability; // default 0
		param.gamma = gamma;// 0.2;
		param.nu = nu;// 0.01; // precision/recall variable
		param.C = costs;// 1;
		param.svm_type = svm_parameter.ONE_CLASS;
		param.kernel_type = kernelType;// svm_parameter.RBF;
		param.cache_size = 2000;
		param.degree = degree;
		param.eps = eps;// 0.001;

		final svm_model model = svm.svm_train(prob, param);

		return model;
	}

	public double evaluate(final double[] features, final svm_model model) {

		final svm_node[] nodes = new svm_node[features.length - 1];
		for (int i = 1; i < features.length; i++) {
			final svm_node node = new svm_node();
			node.index = i;
			node.value = features[i];

			nodes[i - 1] = node;
		}

		final int totalClasses = 2;
		final int[] labels = new int[totalClasses];
		svm.svm_get_labels(model, labels);
		return svm.svm_predict(model, nodes);
	}

}
