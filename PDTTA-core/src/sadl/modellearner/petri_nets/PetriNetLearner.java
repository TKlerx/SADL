/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.modellearner.petri_nets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.models.petri_nets.PetriNet;
import sadl.run.factories.learn.PetriNetFactory;

public class PetriNetLearner implements ProbabilisticModelLearner {
	private static Logger logger = LoggerFactory.getLogger(PetriNetFactory.class);

	public PetriNetLearner(double alpha) {
		logger.info("Alpha has value {}", alpha);
	}

	public PetriNetLearner(double alpha, double param2) {
		logger.info("alpha={}, param2={}", alpha, param2);
	}

	@Override
	public PetriNet train(TimedInput trainingSequences) {
		// TODO Auto-generated method stub
		return null;
	}

}
