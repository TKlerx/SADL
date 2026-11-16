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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import sadl.input.TimedInput;
import sadl.modellearner.TrebaNativeParityLearner.Arc;
import sadl.modellearner.TrebaNativeParityLearner.State;
import sadl.models.PDFA;

/** Red/blue state merging using the published signed MDI divergence increment. */
public final class MdiPaperLearner implements PdfaLearner {
    private final double alpha;
    private final int minimumVisits;

    public MdiPaperLearner(double alpha, int minimumVisits) {
        this.alpha = alpha;
        this.minimumVisits = minimumVisits;
    }

    @Override
    public PDFA train(TimedInput sequences) {
        final var tree = TrebaNativeParityLearner.prefixTree(sequences);
        State root = tree.root();
        List<State> red = new ArrayList<>(List.of(root));
        root.red = true;
        final List<int[]> samples = new ArrayList<>();
        for (var word : sequences) {
            final int[] symbols = new int[word.length()];
            for (int i = 0; i < symbols.length; i++) {
                symbols[i] = sequences.getAlphIndex(word.getSymbol(i));
            }
            samples.add(symbols);
        }
        Arc next;
        while ((next = TrebaNativeParityLearner.firstBlue(red, minimumVisits)) != null) {
            final State blue = next.target;
            final List<State> states = reachable(root);
            boolean accepted = false;
            for (State target : red) {
                final Map<State, State> merged = fold(states, target, blue);
                final State candidate = merged.get(root);
                final int removed = states.size() - reachable(candidate).size();
                if (removed > 0 && divergenceIncrease(root, candidate, samples) / removed < alpha) {
                    root = candidate;
                    final List<State> nextRed = new ArrayList<>();
                    for (State previous : red) {
                        final State representative = merged.get(previous);
                        if (!representative.red) {
                            representative.red = true;
                            nextRed.add(representative);
                        }
                    }
                    red = nextRed;
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                blue.red = true;
                red.add(blue);
            }
        }
        return TrebaNativeParityLearner.toPdfa(root, sequences, tree.alphabetSize(), 0.0);
    }

    private static List<State> reachable(State root) {
        final var seen = new java.util.LinkedHashSet<State>();
        final var pending = new ArrayDeque<State>();
        seen.add(root);
        pending.add(root);
        while (!pending.isEmpty()) {
            for (Arc arc : pending.remove().arcs.values()) {
                if (seen.add(arc.target)) {
                    pending.add(arc.target);
                }
            }
        }
        return new ArrayList<>(seen);
    }

    private static Map<State, State> fold(List<State> states, State target, State source) {
        record Pair(State left, State right) {}
        final Map<State, State> parent = new LinkedHashMap<>();
        final Map<State, TreeMap<Integer, Arc>> outgoing = new LinkedHashMap<>();
        for (State state : states) {
            parent.put(state, state);
            outgoing.put(state, new TreeMap<>(state.arcs));
        }
        final var pending = new ArrayDeque<Pair>();
        pending.add(new Pair(target, source));
        while (!pending.isEmpty()) {
            final Pair pair = pending.remove();
            final State left = representative(parent, pair.left());
            final State right = representative(parent, pair.right());
            if (left == right) {
                continue;
            }
            parent.put(right, left);
            final var arcs = outgoing.get(left);
            outgoing.remove(right).forEach((symbol, arc) -> {
                final Arc existing = arcs.putIfAbsent(symbol, arc);
                if (existing != null) {
                    pending.add(new Pair(existing.target, arc.target));
                }
            });
        }
        final Map<State, State> groups = new LinkedHashMap<>();
        final Map<State, State> copies = new LinkedHashMap<>();
        for (State state : states) {
            copies.put(state, groups.computeIfAbsent(representative(parent, state), key -> new State()));
        }
        for (State state : states) {
            final State copy = copies.get(state);
            copy.stops += state.stops;
            state.arcs.forEach((symbol, arc) ->
                    copy.arcs.computeIfAbsent(symbol, key -> new Arc(copies.get(arc.target), 0)).count += arc.count);
        }
        groups.values().forEach(State::updateTotal);
        return copies;
    }

    private static State representative(Map<State, State> parent, State state) {
        while (parent.get(state) != state) {
            state = parent.get(state);
        }
        return state;
    }

    private static double divergenceIncrease(State current, State candidate, List<int[]> samples) {
        // ponytail: score whole training traces. Optimize only if measured on larger data.
        double sum = 0;
        for (int[] sample : samples) {
            sum += logProbability(current, sample) - logProbability(candidate, sample);
        }
        return sum / samples.size();
    }

    private static double logProbability(State root, int[] sample) {
        double probability = 0;
        State state = root;
        for (int symbol : sample) {
            final Arc arc = state.arcs.get(symbol);
            probability += Math.log((double) arc.count / state.total);
            state = arc.target;
        }
        return probability + Math.log((double) state.stops / state.total);
    }
}
