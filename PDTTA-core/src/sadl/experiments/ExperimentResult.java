/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.experiments;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * 
 * @author Timo Klerx
 *
 */
public class ExperimentResult {

	public void setExecutionTimeTraining(long executionTimeTraining) {
		this.executionTimeTraining = executionTimeTraining;
	}

	public void setExecutionTimeTesting(long executionTimeTesting) {
		this.executionTimeTesting = executionTimeTesting;
	}

	public ExperimentResult(long truePositives, long trueNegatives, long falsePositives, long falseNegatives) {
		super();
		this.truePositives = truePositives;
		this.trueNegatives = trueNegatives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
	}

	public ExperimentResult(long truePositives, long trueNegatives, long falsePositives, long falseNegatives, long executionTimeTraining,
			long executionTimeTesting, int numberOfStates) {
		super();
		this.truePositives = truePositives;
		this.trueNegatives = trueNegatives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
		this.executionTimeTraining = executionTimeTraining;
		this.executionTimeTesting = executionTimeTesting;
		this.numberOfStates = numberOfStates;
	}

	long truePositives;
	long trueNegatives;
	long falsePositives;
	long falseNegatives;
	long executionTimeTraining = 0;
	long executionTimeTesting = 0;
	int numberOfStates = 0;
	int maxMemoryUsage = 0;
	int minMemoryUsage = 0;
	double avgMemoryUsage = 0;

	public int getMaxMemoryUsage() {
		return maxMemoryUsage;
	}

	public int getMinMemoryUsage() {
		return minMemoryUsage;
	}

	public double getAvgMemoryUsage() {
		return avgMemoryUsage;
	}

	public void setNumberOfStates(int numberOfStates) {
		this.numberOfStates = numberOfStates;
	}

	public void setMaxMemoryUsage(int maxMemoryUsage) {
		this.maxMemoryUsage = maxMemoryUsage;
	}

	public void setMinMemoryUsage(int minMemoryUsage) {
		this.minMemoryUsage = minMemoryUsage;
	}

	public void setAvgMemoryUsage(double avgMemoryUsage) {
		this.avgMemoryUsage = avgMemoryUsage;
	}

	public long getExecutionTimeTraining() {
		return executionTimeTraining;
	}

	public long getExecutionTimeTesting() {
		return executionTimeTesting;
	}

	public int getNumberOfStates() {
		return numberOfStates;
	}

	public double getPrecision() {
		return truePositives / (double) (truePositives + falsePositives);
	}

	public double getRecall() {
		return truePositives / (double) (truePositives + falseNegatives);
	}

	public double getFMeasure() {
		final double result = 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
		if (Double.isNaN(result)) {
			return 0;
		}
		return result;
	}

	public long getTruePositives() {
		return truePositives;
	}

	public long getTrueNegatives() {
		return trueNegatives;
	}

	public long getFalsePositives() {
		return falsePositives;
	}

	public long getFalseNegatives() {
		return falseNegatives;
	}

	static String separator = ";";

	public static String CsvHeader() {

		return "trainTime" + separator + "testTime" + separator + "minRam" + separator + "maxRam" + separator + "AvgRam" + separator + "numberOfStates"
				+ separator + "truePositives" + separator + "falsePositives" + separator + "trueNegatives" + separator + "falseNegatives" + separator
				+ "precision" + separator
				+ "recall" + separator + "accuracy" + separator + "FMeasure" + separator + "PhiCoefficient";
	}
	public String toCsvString() {
		return DurationFormatUtils.formatDurationHMS(executionTimeTraining) + "(" + executionTimeTraining + ")" + separator
				+ DurationFormatUtils.formatDurationHMS(executionTimeTesting) + "(" + executionTimeTesting + ")" + separator + minMemoryUsage + separator
				+ maxMemoryUsage + separator + avgMemoryUsage + separator + numberOfStates + separator + truePositives + separator
				+ falsePositives + separator + trueNegatives + separator + falseNegatives + separator + getPrecision() + separator
				+ getRecall() + separator + getAccuracy() + separator + getFMeasure() + separator + getPhiCoefficient();
	}

	public double getPhiCoefficient() {
		final double numerator = (truePositives * trueNegatives) - (falsePositives * falseNegatives);
		final double denominator = Math.sqrt(
				(truePositives + falsePositives) * (truePositives + falseNegatives) * (trueNegatives + falsePositives) * (trueNegatives + falseNegatives));
		return numerator / denominator;
	}

	public double getAccuracy() {
		return (truePositives + trueNegatives) / (double) (truePositives + trueNegatives + falsePositives + falseNegatives);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (falseNegatives ^ (falseNegatives >>> 32));
		result = prime * result + (int) (falsePositives ^ (falsePositives >>> 32));
		result = prime * result + (int) (trueNegatives ^ (trueNegatives >>> 32));
		result = prime * result + (int) (truePositives ^ (truePositives >>> 32));
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
		final ExperimentResult other = (ExperimentResult) obj;
		if (falseNegatives != other.falseNegatives) {
			return false;
		}
		if (falsePositives != other.falsePositives) {
			return false;
		}
		if (trueNegatives != other.trueNegatives) {
			return false;
		}
		if (truePositives != other.truePositives) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ExperimentResult [truePositives=" + truePositives + ", trueNegatives=" + trueNegatives + ", falsePositives=" + falsePositives
				+ ", falseNegatives=" + falseNegatives + ", executionTimeTraining=" + DurationFormatUtils.formatDurationHMS(executionTimeTraining)
				+ ", executionTimeTesting=" + DurationFormatUtils.formatDurationHMS(executionTimeTesting) + ", numberOfStates=" + numberOfStates
				+ ", maxMemoryUsage=" + maxMemoryUsage + "(MB), minMemoryUsage=" + minMemoryUsage + "(MB), avgMemoryUsage=" + avgMemoryUsage + "(MB)]";
	}

	public ExperimentResult combine(ExperimentResult other) {
		final ExperimentResult result = new ExperimentResult(this.truePositives + other.truePositives, this.trueNegatives + other.trueNegatives,
				this.falsePositives + other.falsePositives, this.falseNegatives + other.falseNegatives);
		result.setMaxMemoryUsage(Math.max(this.getMaxMemoryUsage(), other.getMaxMemoryUsage()));
		result.setMinMemoryUsage(Math.min(this.getMinMemoryUsage(), other.getMinMemoryUsage()));
		result.setExecutionTimeTraining(this.getExecutionTimeTraining() + other.getExecutionTimeTraining());
		result.setExecutionTimeTesting(this.getExecutionTimeTesting() + other.getExecutionTimeTesting());
		result.setNumberOfStates(Math.max(this.getNumberOfStates(), other.getNumberOfStates()));
		return result;
	}


}
