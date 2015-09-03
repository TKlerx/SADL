package sadl.models.pdrtaModified;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDRTATransitionModified {

	protected SubEvent event;
	protected PDRTAStateModified target;
	protected Range<Double> intervall;
	protected double propability;

	PDRTATransitionModified(SubEvent event, PDRTAStateModified target, Range<Double> intervall, double probability) {
		this.event = event;
		this.target = target;
		this.intervall = intervall;
		this.propability = probability;
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

	public boolean inIntervall(double value) {
		final double min = intervall.getMinimum();
		final double max = intervall.getMaximum();

		if (min == value) {
			return true;
		}

		if (min < value && value < max) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {

		return event.getSymbol() + "[" + intervall.getMinimum() + "," + intervall.getMaximum() + ")=>" + target.id;
	}
}
