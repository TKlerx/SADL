package sadl.integration;

import java.util.Comparator;

public class MonteCarloPoint implements Comparable<MonteCarloPoint> {
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
}
