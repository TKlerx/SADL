/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.modellearner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import sadl.input.TimedInput;
import sadl.models.PDFA;
import sadl.structure.Transition;

/** Treba's ALERGIA state-merging order and probability estimation. */
public final class TrebaNativeParityLearner implements PdfaLearner {
    private final double alpha;
    private final boolean recursive;
    private final int minimumVisits;
    private final double smoothingPrior;

    public TrebaNativeParityLearner(double alpha, boolean recursive, int minimumVisits) {
        this(alpha, recursive, minimumVisits, 0.0);
    }

    TrebaNativeParityLearner(double alpha, boolean recursive, int minimumVisits, double smoothingPrior) {
        this.alpha = alpha;
        this.recursive = recursive;
        this.minimumVisits = minimumVisits;
        this.smoothingPrior = smoothingPrior;
    }

    @Override
    public PDFA train(TimedInput sequences) {
        final PrefixTree tree = prefixTree(sequences);
        final State root = tree.root();
        final List<State> red = new ArrayList<>();
        red.add(root);
        root.red = true;
        Arc incoming;
        while ((incoming = firstBlue(red, minimumVisits)) != null) {
            final State blue = incoming.target;
            State match = null;
            for (State candidate : red) {
                candidate.updateTotal();
                if (compatible(candidate, blue)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                blue.red = true;
                red.add(blue);
            } else {
                incoming.target = match;
                fold(match, blue);
            }
        }
        return toPdfa(root, sequences, tree.alphabetSize(), smoothingPrior);
    }

    record PrefixTree(State root, int alphabetSize) {}

    static PrefixTree prefixTree(TimedInput sequences) {
        final State root = new State();
        int alphabetSize = 0;
        for (var word : sequences) {
            State state = root;
            for (int i = 0; i < word.length(); i++) {
                final int symbol = sequences.getAlphIndex(word.getSymbol(i));
                alphabetSize = Math.max(alphabetSize, symbol + 1);
                final Arc arc = state.arcs.computeIfAbsent(symbol, key -> new Arc(new State(), 0));
                arc.count++;
                state.total++;
                state = arc.target;
            }
            state.stops++;
            state.total++;
        }

        return new PrefixTree(root, alphabetSize);
    }

    static Arc firstBlue(List<State> red, int minimumVisits) {
        for (State state : red) {
            for (Arc arc : state.arcs.values()) {
                if (!arc.target.red) {
                    arc.target.updateTotal();
                    if (arc.target.total >= minimumVisits) {
                        return arc;
                    }
                }
            }
        }
        return null;
    }

    boolean compatible(State left, State right) {
        if (!locallyCompatible(left, right)) {
            return false;
        }
        if (recursive) {
            for (var entry : left.arcs.entrySet()) {
                final Arc other = right.arcs.get(entry.getKey());
                if (other != null && !locallyCompatible(entry.getValue().target, other.target)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean locallyCompatible(State left, State right) {
        final double bound = (Math.sqrt(1.0 / left.total) + Math.sqrt(1.0 / right.total))
                * Math.sqrt(Math.log(2.0 / alpha)) / Math.sqrt(2.0);
        if (difference(left.stops, right.stops, left.total, right.total) > bound) {
            return false;
        }
        for (var entry : left.arcs.entrySet()) {
            final Arc other = right.arcs.get(entry.getKey());
            if (difference(entry.getValue().count, other == null ? 0 : other.count, left.total, right.total) > bound) {
                return false;
            }
        }
        for (var entry : right.arcs.entrySet()) {
            if (!left.arcs.containsKey(entry.getKey()) && difference(0, entry.getValue().count, left.total, right.total) > bound) {
                return false;
            }
        }
        return true;
    }

    private static double difference(int left, int right, int leftTotal, int rightTotal) {
        return Math.abs((double) left / leftTotal - (double) right / rightTotal);
    }

    private static void fold(State target, State source) {
        // Traverse in symbol order and update counts as each branch is folded.
        // The native loop refreshes totals only when selecting blue or testing red.
        record Branch(State target, State source, int previousSymbol) {}
        final var stack = new ArrayDeque<Branch>();
        target.stops += source.stops;
        stack.push(new Branch(target, source, -1));
        while (!stack.isEmpty()) {
            final Branch branch = stack.pop();
            final var entry = branch.source().arcs.higherEntry(branch.previousSymbol());
            if (entry == null) {
                continue;
            }
            final int symbol = entry.getKey();
            final Arc incoming = entry.getValue();
            final Arc existing = branch.target().arcs.get(symbol);
            stack.push(new Branch(branch.target(), branch.source(), symbol));
            if (existing == null) {
                branch.target().arcs.put(symbol, new Arc(incoming.target, incoming.count));
            } else {
                existing.count += incoming.count;
                if (existing.target == incoming.target) {
                    throw new IllegalStateException("Treba fold reached the same source and target state");
                }
                existing.target.stops += incoming.target.stops;
                stack.push(new Branch(existing.target, incoming.target, -1));
            }
        }
    }

    static PDFA toPdfa(State root, TimedInput alphabet, int alphabetSize, double smoothingPrior) {
        final var states = new ArrayList<State>();
        final var transitions = new HashSet<Transition>();
        final var stopping = new TIntDoubleHashMap();
        root.id = 0;
        states.add(root);
        for (int i = 0; i < states.size(); i++) {
            final State state = states.get(i);
            state.updateTotal();
            final double total = state.total + (alphabetSize + 1) * smoothingPrior;
            stopping.put(i, total == 0 ? 0.0 : (state.stops + smoothingPrior) / total);
            for (var entry : state.arcs.entrySet()) {
                final Arc arc = entry.getValue();
                if (arc.target.id == -1) {
                    arc.target.id = states.size();
                    states.add(arc.target);
                }
                transitions.add(new Transition(i, arc.target.id, alphabet.getSymbol(entry.getKey()),
                        (arc.count + smoothingPrior) / total));
            }
            if (smoothingPrior > 0) {
                for (int symbol = 0; symbol < alphabetSize; symbol++) {
                    if (!state.arcs.containsKey(symbol)) {
                        transitions.add(new Transition(i, i, alphabet.getSymbol(symbol), smoothingPrior / total));
                    }
                }
            }
        }
        final var result = new PDFA(alphabet, transitions, stopping, null, new TIntIntHashMap());
        result.makeImmutable();
        return result;
    }

    static final class State {
        final TreeMap<Integer, Arc> arcs = new TreeMap<>();
        int stops;
        int total;
        int id = -1;
        boolean red;

        void updateTotal() {
            total = stops;
            for (Arc arc : arcs.values()) {
                total += arc.count;
            }
        }
    }

    static final class Arc {
        State target;
        int count;

        Arc(State target, int count) {
            this.target = target;
            this.count = count;
        }
    }
}
