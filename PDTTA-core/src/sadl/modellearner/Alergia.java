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

package sadl.modellearner;

import gnu.trove.map.TObjectIntMap;

import java.util.Set;
import java.util.stream.Collectors;

import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.models.TauPTA;
import sadl.structure.Transition;

/**
 * 
 * @author Timo Klerx
 *
 */
public class Alergia implements PdfaLearner {

	private final double alpha;
	TauPTA pta;
	TauPtaLearner learner = new TauPtaLearner();

	public Alergia(double alpha) {
		this.alpha = alpha;
	}

	@Override
	public PDFA train(TimedInput trainingSequences) {
		// TODO Auto-generated method stub
		pta = learner.train(trainingSequences);
		return null;
	}

	/**
	 * true if states are compatible
	 * 
	 * @param qu
	 * @param qv
	 * @return
	 */
	boolean alergiaCompatibilityTest(int qu, int qv) {
		int f1, n1, f2, n2;
		double gamma, bound;
		f1 = learner.finalStateCount.get(qu);
		n1 = totalFreq(learner.transitionCount, qu);
		f2 = learner.finalStateCount.get(qv);
		n2 = totalFreq(learner.transitionCount, qv);
		gamma = Math.abs(((double) f1) / ((double) n1) - ((double) f2) / ((double) n2));
		bound = ((Math.sqrt(1.0 / n1) + Math.sqrt(1.0 / n2)) * Math.sqrt(Math.log(2.0 / alpha))) / 1.41421356237309504880;
		if (gamma > bound) {
			return false;
		}

		for (final String a : pta.getAlphabet().getSymbols()) {
			f1 = symbolFreq(learner.transitionCount, qu, a);
			n1 = totalFreq(learner.transitionCount, qu);
			f2 = symbolFreq(learner.transitionCount, qv, a);
			n2 = totalFreq(learner.transitionCount, qv);
			gamma = Math.abs(((double) f1) / ((double) n1) - ((double) f2) / ((double) n2));
			bound = ((Math.sqrt(1.0 / n1) + Math.sqrt(1.0 / n2)) * Math.sqrt(Math.log(2.0 / alpha))) / 1.41421356237309504880;
			if (gamma > bound) {
				return false;
			}
		}
		return true;
	}

	/**
	 * frequency of transitions arriving at state qu
	 * 
	 * @param transitionCount
	 * @param qu
	 * @return
	 */
	private int totalFreq(TObjectIntMap<Transition> transitionCount, int qu) {
		int result = 0;
		final Set<Transition> incoming = pta.getAllTransitions().stream().filter(t -> (t.getToState() == qu)).collect(Collectors.toSet());
		for (final Transition t : incoming) {
			result += transitionCount.get(t);
		}
		return result;
	}

	private int symbolFreq(TObjectIntMap<Transition> transitionCount, int fromState, String event) {
		int result = 0;
		final Set<Transition> incoming = pta.getAllTransitions().stream().filter(t -> (t.getFromState() == fromState && t.getSymbol().equals(event)))
				.collect(Collectors.toSet());
		for (final Transition t : incoming) {
			result += transitionCount.get(t);
		}
		return result;
	}

}
