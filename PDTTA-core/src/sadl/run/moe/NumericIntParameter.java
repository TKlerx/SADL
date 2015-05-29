package sadl.run.moe;

public class NumericIntParameter implements Parameter {
	private final int min;
	private final int max;
	private final int defValue;

	public NumericIntParameter(int min, int max, int defValue) {
		this.min = min;
		this.max = max;
		this.defValue = defValue;
	}
}
