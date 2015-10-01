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