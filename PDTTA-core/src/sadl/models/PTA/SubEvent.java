package sadl.models.PTA;

import jsat.distributions.empirical.GaussKFInvertible;

import org.apache.commons.lang3.Range;

public class SubEvent {

	protected Event event;
	protected int subEventNumber;
	protected Range<Double> anomalyIntervall;
	protected Range<Double> warningIntervall;
	protected Range<Double> maxIntervall;
	protected double expectedValue;
	protected double variance;

	protected SubEvent previousSubEvent;
	protected SubEvent nextSubEvent;

	private static double anomalyNormalPoint = GaussKFInvertible.InvertedIntGaussKF(0.000001d, 0.0000001d);
	private static double warningNormalPoint = GaussKFInvertible.InvertedIntGaussKF(0.1d, 0.0000001d);;

	public SubEvent(Event event, int subEventNumber, double expectedValue, double variance, Range<Double> maxIntervall) {

		this.event = event;
		this.subEventNumber = subEventNumber;
		this.expectedValue = expectedValue;
		this.variance = variance;

		final double differenceAnomaly = (anomalyNormalPoint * variance);
		anomalyIntervall = Range.between(Math.max(0, expectedValue - differenceAnomaly), expectedValue + differenceAnomaly);

		final double differenceWarning = (warningNormalPoint * variance);
		warningIntervall = Range.between(expectedValue - differenceWarning, expectedValue + differenceWarning);

		this.maxIntervall = maxIntervall;
	}

	public String getSymbol() {

		return event.getSymbol() + subEventNumber;
	}

	public boolean isAnomaly(double time) {
		final double leftBound = anomalyIntervall.getMinimum();
		final double rightBound = anomalyIntervall.getMaximum();

		if (leftBound == time || (leftBound < time && time < rightBound)) {
			return false;
		}

		return true;
	}

	public boolean isInCriticalArea(double time) {

		if (this.isAnomaly(time) || isInBounds(time)) {
			return false;
		}

		return true;
	}

	public boolean isInBounds(double time) {
		final double leftBound = this.getLeftBound();
		final double rightBound = this.getRightBound();

		if (leftBound == time || (leftBound < time && time < rightBound)) {
			return true;
		}

		return false;
	}

	public double getLeftBound() {

		return Math.max(anomalyIntervall.getMinimum(), maxIntervall.getMinimum());
	}

	public double getRightBound() {
		return Math.min(anomalyIntervall.getMaximum(), maxIntervall.getMaximum());
	}

	public double getLeftBoundInState(PTAState state) {

		if (this.hasLeftCriticalArea() && state.outTransitions.containsKey(this.getPreviousSubEvent().getSymbol())) {
			return this.getLeftBound();
		}

		return this.anomalyIntervall.getMinimum();

	}

	public double getRightBoundInState(PTAState state) {

		if (this.hasRightCriticalArea() && state.outTransitions.containsKey(this.getNextSubEvent().getSymbol())) {
			return this.getRightBound();
		}

		return this.anomalyIntervall.getMaximum();
	}

	public Range<Double> getIntervallInState(PTAState state) {

		return Range.between(getLeftBoundInState(state), getRightBoundInState(state));
	}

	public boolean hasLeftCriticalArea() {

		if (anomalyIntervall.getMinimum() < maxIntervall.getMinimum()) {
			return true;
		}

		return false;
	}

	public boolean hasRightCriticalArea() {

		if (maxIntervall.getMaximum() < anomalyIntervall.getMaximum()) {
			return true;
		}

		return false;
	}

	public boolean hasWarning(double time) {
		final double leftBound = warningIntervall.getMinimum();
		final double rightBound = warningIntervall.getMaximum();

		if (leftBound == time || (leftBound < time && time < rightBound)) {
			return true;
		}

		return true;
	}

	public SubEvent getPreviousSubEvent() {

		return previousSubEvent;
	}

	public SubEvent getNextSubEvent() {

		return nextSubEvent;
	}

	public Event getEvent() {
		return this.event;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null || !(obj instanceof SubEvent)) {
			return false;
		}

		final SubEvent e = (SubEvent) obj;
		return e.getSymbol().equals(this.getSymbol());
	}

	public static void setAnomalyProbability(double p) {

		if (p <= 0.0 || p >= 1.0) {
			throw new IllegalArgumentException("p is not between 0 and 1.");
		}

		anomalyNormalPoint = GaussKFInvertible.InvertedIntGaussKF(p / 2, 0.0000001d);
	}

	public static void setWarningProbability(double p) {

		if (p <= 0.0 || p >= 1.0) {
			throw new IllegalArgumentException("p is not between 0 and 1.");
		}

		warningNormalPoint = GaussKFInvertible.InvertedIntGaussKF(p / 2, 0.0000001d);
	}

	@Override
	public String toString() {

		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(this.getSymbol() + "[" + anomalyIntervall.getMinimum() + "[" + this.getLeftBound() + " " + this.getRightBound() + ")"
				+ anomalyIntervall.getMaximum() + ")");

		return stringBuilder.toString();
	}

}
