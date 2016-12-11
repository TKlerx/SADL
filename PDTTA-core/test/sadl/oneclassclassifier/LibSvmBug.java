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
package sadl.oneclassclassifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class LibSvmBug {


	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final LibSvmBug bug = new LibSvmBug();
		try (BufferedReader bw = Files.newBufferedReader(Paths.get("data.csv"), StandardCharsets.UTF_8)) {
			String line = "";
			final List<double[]> trainingData = new ArrayList<>();
			while ((line = bw.readLine()) != null) {
				if (!line.isEmpty()) {
					final String[] split = line.split(",");
					final double[] row = new double[split.length];
					for (int i = 0; i < split.length; i++) {
						row[i] = Double.parseDouble(split[i]);
					}
					trainingData.add(row);
				}
			}
			bug.train(trainingData);
		}
	}

	svm_model model;
	svm_parameter param;

	public LibSvmBug() {
		param = new svm_parameter();
		param.probability = 0; // default 0
		param.gamma = 4.826430398760473E15;// 0.2;
		param.nu = 0.25733100650382557;// 0.01; // precision/recall variable
		param.C = 874.7836937317443;// 1;
		param.svm_type = svm_parameter.ONE_CLASS;
		param.kernel_type = 1;// svm_parameter.RBF;
		param.cache_size = 200000;
		param.eps = 0.8230927132407901;// 0.001;
		param.degree = 1481648982; // 3
	}

	public void train(List<double[]> trainingSamples) {
		model = svmTrain(trainingSamples);
	}

	private svm_model svmTrain(final List<double[]> train) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("data.csv"), StandardCharsets.UTF_8)) {
			for (final double[] ds : train) {
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
		@SuppressWarnings("hiding")
		final
		svm_model model = svm.svm_train(prob, param);

		return model;
	}

	public double evaluate(final double[] features, @SuppressWarnings("hiding") final svm_model model) {

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
