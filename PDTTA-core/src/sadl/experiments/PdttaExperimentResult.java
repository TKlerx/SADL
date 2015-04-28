/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package sadl.experiments;

import sadl.constants.AnomalyInsertionType;
import sadl.constants.MergeTest;
import sadl.constants.ProbabilityAggregationMethod;



public class PdttaExperimentResult {
	private double fsmLogLikelihood;

	public PdttaExperimentResult(int truePositives, int trueNegatives, int falsePositives, int falseNegatives) {
		super();
		this.truePositives = truePositives;
		this.trueNegatives = trueNegatives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
		precision = (double) truePositives / (truePositives + falsePositives);
		recall = (double) truePositives / (truePositives + falseNegatives);
		fMeasure = 2 * precision * recall / (precision + recall);
	}
	double precision;
	double recall;
	double fMeasure;
	int truePositives;
	int trueNegatives;
	int falsePositives;
	int falseNegatives;
	boolean recMergeTest;
	double mergeAlpha;
	AnomalyInsertionType anomalyInsertionType;

	public AnomalyInsertionType getAnomalyInsertionType() {
		return anomalyInsertionType;
	}

	public void setAnomalyInsertionType(AnomalyInsertionType anomalyInsertionType) {
		this.anomalyInsertionType = anomalyInsertionType;
	}


	@Override
	public String toString() {
		return "PdttaExperimentResult [fsmLogLikelihood=" + fsmLogLikelihood + ", precision=" + precision + ", recall=" + recall + ", fMeasure=" + fMeasure
				+ ", truePositives=" + truePositives + ", trueNegatives=" + trueNegatives + ", falsePositives=" + falsePositives + ", falseNegatives="
				+ falseNegatives + ", recMergeTest=" + recMergeTest + ", mergeAlpha=" + mergeAlpha + ", anomalyInsertionType=" + anomalyInsertionType
				+ ", mergeTest=" + mergeTest + ", timeTreshold=" + timeThreshold + ", eventTreshold=" + eventThreshold + ", aggType=" + aggType + "]";
	}
	MergeTest mergeTest;
	double timeThreshold;
	double eventThreshold;
	ProbabilityAggregationMethod aggType;

	public PdttaExperimentResult(double fsmLogLikelihood, double precision, double recall, double fMeasure, int truePositives, int trueNegatives,
			int falsePositives, int falseNegatives, boolean recMergeTest, double mergeAlpha, MergeTest mergeTest, double timeTreshold, double eventTreshold,
			ProbabilityAggregationMethod aggType, AnomalyInsertionType anomalyInsertionType) {
		this(truePositives, trueNegatives, falsePositives, falseNegatives);
		this.fsmLogLikelihood = fsmLogLikelihood;
		this.precision = precision;
		this.recall = recall;
		this.fMeasure = fMeasure;
		this.recMergeTest = recMergeTest;
		this.mergeAlpha = mergeAlpha;
		this.mergeTest = mergeTest;
		this.timeThreshold = timeTreshold;
		this.eventThreshold = eventTreshold;
		this.aggType = aggType;
		this.anomalyInsertionType = anomalyInsertionType;
	}

	public void setLogLikelihood(double loglikelihood) {
		this.fsmLogLikelihood = loglikelihood;

	}

	public double getFsmLogLikelihood() {
		return fsmLogLikelihood;
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getfMeasure() {
		if (Double.isNaN(fMeasure)) {
			return 0;
		}
		return fMeasure;
	}

	public int getTruePositives() {
		return truePositives;
	}

	public int getTrueNegatives() {
		return trueNegatives;
	}

	public int getFalsePositives() {
		return falsePositives;
	}

	public int getFalseNegatives() {
		return falseNegatives;
	}

	public boolean isRecMergeTest() {
		return recMergeTest;
	}

	public double getMergeAlpha() {
		return mergeAlpha;
	}

	public MergeTest getMergeTest() {
		return mergeTest;
	}

	public double getTimeTreshold() {
		return timeThreshold;
	}

	public double getEventTreshold() {
		return eventThreshold;
	}

	public ProbabilityAggregationMethod getAggType() {
		return aggType;
	}

	public void setRecMergeTest(boolean recMergeTest) {
		this.recMergeTest = recMergeTest;
	}

	public void setMergeAlpha(double mergeAlpha) {
		this.mergeAlpha = mergeAlpha;
	}

	public void setMergeTest(MergeTest mergeTest) {
		this.mergeTest = mergeTest;
	}

	public void setTimeThreshold(double timeThreshold) {
		this.timeThreshold = timeThreshold;
	}

	public void setEventThreshold(double eventThreshold) {
		this.eventThreshold = eventThreshold;
	}

	public void setAggType(ProbabilityAggregationMethod aggType) {
		this.aggType = aggType;
	}

	static String separator = ";";

	public static String CsvHeader() {
		return "fsmLogLikelihood" + separator + "precision" + separator + "recall" + separator + "fMeasure" + separator + "truePositives" + separator
				+ "trueNegatives" + separator + "falsePositives" + separator + "falseNegatives" + separator + "recMergeTest" + separator + "mergeAlpha"
				+ separator + "anomalyInsertionType" + separator + "mergeTest" + separator + "timeTreshold" + separator + "eventTreshold" + separator
				+ "aggType";
	}
	public String toCsvString() {
		return fsmLogLikelihood + separator + precision + separator + recall + separator + fMeasure + separator + truePositives + separator + trueNegatives
				+ separator + falsePositives + separator + falseNegatives + separator + recMergeTest + separator + mergeAlpha + separator
				+ anomalyInsertionType + separator + mergeTest + separator + timeThreshold + separator + eventThreshold + separator + aggType;
	}

}
