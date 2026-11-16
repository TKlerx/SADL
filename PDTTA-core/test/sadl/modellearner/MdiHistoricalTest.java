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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;

import org.junit.Test;

import com.beust.jcommander.JCommander;

import sadl.run.factories.learn.PdttaFactory;
import sadl.utils.IoUtils;

public class MdiHistoricalTest {
    @Test
    public void learnsTheFrozenHistoricalStateCount() throws Exception {
        final var split = IoUtils.readTrainTestFile(Path.of(getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
        final var factory = new PdttaFactory();
        new JCommander(factory).parse("-mergeMethod", "MDI_TREBA_HISTORICAL", "-tauEstimation", "DENSITY");
        final var learner = factory.create();
        final var model = learner.train(split.getLeft());
        assertEquals(39, model.getStateCount());
        final var word = split.getRight().get(0);
        assertTrue(model.isInAutomaton(word));
        assertArrayEquals(new double[] {0.09290602356729853, 0.40443131462333826, 1.0, 0.08702334772743905,
                0.5560821195930615, 0.06169033155236773, 0.010722388933616336, 0.010722388933616336,
                0.08702334772743905, 0.01357681329137086}, model.calculateProbabilities(word).getFirst().toArray(), 1e-12);
        assertEquals(39, learner.train(split.getLeft()).getStateCount());
    }
}
