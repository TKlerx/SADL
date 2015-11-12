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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sadl.interfaces.ModelLearner;
import sadl.modellearner.rtiplus.GreedyPDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner;
import sadl.modellearner.rtiplus.SimplePDRTALearner.DistributionCheckType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.OperationTesterType;
import sadl.modellearner.rtiplus.SimplePDRTALearner.SplitPosition;
import sadl.run.factories.LearnerFactory;

/**
 * 
 * @author Fabian Witter
 *
 */
@Parameters(commandDescription = "Run with RTI+ as a learner")
public class RTIFactory implements LearnerFactory {

	@Parameter(names = "-sig", required = true, arity = 1)
	double sig;

	@Parameter(names = "-hist", required = true, arity = 1)
	String hist;

	@Parameter(names = "-greedy", arity = 0)
	boolean greedy = false;

	@Parameter(names = "-em", arity = 1)
	OperationTesterType tester = OperationTesterType.LRT;

	@Parameter(names = "-ida", arity = 1)
	DistributionCheckType distrCheck = DistributionCheckType.DISABLED;

	@Parameter(names = "-sp", arity = 1)
	SplitPosition splitPos = SplitPosition.MIDDLE;

	@Parameter(names = "-bop", arity = 1)
	String boolOps = "AAA";

	@Parameter(names = "-steps", arity = 1)
	String stepsDir = null;

	@Override
	public ModelLearner create() {

		ModelLearner ml = null;
		if (greedy) {
			ml = new GreedyPDRTALearner(sig, hist, tester, distrCheck, splitPos, boolOps, stepsDir);
		} else {
			ml = new SimplePDRTALearner(sig, hist, tester, distrCheck, splitPos, boolOps, stepsDir);
		}
		return ml;
	}

}
