package sadl.integration;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.map.TDoubleIntMap;
import gnu.trove.map.TDoubleObjectMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import jsat.distributions.ContinuousDistribution;
import sadl.integration.MonteCarloPoint.MonteCarloPointComparator;
import sadl.utils.MasterSeed;

public class MonteCarlo {
	int resolutionSteps;
	int pointsToStore;
	MonteCarloPoint[] integral2;
	TDoubleIntMap integral = new TDoubleIntHashMap();

	public MonteCarlo(int numberOfSteps, int pointsToStore) {
		this.resolutionSteps = numberOfSteps;
		this.pointsToStore = pointsToStore;
	}

	public void preprocess(ContinuousDistribution d, double xMin, double xMax) {
		if (Double.isInfinite(xMin)) {
			xMin = Double.MIN_VALUE;
		}
		if (Double.isInfinite(xMax)) {
			xMax = Double.MAX_VALUE;
		}
		final Pair<Double, Double> minMax = findExtreme(d, xMin, xMax);
		final double yMin = minMax.getLeft();
		final double yMax = minMax.getRight();
		final double xDiff = xMax - xMin;
		final double yDiff = yMax - yMin;
		final Random xRandom = MasterSeed.nextRandom();
		final Random yRandom = MasterSeed.nextRandom();
		int pointsFound = 0;
		final TDoubleObjectMap<MonteCarloPoint> tempMap = new TDoubleObjectHashMap<>();
		int pointsRejected = 0;
		while (pointsFound < pointsToStore) {
			final double xSampled = xMin + (xDiff * xRandom.nextDouble());
			final double ySampled = yMin + (yDiff * yRandom.nextDouble());
			final double pdfValue = d.pdf(xSampled);
			if (pdfValue > 0 && ySampled <= pdfValue) {
				// store the point because the sampled y value is smaller than the pdf value at the x value
				MonteCarloPoint point = tempMap.get(xSampled);
				if(point == null){
					point = new MonteCarloPoint(xSampled, pdfValue);
					tempMap.put(xSampled, point);
				}
				point.incSampleCount();
				integral.adjustOrPutValue(xSampled, 1, 1);
				pointsFound++;
			} else {
				pointsRejected++;
			}
		}
		System.out.println("Rejected " + pointsRejected + " points");
		integral2 = new MonteCarloPoint[tempMap.size()];
		int i = 0;
		for (final MonteCarloPoint mcp : tempMap.valueCollection()) {
			integral2[i] = mcp;
			i++;
		}
		Arrays.sort(integral2, new MonteCarloPointComparator());
		// Collections.sort(integral2);
		// integral2.sort((m1, m2) -> Double.compare(m1.getX(), m2.getX()));

	}

	public void preprocess(ContinuousDistribution d) {
		preprocess(d, d.min(), d.max());
	}

	/**
	 * Computes the proportion of the pdf where the pdf value of the function is smaller than the given value
	 * 
	 * @param pdfValue
	 * @return the proportion of the area with smaller pdf value
	 */
	public double integrate(double pdfValue) {
		int foundIndex = Arrays.binarySearch(integral2, new MonteCarloPoint(0, pdfValue));
		if (foundIndex < 0) {
			foundIndex++;
			foundIndex *= -1;
			foundIndex--;
		}
		System.out.println("FoundIndex=" + foundIndex);
		if (foundIndex - 1 >= 0) {
			System.out.println("Pdf value one index before=" + integral2[foundIndex - 1].getPdfValue());
		}
		System.out.println("Pdf value to look for=" + pdfValue);
		System.out.println("Pdf value at index=" + integral2[foundIndex].getPdfValue());
		if (foundIndex + 1 < integral2.length) {
			System.out.println("Pdf value one index after=" + integral2[foundIndex + 1].getPdfValue());
		}
		int numberOfPoints = 0;
		for (int i = 0; i < foundIndex; i++) {
			numberOfPoints += integral2[i].getSampleCount();
		}
		System.out.println("number of Points found=" + numberOfPoints);
		return numberOfPoints / (double) pointsToStore;
	}
	private Pair<Double, Double> findExtreme(ContinuousDistribution d, double xMin, double xMax) {
		final double stepSize = (xMax - xMin) / resolutionSteps;
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		for(double i = xMin; i<=xMax; i+=stepSize){
			final double pdfValue = d.pdf(i);
			yMin = Math.min(yMin, pdfValue);
			yMax = Math.max(yMax, pdfValue);
		}
		return Pair.of(yMin, yMax);
	}
}