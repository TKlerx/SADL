package sadl.models.pta;


public class HalfClosedInterval<T extends Comparable<T>> implements Cloneable {
	protected T min;
	protected T max;

	public HalfClosedInterval(T min, T max) {

		if (min == null || max == null || min.compareTo(max) > 0) {
			throw new IllegalArgumentException();
		}

		this.min = min;
		this.max = max;
	}

	public T getMinimum() {

		return min;
	}

	public T getMaximum() {

		return max;
	}

	public void setMinimum(T newMin) {

		if (newMin == null || newMin.compareTo(max) > 0) {
			throw new IllegalArgumentException();
		}

		min = newMin;
	}

	public void setMaximum(T newMax) {

		if (newMax == null || newMax.compareTo(min) < 0) {
			throw new IllegalArgumentException();
		}

		max = newMax;
	}

	public boolean cutLeft(T value) {

		if (this.contains(value)) {

			min = value;
			return true;
		}

		return false;
	}

	public boolean cutRight(T value) {

		if (this.contains(value)) {

			max = value;
			return true;
		}

		return false;
	}

	public boolean contains(T value) {

		if (min.compareTo(value) == 0 || (min.compareTo(value) < 0 && value.compareTo(max) < 0)) {
			return true;
		}

		return false;
	}

	// true if interval is included in current interval
	public boolean contains(HalfClosedInterval<T> value) {
		final T valueMin = value.getMinimum();
		final T valueMax = value.getMaximum();

		if ((min.compareTo(valueMin) == 0 || (min.compareTo(valueMin) < 0) && (valueMax.compareTo(max) == 0 || valueMax.compareTo(max) < 0))) {
			return true;
		}

		return false;
	}

	public boolean intersects(HalfClosedInterval<T> value) {

		if (contains(value.getMinimum()) || value.contains(min) || (contains(value.getMaximum()) && !min.equals(value.getMaximum()))
				|| (value.contains(max) && !max.equals(value.getMinimum()))) {
			return true;
		}

		return false;
	}

	// returns the intersaction of two intervals
	public HalfClosedInterval<T> getIntersectionWith(HalfClosedInterval<T> value){

		if (!intersects(value)) {
			return null;
		}

		T minIntersection;
		T maxIntersection;

		final T minValue = value.getMinimum();
		final T maxValue = value.getMaximum();

		if (min.compareTo(minValue) > 0) {
			minIntersection = min;
		}else{
			minIntersection = minValue;
		}

		if (max.compareTo(maxValue) < 0) {
			maxIntersection = max;
		}else{
			maxIntersection = maxValue;
		}

		return new HalfClosedInterval<>(minIntersection, maxIntersection);
	}

	@Override
	public String toString() {

		if (min.equals(max)) {
			return "[" + min + ";" + max + "]";
		}

		return "[" + min + ";" + max + ")";
	}

	@Override
	public HalfClosedInterval<T> clone() {

		return new HalfClosedInterval<>(min, max);
	}

}
