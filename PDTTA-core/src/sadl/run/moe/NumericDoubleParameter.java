package sadl.run.moe;

public class NumericDoubleParameter implements Parameter {

	private final double min;
	private final double max;
	private final double defValue;

	public NumericDoubleParameter(double min, double max, double defValue) {
		this.min = min;
		this.max = max;
		this.defValue = defValue;
	}

}
