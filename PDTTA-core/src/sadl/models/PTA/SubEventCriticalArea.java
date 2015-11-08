package sadl.models.PTA;

import org.apache.commons.lang3.Range;

public class SubEventCriticalArea extends SubEvent {

	double enterProbability;
	int almostSurelyCount;

	public SubEventCriticalArea(Event event, int subEventNumber, double expectedValue, double deviation, Range<Double> boundInterval,
			Range<Double> anomalyInterval, Range<Double> warningInterval, double enterProbability) {
		super(event, subEventNumber, expectedValue, deviation, boundInterval, anomalyInterval, warningInterval);

		this.enterProbability = enterProbability;
		almostSurelyCount = (int) (Math.log(0.9999999999d) / Math.log(enterProbability));
	}

	public double getEnterProbability() {

		return enterProbability;
	}

	public int getAlmostSurelyCount() {

		return almostSurelyCount;
	}
}
