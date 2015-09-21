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

import org.apache.commons.math3.util.Precision;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import sadl.constants.ScalingMethod;

/**
 * 
 * @author Timo Klerx
 *
 */
public class LibSvmClassifier extends NumericClassifier {
	// private static Logger logger = LoggerFactory.getLogger(LibSvmClassifier.class);
	svm_model model;
	svm_parameter param;

	public LibSvmClassifier(int useProbability, double gamma, double nu, int kernelType, double eps, int degree, ScalingMethod scalingMethod) {
		super(scalingMethod);
		param = new svm_parameter();
		param.probability = useProbability; // default 0
		param.gamma = gamma;// 0.2;
		param.nu = nu;// 0.01; // precision/recall variable
		param.svm_type = svm_parameter.ONE_CLASS;
		param.kernel_type = kernelType;// svm_parameter.RBF;
		param.cache_size = 2000;
		param.eps = eps;// 0.001;
		param.degree = degree; // 3
	}

	private svm_model svmTrain(final List<double[]> train) {
		if (Precision.equals(param.gamma, 0)) {
			param.gamma = ((double) 1) / train.get(0).length;
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


	@Override
	public boolean isOutlierScaled(double[] testSample) {
		return evaluate(testSample, model) == -1;
	}

	@Override
	public void trainModelScaled(List<double[]> trainSamples) {
		model = svmTrain(trainSamples);

	}

}
