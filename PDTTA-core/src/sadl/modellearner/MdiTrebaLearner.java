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
import java.util.Iterator;
import java.util.Map;

import sadl.input.TimedInput;
import sadl.modellearner.TrebaNativeParityLearner.Arc;
import sadl.modellearner.TrebaNativeParityLearner.State;
import sadl.models.PDFA;

/** Treba's blue-side MDI scorer and red/blue state-merging order. */
public final class MdiTrebaLearner implements PdfaLearner {
    private final double alpha;
    private final int minimumVisits;

    public MdiTrebaLearner(double alpha, int minimumVisits) {
        this.alpha = alpha;
        this.minimumVisits = minimumVisits;
    }

    @Override
    public PDFA train(TimedInput sequences) {
        final var tree = TrebaNativeParityLearner.prefixTree(sequences);
        final int observations = sequences.size();
        TrebaNativeParityLearner.mergeRedBlue(tree.root(), minimumVisits,
                (red, blue) -> compatible(red, blue, observations, alpha));
        return TrebaNativeParityLearner.toPdfa(tree.root(), sequences, tree.alphabetSize(), 0.0);
    }

    static boolean compatible(State red, State blue, int observations, double alpha) {
        return score(red, blue, observations) < alpha;
    }

    static double score(State red, State blue, int observations) {
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
                    final double mergedStop = visit.red == null
                            ? visit.mergedLog + logRatio(visit.blue.stops, visit.blue.total)
                            : visit.mergedLog + logRatio(visit.blue.stops + visit.red.stops,
                                    visit.blue.total + visit.red.total);
                    sum += (double) visit.blue.stops / observations * Math.abs(blueStop - mergedStop);
                }
                visit.arcs = visit.blue.arcs.entrySet().iterator();
            }
            if (!visit.arcs.hasNext()) {
                pending.pop();
                continue;
            }
            final var entry = visit.arcs.next();
            final Arc blueArc = entry.getValue();
            final Arc redArc = visit.red == null ? null : visit.red.arcs.get(entry.getKey());
            final double blueStep = logRatio(blueArc.count, visit.blue.total);
            // Treba carries its accumulators forward across sibling arcs.
            visit.blueLog += blueStep;
            if (redArc != null) {
                visit.mergedLog += logRatio(blueArc.count + redArc.count, visit.blue.total + visit.red.total);
            }
            // A blue-only subtree keeps its conditional probabilities after the fold.
            final double mergedLog = redArc == null ? visit.mergedLog + blueStep : visit.mergedLog;
            pending.push(new Visit(redArc == null ? null : redArc.target, blueArc.target, visit.blueLog, mergedLog));
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
    }
}
