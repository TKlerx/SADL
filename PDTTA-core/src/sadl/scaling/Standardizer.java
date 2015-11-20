package sadl.scaling;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import jsat.math.OnLineStatistics;
import sadl.interfaces.Scaling;

public class Standardizer implements Scaling {
	double mus[];
	double sigmas[];
	@Override
	public void setFeatureCount(int length) {
		mus = new double[length];
		sigmas = new double[length];
	}

	boolean trained = false;
	@Override
	public List<double[]> train(List<double[]> input) {
		final OnLineStatistics[] os = new OnLineStatistics[mus.length];
		for (int i = 0; i < os.length; i++) {
			os[i] = new OnLineStatistics();
		}
		for (final double[] ds : input) {
			for (int i = 0; i < ds.length; i++) {
				os[i].add(ds[i]);
			}
		}
		for (int i = 0; i < os.length; i++) {
			mus[i] = os[i].getMean();
			sigmas[i] = os[i].getStandardDeviation();
		}
		trained = true;
		return scale(input);
	}

	@Override
	public List<double[]> scale(List<double[]> input) {
		if (!trained) {
			throw new IllegalStateException("Scaler must be trained first before scaling");
		}
		final List<double[]> result = new ArrayList<>(input.size());
		for (final double[] ds : input) {
			final double[] temp = new double[ds.length];
			for (int i = 0; i < ds.length; i++) {
				if (Precision.equals(0, sigmas[i])) {
					temp[i] = 1;
				} else {
					temp[i] = (ds[i] - mus[i]) / sigmas[i];
				}
			}
			result.add(temp);
		}
		return result;
	}

}
