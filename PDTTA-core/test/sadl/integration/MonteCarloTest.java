package sadl.integration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jsat.distributions.Normal;

public class MonteCarloTest {

	@Test
	public void testGaussian() {
		final Normal n = new Normal(0,1);
		System.out.println(n.pdf(0));
		final MonteCarlo mc = new MonteCarlo(10000, 10000);
		mc.preprocess(n, -20, 20);
		// System.out.println("mc.integrate(0.4)=" + mc.integrate(n.pdf(0)));
		// assertEquals(1, mc.integrate(n.pdf(0)), 0.001);
		System.out.println("mc.integrate(1)=" + mc.integrate(n.pdf(1)));
		assertEquals(0.3091, mc.integrate(n.pdf(1)), 0.0001);
	}

}
