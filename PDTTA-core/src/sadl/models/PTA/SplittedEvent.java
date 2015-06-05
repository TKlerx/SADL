package sadl.models.PTA;

import jsat.distributions.empirical.kernelfunc.GaussKF;

import org.apache.commons.lang3.Range;

public class SplittedEvent {

	protected Event event;
	protected int subEventNumber;
	protected Range<Double> intervallAnomaly;
	protected Range<Double> intervallWarning;
	protected double expectedValue;
	protected double variance;
	// TODO durch 2 teilen?
	private static double anomalyNormalPoint = pointWithIntegral(0.0001d);
	private static double warningNormalPoint = pointWithIntegral(0.1d);

	public SplittedEvent(Event event, int subEventNumber, double expectedValue, double variance, Range<Double> maxIntervall) {

		this.event = event;
		this.subEventNumber = subEventNumber;
		this.expectedValue = expectedValue;
		this.variance = variance;

		final double differenceAnomaly = (anomalyNormalPoint * variance);
		intervallAnomaly = Range.between(Math.max(expectedValue - differenceAnomaly, maxIntervall.getMinimum()),
				Math.min(expectedValue + differenceAnomaly, maxIntervall.getMaximum()));

		final double differenceWarning = (warningNormalPoint * variance);
		intervallWarning = Range.between(Math.max(expectedValue - differenceWarning, maxIntervall.getMinimum()),
				Math.min(expectedValue + differenceWarning, maxIntervall.getMaximum()));
	}

	public String getSymbol() {

		return event.getSymbol() + subEventNumber;
	}

	public boolean inIntervall(double time) {

		return intervallAnomaly.contains(time);
	}

	public boolean hasWarning(double time) {

		return !(intervallWarning.contains(time));
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof SplittedEvent)) {
			return false;
		}

		final SplittedEvent event = (SplittedEvent) obj;
		return event.getSymbol().equals(this.getSymbol());
	}

	public static void setAnomalyProbability(double p) {

		if (p < 0.0 || p >= 1.0) {
			throw new IllegalArgumentException("p is not between 0 and 1.");
		}

		anomalyNormalPoint = pointWithIntegral(p / 2);
	}

	public static void setWarningProbability(double p) {

		if (p < 0.0 || p >= 1.0) {
			throw new IllegalArgumentException("p is not between 0 and 1.");
		}

		warningNormalPoint = pointWithIntegral(p / 2);
	}

	// TODO move?
	public static double pointWithIntegral(double p) {

		final GaussKF normalFunc = GaussKF.getInstance();
		double start = -normalFunc.cutOff();
		double end = 0.0d;

		do {

			final double between = (start + end) / 2.0;
			if (normalFunc.intK(between) > p) {
				end = between;
			} else {
				start = between;
			}

			System.out.print("c ");

		} while ((end - start) > 0.0000000001d);

		return -(end + start) / 2.0;
	}
}
