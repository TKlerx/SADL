package sadl.models.PTA;

import jsat.distributions.empirical.GaussKFInvertible;

import org.apache.commons.lang3.Range;

public class SubEvent {

	protected Event event;
	protected int subEventNumber;
	protected Range<Double> anomalyInterval;
	protected Range<Double> warningInterval;
	protected Range<Double> maxInterval;
	protected double expectedValue;
	protected double variance;

	protected SubEvent previousSubEvent;
	protected SubEvent nextSubEvent;

	private static double anomalyNormalPoint = GaussKFInvertible.InvertedIntGaussKF(0.000001d, 0.0000001d);
	private static double warningNormalPoint = GaussKFInvertible.InvertedIntGaussKF(0.1d, 0.0000001d);;

	public SubEvent(Event event, int subEventNumber, double expectedValue, double variance, Range<Double> maxInterval) {

		this.event = event;
		this.subEventNumber = subEventNumber;
		this.expectedValue = expectedValue;
		this.variance = variance;

		final double differenceAnomaly = (anomalyNormalPoint * variance);
		anomalyInterval = Range.between(Math.max(0, expectedValue - differenceAnomaly), expectedValue + differenceAnomaly);

		final double differenceWarning = (warningNormalPoint * variance);
		warningInterval = Range.between(expectedValue - differenceWarning, expectedValue + differenceWarning);

		this.maxInterval = maxInterval;
	}

	public String getSymbol() {

		return event.getSymbol() + subEventNumber;
	}

	public boolean isAnomaly(double time) {

		double left;
		final double right;

		if (this.hasLeftCriticalArea()) {
			left = this.getLeftBound();
		} else {
			left = this.anomalyInterval.getMinimum();
		}

		if (this.hasRightCriticalArea()) {
			right = this.getRightBound();
		} else {
			right = this.anomalyInterval.getMaximum();
		}

		if (left == time || (left < time && time < right)) {
			return false;
		}

		return true;
	}

	public boolean isInCriticalArea(double time) {

		if (this.isAnomaly(time)) {
			return false;
		}

		if (this.hasLeftCriticalArea() && time < this.getLeftBound()) {
			return true;
		}

		if (this.hasRightCriticalArea() && this.getRightBound() >= time) {
			return true;
		}

		return false;
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

		// return Math.max(anomalyIntervall.getMinimum(), maxIntervall.getMinimum());
		return maxInterval.getMinimum();
	}

	public double getRightBound() {

		// return Math.min(anomalyIntervall.getMaximum(), maxIntervall.getMaximum());
		return maxInterval.getMaximum();
	}

	public double getLeftIntervalInState(PTAState state) {

		if (this.hasLeftCriticalArea() && state.outTransitions.containsKey(this.getPreviousSubEvent().getSymbol())) {
			return this.getLeftBound();
		}

		return this.anomalyInterval.getMinimum();

	}

	public double getLeftInterval() {

		if (this.hasLeftCriticalArea()) {
			return this.getLeftBound();
		}

		return this.anomalyInterval.getMinimum();

	}

	public double getRightIntervalInState(PTAState state) {

		if (this.hasRightCriticalArea() && state.outTransitions.containsKey(this.getNextSubEvent().getSymbol())) {
			return this.getRightBound();
		}

		return this.anomalyInterval.getMaximum();
	}

	public double getRightInterval() {

		if (this.hasRightCriticalArea()) {
			return this.getRightBound();
		}

		return this.anomalyInterval.getMaximum();
	}

	public Range<Double> getIntervalInState(PTAState state) {

		return Range.between(getLeftIntervalInState(state), getRightIntervalInState(state));
	}

	public Range<Double> getInterval() {

		return Range.between(getLeftInterval(), getRightInterval());
	}

	public boolean hasLeftCriticalArea() {

		if (anomalyInterval.getMinimum() < maxInterval.getMinimum()) {
			return true;
		}

		return false;
	}

	public boolean hasRightCriticalArea() {

		if (maxInterval.getMaximum() < anomalyInterval.getMaximum()) {
			return true;
		}

		return false;
	}

	public boolean hasWarning(double time) {
		final double leftBound = warningInterval.getMinimum();
		final double rightBound = warningInterval.getMaximum();

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

	public double generateRandomTime(boolean allowAnomaly) {

		final GaussKFInvertible func = GaussKFInvertible.getInstance();
		double randomTime = 0.0d;
		boolean condition;

		do {

			randomTime = func.getRandom(expectedValue, variance, 0.000001);

			if (!allowAnomaly && this.isAnomaly(randomTime)) {
				condition = false;
			} else {
				condition = true;
			}

		} while (randomTime <= 0.0d && !condition);

		return randomTime;
	}

	public double calculateProbability(double time) {

		return GaussKFInvertible.getInstance().k((time - expectedValue) / variance); // TODO check
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
		stringBuilder.append(this.getSymbol() + "[" + anomalyInterval.getMinimum() + "[" + this.getLeftBound() + " " + this.getRightBound() + ")"
				+ anomalyInterval.getMaximum() + ")");

		return stringBuilder.toString();
	}

}
