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

package sadl.run.factories.learn;

import sadl.constants.EventsCreationStrategy;
import sadl.constants.KDEFormelVariant;
import sadl.constants.PTAOrdering;
import sadl.constants.TransitionsType;
import sadl.interfaces.ModelLearner;
import sadl.modellearner.ButlaPdtaLearner;
import sadl.run.factories.LearnerFactory;

import com.beust.jcommander.Parameter;

public class ButlaFactory implements LearnerFactory {
	@Parameter(names = "-bandwidth")
	double bandwidth;

	@Parameter(names = "-alpha")
	double alpha;

	@Parameter(names = "-transitionsToCheck")
	TransitionsType transitionsToCheck;

	@Parameter(names = "-anomalyProbability")
	double anomalyProbability;

	@Parameter(names = "-mergeStrategy")
	PTAOrdering mergeStrategy;

	@Parameter(names = "-splittingStrategy")
	EventsCreationStrategy splittingStrategy;

	@Parameter(names = "-formelVariant")
	KDEFormelVariant formelVariant;

	@Override
	public ModelLearner create() {

		final ModelLearner learner = new ButlaPdtaLearner(bandwidth, alpha, transitionsToCheck, anomalyProbability, anomalyProbability, mergeStrategy,
				splittingStrategy,
				formelVariant);
		return learner;
	}

}
