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
package sadl.run.datagenerators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import jsat.distributions.ContinuousDistribution;
import jsat.distributions.GaussianMixture;
import jsat.distributions.Normal;
import jsat.distributions.Uniform;
import sadl.constants.ClassLabel;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.models.PDTTA;
import sadl.structure.Transition;
import sadl.structure.ZeroProbTransition;

public class AlgoWeakTwo {

	// TODO load normal and abnormal PDRTA from file
	// TODO replace a_1/a_2 by 'a' after sampling
	public static void main(String[] args) {
		final AlgoWeakTwo foo = new AlgoWeakTwo();
		System.out.println("normal");
		foo.normalPndtta(true);
		foo.normalPndtta(false);

		System.out.println("A1");
		foo.abnormalPndttaA1(true);
		foo.abnormalPndttaA1(false);

		System.out.println("A2");
		foo.abnormalPndttaA2(true);
		foo.abnormalPndttaA2(false);

		System.out.println("A3");
		foo.abnormalPndttaA3(true);
		foo.abnormalPndttaA3(false);

		System.out.println("A4");
		foo.abnormalPndttaA4(true);
		foo.abnormalPndttaA4(false);

		System.out.println("A5");
		foo.abnormalPndttaA5(true);
		foo.abnormalPndttaA5(false);

		System.out.println("A6");
		foo.abnormalPndttaA6(true);
		foo.abnormalPndttaA6(false);

		System.out.println("A7");
		foo.abnormalPndttaA7(true);
		foo.abnormalPndttaA7(false);
	}

	public void abnormalPndttaA1(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "d", "c", "a" };
		} else {
			symbols = new String[] { "b", "d", "c", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA2(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a2", "a1" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		String t1Symbol;
		if (deterministicAutomaton) {
			t1Symbol = "b";
		} else {
			t1Symbol = "a2";
		}
		final Transition t1 = new Transition(0, 1, t1Symbol, 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = "a";
		} else {
			t2Symbol = "a1";
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA3(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t2.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t1.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA4(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA5(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.25);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.25);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 1);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(0, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		sampleSequence.setLabel(ClassLabel.ANOMALY);
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA6(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(2, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(1, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void abnormalPndttaA7(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 2, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 1, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

	public void normalPndtta(boolean deterministicAutomaton) {
		final String[] symbols;
		if (deterministicAutomaton) {
			symbols = new String[] { "b", "c", "d", "a" };
		} else {
			symbols = new String[] { "b", "c", "d", "a1", "a2" };
		}
		final TimedInput alphabet = new TimedInput(symbols);
		final Set<Transition> transitions = new HashSet<>();
		final Transition t1 = new Transition(0, 1, symbols[3], 0.5);
		transitions.add(t1);
		String t2Symbol;
		if (deterministicAutomaton) {
			t2Symbol = symbols[0];
		} else {
			t2Symbol = symbols[4];
		}
		final Transition t2 = new Transition(0, 2, t2Symbol, 0.5);
		transitions.add(t2);
		final Transition t3 = new Transition(1, 3, symbols[1], 1);
		transitions.add(t3);
		final Transition t4 = new Transition(2, 3, symbols[2], 1);
		transitions.add(t4);
		final Transition t5 = new Transition(3, 0, symbols[0], 0.5);
		transitions.add(t5);
		final TIntDoubleMap finalStateProbabilities = new TIntDoubleHashMap();
		finalStateProbabilities.put(3, 0.5);
		final PDFA normalPdfa = new PDFA(alphabet, transitions, finalStateProbabilities, null);
		final Map<ZeroProbTransition, ContinuousDistribution> transitionDistributions = new HashMap<>();
		transitionDistributions.put(t1.toZeroProbTransition(), new Normal(2.5, 1));
		transitionDistributions.put(t2.toZeroProbTransition(), new GaussianMixture(new double[] { 10, 15 }, new double[] { 2, 2 }));
		transitionDistributions.put(t3.toZeroProbTransition(), new Normal(50, 10));
		transitionDistributions.put(t4.toZeroProbTransition(), new Normal(5, 1));
		transitionDistributions.put(t5.toZeroProbTransition(), new Uniform(1, 20));
		final PDTTA normalPntta = new PDTTA(normalPdfa, transitionDistributions, null);
		String seq;
		final TimedWord sampleSequence = normalPntta.sampleSequence();
		seq = sampleSequence.toString();
		// seq = seq.replaceAll("a\\d", "a");
		System.out.println(seq);
		System.out.println();
	}

}
