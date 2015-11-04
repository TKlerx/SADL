package sadl.models.pdrtaModified;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDRTATransitionModified {

	protected SubEvent event;
	protected PDRTAStateModified target;
	protected Range<Double> interval;
	protected double propability;

	PDRTATransitionModified(SubEvent event, PDRTAStateModified target, Range<Double> intervall, double probability) {
		this.event = event;
		this.target = target;
		this.interval = intervall;
		this.propability = probability;

		if (event == null || target == null || intervall == null || Double.isNaN(probability)) {
			throw new IllegalArgumentException();
		}
	}

	public SubEvent getEvent() {

		return event;
	}

	public PDRTAStateModified getTarget() {

		return target;
	}

	public double getPropability() {

		return propability;
	}

	public boolean inInterval(double value) {
		final double min = interval.getMinimum();
		final double max = interval.getMaximum();

		if (min == value || (min < value && value < max)) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {

		return event.getSymbol() + "[" + interval.getMinimum() + "," + interval.getMaximum() + ")=>" + target.id;
	}
}
