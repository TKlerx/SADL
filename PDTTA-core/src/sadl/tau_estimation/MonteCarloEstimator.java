package sadl.tau_estimation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jsat.distributions.ContinuousDistribution;
import sadl.integration.MonteCarIntegration;
import sadl.interfaces.TauEstimator;

public class MonteCarloEstimator implements TauEstimator, Serializable {
	private static final long serialVersionUID = -4398919127157832777L;
	Map<ContinuousDistribution, MonteCarIntegration> mcs = new HashMap<>();
	int pointsToStore, numberOfSteps;

	public MonteCarloEstimator(int numberOfSteps, int pointsToStore) {
		super();
		this.pointsToStore = pointsToStore;
		this.numberOfSteps = numberOfSteps;
	}

	@Override
	public double estimateTau(ContinuousDistribution d, double timeValue) {
		MonteCarIntegration mc = mcs.get(d);
		if (mc == null) {
			mc = new MonteCarIntegration(pointsToStore);
			mc.preprocess(d, numberOfSteps);
			mcs.put(d, mc);
		}
		final double result = mc.integrate(d.pdf(timeValue));
		return result;
	}

}