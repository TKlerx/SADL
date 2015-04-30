/**
 * This file is part of SADL, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.upb.timok.oneclassclassifier;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.functions.LibSVM;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;
import de.upb.timok.constants.ScalingMethod;
import de.upb.timok.utils.DatasetTransformationUtils;
/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public class WekaSvmClassifier implements OneClassClassifier {
	private static Logger logger = LoggerFactory.getLogger(WekaSvmClassifier.class);

	LibSVM wekaSvm;
	Filter filter = null;

	public WekaSvmClassifier(int useProbability, double gamma, double nu, double costs, int kernelType, double eps, int degree, ScalingMethod scalingMethod) {
		wekaSvm = new LibSVM();
		wekaSvm.setCost(costs);
		wekaSvm.setGamma(gamma);
		wekaSvm.setNu(nu);
		wekaSvm.setEps(eps);
		wekaSvm.setDegree(degree);
		if (scalingMethod == ScalingMethod.NORMALIZE) {
			filter = new Normalize();
		} else if (scalingMethod == ScalingMethod.STANDARDIZE) {
			filter = new Standardize();
		}
		if (useProbability > 0) {
			wekaSvm.setProbabilityEstimates(true);
		} else {
			wekaSvm.setProbabilityEstimates(false);
		}
		// * Set type of SVM (default: 0)
		// * 0 = C-SVC
		// * 1 = nu-SVC
		// * 2 = one-class SVM
		// * 3 = epsilon-SVR
		// * 4 = nu-SVR</pre>
		wekaSvm.setSVMType(new SelectedTag(LibSVM.SVMTYPE_ONE_CLASS_SVM, LibSVM.TAGS_SVMTYPE));
		// * <pre> -K &lt;int&gt;
		// * Set type of kernel function (default: 2)
		// * 0 = linear: u'*v
		// * 1 = polynomial: (gamma*u'*v + coef0)^degree
		// * 2 = radial basis function: exp(-gamma*|u-v|^2)
		// * 3 = sigmoid: tanh(gamma*u'*v + coef0)</pre>
		wekaSvm.setKernelType(new SelectedTag(kernelType, LibSVM.TAGS_KERNELTYPE));
	}

	// Take input from the $n$ thresholds and output 0 or 1
	// This must be a one class svm!
	@Override
	public void train(List<double[]> trainingSamples) {
		Instances data = DatasetTransformationUtils.trainingSetToInstances(trainingSamples);
		// setting class attribute if the data format does not provide this information
		// For example, the XRFF format saves the class attribute information as well
		try {
			if (filter != null) {
				filter.setInputFormat(data);
				data = Filter.useFilter(data, filter);
			}
			if (data.classIndex() == -1) {
				data.setClassIndex(data.numAttributes() - 1);
			}
			wekaSvm.buildClassifier(data);
		} catch (final Exception e) {
			logger.error("Unexpected exception", e);
		}

	}

	@Override
	public boolean isOutlier(double[] testSample) {
		Instance wekaInstance;
		if (testSet == null) {
			final ArrayList<double[]> temp = new ArrayList<>();
			temp.add(testSample);
			testSet = DatasetTransformationUtils.testSetToInstances(temp);
			wekaInstance = testSet.get(0);
		} else {
			wekaInstance = new DenseInstance(1, testSample);
			testSet.add(wekaInstance);
			wekaInstance.setDataset(testSet);
		}
		try {
			if (filter != null) {
				testSet = Filter.useFilter(testSet, filter);
				wekaInstance = testSet.lastInstance();
			}
			double result;
			result = wekaSvm.classifyInstance(wekaInstance);
			if (Double.isNaN(result)) {
				return true;
			} else {
				return false;
			}
		} catch (final Exception e) {
			logger.error("Unexpected exception", e);
		}
		return false;
	}



	Instances testSet;

	@Override
	public boolean[] areAnomalies(List<double[]> testSamples) {
		final boolean[] result = new boolean[testSamples.size()];
		testSet = DatasetTransformationUtils.testSetToInstances(testSamples);
		try {
			if (filter != null) {
				testSet = Filter.useFilter(testSet, filter);
			}
			for (int i = 0; i < testSamples.size(); i++) {
				final Instance wekaInstance = testSet.get(i);
				double d;
				d = wekaSvm.classifyInstance(wekaInstance);
				if (Double.isNaN(d)) {
					result[i] = true;
				} else {
					result[i] = false;
				}
			}
		} catch (final Exception e) {
			logger.error("Unexpected exception", e);
		}
		return result;
	}

}
