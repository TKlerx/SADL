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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.modellearner.TrebaNativeParityLearner.Arc;
import sadl.modellearner.TrebaNativeParityLearner.State;
import sadl.models.PDFA;

/**
 * Treba-style blue-side scoring and red/blue state merging.
 * The native scorer can read an uninitialized stopping score when blue can stop
 * and red cannot. This implementation defines that contribution from the
 * candidate probabilities by default. The opt-in historical mode models the
 * pinned native build's observed slot reuse.
 */
public final class MdiTrebaLearner implements PdfaLearner {
    private final double alpha;
    private final int minimumVisits;
    private final boolean historical;
    private final double smoothingPrior;

    public MdiTrebaLearner(double alpha, int minimumVisits) {
        this(alpha, minimumVisits, false, 0.0);
    }

    public static MdiTrebaLearner historical(double alpha, int minimumVisits) {
        return new MdiTrebaLearner(alpha, minimumVisits, true, 0.0);
    }

    MdiTrebaLearner(double alpha, int minimumVisits, boolean historical, double smoothingPrior) {
        this.alpha = alpha;
        this.minimumVisits = minimumVisits;
        this.historical = historical;
        this.smoothingPrior = smoothingPrior;
    }

    @Override
    public PDFA train(TimedInput sequences) {
        final var tree = TrebaNativeParityLearner.prefixTree(sequences);
        final int observations = sequences.size();
        // Model the pinned build's reused stack slots only within this training run.
        final Map<Integer, Double> stopSlots = historical ? new HashMap<>() : null;
        TrebaNativeParityLearner.mergeRedBlue(tree.root(), minimumVisits,
                (red, blue) -> score(red, blue, observations, stopSlots) < alpha);
        return TrebaNativeParityLearner.toPdfa(tree.root(), sequences, tree.alphabetSize(), smoothingPrior);
    }

    static boolean compatible(State red, State blue, int observations, double alpha) {
        return score(red, blue, observations) < alpha;
    }

    static double score(State red, State blue, int observations) {
        return score(red, blue, observations, null);
    }

    private static double score(State red, State blue, int observations, Map<Integer, Double> stopSlots) {
        final var pending = new ArrayDeque<Visit>();
        pending.push(new Visit(red, blue, 0, 0));
        double sum = 0;
        int removed = 0;
        while (!pending.isEmpty()) {
            final Visit visit = pending.peek();
            if (visit.arcs == null) {
                if (visit.red != null) {
                    removed++;
                }
                if (visit.blue.stops > 0) {
                    final double blueStop = visit.blueLog + logRatio(visit.blue.stops, visit.blue.total);
                    final double mergedStop = visit.mergedStop(stopSlots, pending.size());
                    sum += (double) visit.blue.stops / observations * Math.abs(blueStop - mergedStop);
                }
                visit.arcs = visit.blue.arcs.entrySet().iterator();
            }
            if (!visit.arcs.hasNext()) {
                pending.pop();
                continue;
            }
            if (stopSlots != null) {
                // The compiler reuses the slot for a pointer when traversing an arc.
                stopSlots.remove(pending.size());
            }
            pending.push(visit.nextChild(stopSlots != null));
        }
        return sum / removed;
    }

    private static double logRatio(int count, int total) {
        return Math.log((double) count / total);
    }

    private static final class Visit {
        final State red;
        final State blue;
        double blueLog;
        double mergedLog;
        Iterator<Map.Entry<Integer, Arc>> arcs;

        Visit(State red, State blue, double blueLog, double mergedLog) {
            this.red = red;
            this.blue = blue;
            this.blueLog = blueLog;
            this.mergedLog = mergedLog;
        }

        double mergedStop(Map<Integer, Double> stopSlots, int depth) {
            if (stopSlots != null && (red == null || red.stops == 0)) {
                mergedLog = 0.0;
                return stopSlots.getOrDefault(depth, 0.0);
            }
            final double score = red == null ? mergedLog + logRatio(blue.stops, blue.total)
                    : mergedLog + logRatio(blue.stops + red.stops, blue.total + red.total);
            if (stopSlots != null) {
                stopSlots.put(depth, score);
            }
            return score;
        }

        Visit nextChild(boolean historical) {
            final var entry = arcs.next();
            final Arc blueArc = entry.getValue();
            final Arc redArc = red == null ? null : red.arcs.get(entry.getKey());
            final double blueStep = logRatio(blueArc.count, blue.total);
            // Treba carries its accumulators forward across sibling arcs.
            blueLog += blueStep;
            if (redArc == null) {
                return new Visit(null, blueArc.target, blueLog, historical ? mergedLog : mergedLog + blueStep);
            }
            mergedLog += logRatio(blueArc.count + redArc.count, blue.total + red.total);
            return new Visit(redArc.target, blueArc.target, blueLog, mergedLog);
        }
    }
}
