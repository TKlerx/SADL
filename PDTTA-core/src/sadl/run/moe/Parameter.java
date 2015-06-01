package sadl.run.moe;

public interface Parameter extends Comparable<Parameter> {
	@Override
	public default int compareTo(Parameter o) {
		return this.getName().compareTo(o.getName());
	}

	public String getName();

	public String getMax();

	public String getMin();

	public Double getDefault();
}
