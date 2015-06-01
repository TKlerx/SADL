package sadl.run.moe;

public class NumericDoubleParameter implements Parameter {

	private final double min;
	private final double max;
	private final double defValue;
	private final String name;

	public NumericDoubleParameter(String name, double min, double max, double defValue) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.defValue = defValue;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getMax() {
		return Double.toString(max);
	}

	@Override
	public String getMin() {
		return Double.toString(min);
	}

	@Override
	public Double getDefault() {
		return defValue;
	}

}
