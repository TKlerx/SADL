package sadl.models.pdrtaModified;

import org.apache.commons.lang3.Range;

import sadl.models.PTA.SubEvent;

public class PDRTATransitionModified {

	protected SubEvent event;
	protected PDRTAStateModified target;
	protected Range<Double> interval;
	protected double propability;

	PDRTATransitionModified(SubEvent event, PDRTAStateModified target, Range<Double> interval, double probability) {

		if (event == null) {
			throw new IllegalArgumentException("Event is empty");
		} else if (target == null) {
			throw new IllegalArgumentException("Target is empty");
		} else if (interval == null) {
			throw new IllegalArgumentException("Interval is empty");
		} else if (Double.isNaN(probability) || probability < 0.0d || probability > 1.0d) {
			throw new IllegalArgumentException("Probability wrong parameter: " + probability);
		}

		this.event = event;
		this.target = target;
		this.interval = interval;
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
