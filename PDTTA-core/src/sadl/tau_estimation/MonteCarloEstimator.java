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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsat.distributions.ContinuousDistribution;
import sadl.integration.MonteCarloIntegration;
import sadl.interfaces.TauEstimator;

public class MonteCarloEstimator implements TauEstimator, Serializable {
	private static Logger logger = LoggerFactory.getLogger(MonteCarloEstimator.class);

	private static final long serialVersionUID = -4398919127157832777L;
	Map<ContinuousDistribution, MonteCarloIntegration> mcs = new ConcurrentHashMap<>();
	int pointsToStore, numberOfSteps;

	public MonteCarloEstimator(int numberOfSteps, int pointsToStore) {
		super();
		this.pointsToStore = pointsToStore;
		this.numberOfSteps = numberOfSteps;
	}

	@Override
	public double estimateTau(ContinuousDistribution d, double timeValue) {
		final MonteCarloIntegration mc = mcs.get(d);
		final double result = mc.integrate(d.pdf(timeValue));
		return result;
	}

	@Override
	public void preprocess(Collection<ContinuousDistribution> values) {
		for (final ContinuousDistribution d : values) {
			final MonteCarloIntegration mc = new MonteCarloIntegration(pointsToStore);
			mcs.put(d, mc);
			logger.debug("Preprocessed {} Monte Carlo Intervals.", mcs.size());
		}
		mcs.entrySet().parallelStream().forEach(e -> {
			e.getValue().preprocess(e.getKey(), numberOfSteps);
		});
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mcs == null) ? 0 : mcs.hashCode());
		result = prime * result + numberOfSteps;
		result = prime * result + pointsToStore;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MonteCarloEstimator other = (MonteCarloEstimator) obj;
		if (mcs == null) {
			if (other.mcs != null) {
				return false;
			}
		} else if (!mcs.equals(other.mcs)) {
			return false;
		}
		if (numberOfSteps != other.numberOfSteps) {
			return false;
		}
		if (pointsToStore != other.pointsToStore) {
			return false;
		}
		return true;
	}

}