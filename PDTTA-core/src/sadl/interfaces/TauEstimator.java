package sadl.interfaces;

import jsat.distributions.ContinuousDistribution;

public interface TauEstimator {
	public double estimateTau(ContinuousDistribution d, double timeValue);
}
