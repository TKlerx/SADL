package sadl.oneclassclassifier;

import java.util.List;

import jsat.classifiers.neuralnetwork.SOM;
import sadl.constants.ScalingMethod;
import sadl.utils.DatasetTransformationUtils;

public class SomClassifier extends NumericClassifier {
	SOM som;

	public SomClassifier(ScalingMethod scalingMethod, int height, int width) {
		super(scalingMethod);
		som = new SOM(height, width);
	}

	@Override
	protected boolean isOutlierScaled(double[] scaledTestSample) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void trainModelScaled(List<double[]> scaledTrainSamples) {
		som.trainC(DatasetTransformationUtils.doublesToDataSet(scaledTrainSamples));
	}

}
