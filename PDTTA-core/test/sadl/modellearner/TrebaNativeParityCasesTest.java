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

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TrebaNativeParityCasesTest {
    @Parameters(name = "{0}")
    public static List<Object[]> cases() throws Exception {
        final List<Object[]> cases = new ArrayList<>();
        final var rows = Files.readAllLines(Path.of("..", "specs", "treba-alergia-parity", "native-depth.csv"));
        assertEquals(4, rows.size());
        final String[] mismatches = {"grandchild mismatch", "immediate successor mismatch", "local mismatch"};
        for (int i = 0; i < mismatches.length; i++) {
            final String[] row = rows.get(i + 1).split(",");
            for (int recursive = 0; recursive <= 1; recursive++) {
                cases.add(new Object[] {mismatches[i] + (recursive == 1 ? " with recursion" : " without recursion"),
                        "1".equals(row[3 + recursive]), Integer.parseInt(row[0]), Integer.parseInt(row[1]),
                        recursive == 1});
            }
        }
        return cases;
    }

    private final boolean expected;
    private final int left;
    private final int right;
    private final boolean recursive;

    public TrebaNativeParityCasesTest(String name, boolean expected, int left, int right, boolean recursive) {
        this.expected = expected;
        this.left = left;
        this.right = right;
        this.recursive = recursive;
    }

    @Test
    public void matchesNativeCompatibilityDecision() {
        final var states = new TrebaNativeParityLearner.State[7];
        for (int i = 0; i < states.length; i++) {
            states[i] = new TrebaNativeParityLearner.State();
            states[i].total = 100;
        }
        for (int i : new int[] {0, 1, 3, 4, 5}) {
            states[i].arcs.put(0, new TrebaNativeParityLearner.Arc(states[i + 1], 100));
        }
        states[2].stops = 100;
        states[6].stops = 100;
        final var learner = new TrebaNativeParityLearner(0.05, recursive, 0);
        assertEquals(expected, learner.compatible(states[left], states[right]));
    }
}
