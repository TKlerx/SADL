package sadl.integration;

import java.io.Serializable;
import java.util.Comparator;

public class MonteCarloPoint implements Comparable<MonteCarloPoint>, Serializable {
	private static final long serialVersionUID = -9038714832199550481L;
	double x;
	double pdfValue;
	int sampleCount;

	@Override
	public String toString() {
		return "MonteCarloPoint [x=" + x + ", pdfValue=" + pdfValue + ", sampleCount=" + sampleCount + "]";
	}

	public MonteCarloPoint(double x, double pdfValue) {
		super();
		this.x = x;
		this.pdfValue = pdfValue;
		sampleCount = 0;
	}

	public MonteCarloPoint(double x, double pdfValue, int sampleCount) {
		super();
		this.x = x;
		this.pdfValue = pdfValue;
		this.sampleCount = sampleCount;
	}

	public void incSampleCount() {
		sampleCount++;
	}

	public double getPdfValue() {
		return pdfValue;
	}

	public double getX() {
		return x;
	}

	@Override
	public int compareTo(MonteCarloPoint arg0) {
		return Double.compare(pdfValue, arg0.pdfValue);
	}
	static class MonteCarloPointComparator implements Comparator<MonteCarloPoint>{

		@Override
		public int compare(MonteCarloPoint arg0, MonteCarloPoint arg1) {
			return Double.compare(arg0.pdfValue, arg1.pdfValue);
		}

	}

	public int getSampleCount() {
		return sampleCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(pdfValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + sampleCount;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MonteCarloPoint other = (MonteCarloPoint) obj;
		if (Double.doubleToLongBits(pdfValue) != Double.doubleToLongBits(other.pdfValue)) {
			return false;
		}
		if (sampleCount != other.sampleCount) {
			return false;
		}
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) {
			return false;
		}
		return true;
	}

}
