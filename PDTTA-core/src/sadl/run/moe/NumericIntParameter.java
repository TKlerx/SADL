package sadl.run.moe;

public class NumericIntParameter implements Parameter {
	private final int min;
	private final int max;
	private final int defValue;
	private final String name;

	public NumericIntParameter(String name, int min, int max, int defValue) {
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
		return Integer.toString(max);
	}

	@Override
	public String getMin() {
		return Integer.toString(min);
	}

	@Override
	public Double getDefault() {
		return (double) defValue;
	}
}
