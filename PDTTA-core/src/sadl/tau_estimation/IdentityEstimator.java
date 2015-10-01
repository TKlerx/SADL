package sadl.tau_estimation;

import java.io.Serializable;

import jsat.distributions.ContinuousDistribution;
import sadl.interfaces.TauEstimator;

public class IdentityEstimator implements TauEstimator,Serializable {

	private static final long serialVersionUID = -5717365756851163588L;

	@Override
	public double estimateTau(ContinuousDistribution d, double timeValue) {
		return d.pdf(timeValue);
	}

}
