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
package sadl.integration;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Normal;
import jsat.distributions.SingleValueDistribution;
import jsat.distributions.Uniform;

public class MonteCarloTest {

	@Test
	public void testGaussian() {
		final StopWatch sw = new StopWatch();
		sw.start();
		final Normal n = new Normal(0,1);
		final MonteCarloIntegration mc = new MonteCarloIntegration(100000);
		mc.preprocess(n, 1000, -20, 20);
		System.out.println("mc.integrate(0.02)=" + mc.integrate(n.pdf(-2)));
		assertEquals(0.04605, mc.integrate(n.pdf(-2)), 0.005);
		System.out.println("mc.integrate(0.4)=" + mc.integrate(n.pdf(0)));
		assertEquals(1, mc.integrate(n.pdf(0)), 0.005);
		System.out.println("mc.integrate(.31692)=" + mc.integrate(n.pdf(1)));
		assertEquals(.31692, mc.integrate(n.pdf(1)), 0.005);
		for (double d = -10; d < 0; d += 0.02) {
			final double negIntegral = mc.integrate(n.pdf(-d));
			assertEquals("Error while comparing for d=" + d, mc.integrate(n.pdf(d)), negIntegral, 0.00001);
			assertEquals("Error while comparing for d=" + d, n.cdf(d) * 2, negIntegral, 0.005);
		}
		sw.stop();
		System.out.println("Gaussian integration took " + DurationFormatUtils.formatDurationHMS(sw.getTime()));
	}

	@Test
	public void testSingleValue() {
		final StopWatch sw = new StopWatch();
		sw.start();
		final ContinuousDistribution d = new SingleValueDistribution(0);
		final MonteCarloIntegration mc = new MonteCarloIntegration(100000);
		mc.preprocess(d, 1000, -20, 20);
		System.out.println("\nmc.integrate(0)=" + mc.integrate(d.pdf(-2)));
		assertEquals(0, mc.integrate(d.pdf(-2)), 0.005);

		System.out.println("mc.integrate(1)=" + mc.integrate(d.pdf(0)));
		assertEquals(1, mc.integrate(d.pdf(0)), 0.005);

		System.out.println("mc.integrate(0)=" + mc.integrate(d.pdf(1)));
		assertEquals(0, mc.integrate(d.pdf(1)), 0.005);

		final ContinuousDistribution d2 = new SingleValueDistribution(1);
		final MonteCarloIntegration mc2 = new MonteCarloIntegration(100000);
		mc2.preprocess(d2, 1000, -20, 20);
		System.out.println("\nmc.integrate(0)=" + mc2.integrate(d2.pdf(-2)));
		assertEquals(0, mc2.integrate(d2.pdf(-2)), 0.005);

		System.out.println("mc.integrate(1)=" + mc2.integrate(d2.pdf(1)));
		assertEquals(1, mc2.integrate(d2.pdf(1)), 0.005);

		System.out.println("mc.integrate(0)=" + mc2.integrate(d2.pdf(0)));
		assertEquals(0, mc2.integrate(d2.pdf(0)), 0.005);
		sw.stop();
		System.out.println("Single value integration took " + DurationFormatUtils.formatDurationHMS(sw.getTime()));
	}

	@Test
	public void testUniform() {
		final StopWatch sw = new StopWatch();
		sw.start();
		final Uniform n = new Uniform(1, 3);
		final MonteCarloIntegration mc = new MonteCarloIntegration(100000);
		mc.preprocess(n, 1000, -20, 20);
		System.out.println("mc.integrate(1)=" + mc.integrate(n.pdf(1)));
		assertEquals(1, mc.integrate(n.pdf(1)), 0.005);
		System.out.println("mc.integrate(2)=" + mc.integrate(n.pdf(2)));
		assertEquals(1, mc.integrate(n.pdf(2)), 0.005);
		System.out.println("mc.integrate(3)=" + mc.integrate(n.pdf(13)));
		assertEquals(1, mc.integrate(n.pdf(3)), 0.005);
		sw.stop();
		System.out.println("Uniform integration took " + DurationFormatUtils.formatDurationHMS(sw.getTime()));
	}

}
