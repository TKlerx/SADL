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
package sadl.run.datagenerators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sadl.input.TimedInput;
import sadl.run.datagenerators.AlgoWeakTwo.AnomalyType;
import sadl.utils.MasterSeed;

public class AlgoWeakTwoExportTest {
    @Before
    @After
    public void resetSamplingSeed() {
        MasterSeed.reset();
    }

    @Test
    public void pdrtaExportsMatchedNormalAndA2BranchesWithTheSameSymbols() throws IOException {
        final var generator = new AlgoWeakTwo();
        assertMatchedBranches(generator::sampleNormalPdrtaSequence,
                () -> generator.sampleAbnormalPdrtaSequence(AnomalyType.TWO), false);
    }

    @Test
    public void pnttaExportsMatchedNormalAndA2BranchesWithTheSameSymbols() throws IOException {
        final var generator = new AlgoWeakTwo();
        assertMatchedBranches(() -> generator.sampleNormalPndtta(false),
                () -> generator.sampleAbnormalPndttaA2(false), false);
    }

    @Test
    public void deterministicPdttaKeepsTheObservableA2SymbolSwap() throws IOException {
        final var generator = new AlgoWeakTwo();
        assertMatchedBranches(() -> generator.sampleNormalPndtta(true),
                () -> generator.sampleAbnormalPndttaA2(true), true);
    }

    private static void assertMatchedBranches(Supplier<String> normal, Supplier<String> a2, boolean deterministic)
            throws IOException {
        final var sharedSymbols = Map.of("c", "a", "d", "a");
        final var normalTraces = shortTraces(normal, false, deterministic ? Map.of("c", "a", "d", "b") : sharedSymbols);
        final var a2Traces = shortTraces(a2, true, deterministic ? Map.of("c", "b", "d", "a") : sharedSymbols);
        for (String branch : List.of("c", "d")) {
            assertTrue("Expected a normal and A2 pair with the same delays on branch " + branch,
                    normalTraces.stream().anyMatch(timing -> timing.branch().equals(branch) && a2Traces.contains(timing)));
        }
    }

    private static Set<BranchTiming> shortTraces(Supplier<String> export, boolean anomaly,
            Map<String, String> firstSymbols) throws IOException {
        final var inputText = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            inputText.append(export.get()).append('\n');
        }
        final Set<BranchTiming> traces = new HashSet<>();
        for (var word : TimedInput.parse(new StringReader(inputText.toString()))) {
            assertEquals("Exported class label", anomaly, word.isAnomaly());
            if (word.length() == 2) {
                final String branch = word.getSymbol(1);
                assertEquals("Exported first event on branch " + branch, firstSymbols.get(branch), word.getSymbol(0));
                traces.add(new BranchTiming(branch, word.getTimeValue(0), word.getTimeValue(1)));
            }
        }
        return traces;
    }

    private record BranchTiming(String branch, int firstDelay, int secondDelay) {}
}
