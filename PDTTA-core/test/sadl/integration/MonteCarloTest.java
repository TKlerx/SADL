package sadl.integration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jsat.distributions.ContinuousDistribution;
import jsat.distributions.Normal;
import jsat.distributions.SingleValueDistribution;

public class MonteCarloTest {

	@Test
	public void testGaussian() {
		final Normal n = new Normal(0,1);
		final MonteCarIntegration mc = new MonteCarIntegration(100000);
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
	}

	@Test
	public void testSingleValue() {
		final ContinuousDistribution d = new SingleValueDistribution(0);
		final MonteCarIntegration mc = new MonteCarIntegration(100000);
		mc.preprocess(d, 1000, -20, 20);
		System.out.println("\nmc.integrate(0)=" + mc.integrate(d.pdf(-2)));
		assertEquals(0, mc.integrate(d.pdf(-2)), 0.005);

		System.out.println("mc.integrate(1)=" + mc.integrate(d.pdf(0)));
		assertEquals(1, mc.integrate(d.pdf(0)), 0.005);

		System.out.println("mc.integrate(0)=" + mc.integrate(d.pdf(1)));
		assertEquals(0, mc.integrate(d.pdf(1)), 0.005);

		final ContinuousDistribution d2 = new SingleValueDistribution(1);
		final MonteCarIntegration mc2 = new MonteCarIntegration(100000);
		mc2.preprocess(d2, 1000, -20, 20);
		System.out.println("\nmc.integrate(0)=" + mc2.integrate(d2.pdf(-2)));
		assertEquals(0, mc2.integrate(d2.pdf(-2)), 0.005);

		System.out.println("mc.integrate(1)=" + mc2.integrate(d2.pdf(1)));
		assertEquals(1, mc2.integrate(d2.pdf(1)), 0.005);

		System.out.println("mc.integrate(0)=" + mc2.integrate(d2.pdf(0)));
		assertEquals(0, mc2.integrate(d2.pdf(0)), 0.005);
	}

}
