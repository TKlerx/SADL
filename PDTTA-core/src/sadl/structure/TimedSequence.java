/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.AnomalyInsertionType;
import sadl.constants.ClassLabel;

/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public class TimedSequence {
	private static Logger logger = LoggerFactory.getLogger(TimedSequence.class);

	private TIntList events = new TIntArrayList();
	private TDoubleList timeValues = new TDoubleArrayList();
	private ClassLabel label = ClassLabel.NORMAL;
	@SuppressWarnings("unused")
	private AnomalyInsertionType anomalyType = AnomalyInsertionType.NONE;

	public void setTimeValues(TDoubleList timeValues) {
		this.timeValues = timeValues;
	}

	public void setLabel(ClassLabel label) {
		this.label = label;
	}

	public ClassLabel getLabel() {
		return label;
	}

	public TimedSequence(TIntList events, TDoubleList timeValues, ClassLabel label) {
		this(events, timeValues,label, label == ClassLabel.ANOMALY? AnomalyInsertionType.ALL:AnomalyInsertionType.NONE);
	}

	public TimedSequence(TIntList events, TDoubleList timeValues, ClassLabel label, AnomalyInsertionType anomalyType) {
		super();
		this.events = events;
		this.timeValues = timeValues;
		this.label = label;
		this.anomalyType = anomalyType;
		checkConsistency();
	}

	public TimedSequence(String line) {
		this(line, false, false);
	}

	public int length(){
		return getEvents().size();
	}

	// public TimedSequence(String line, boolean isRti, boolean containsClassLabels) {
	// this(line, isRti, containsClassLabels, false);
	// }

	public static List<String> trimSplit(String inputString) {
		final List<String> result = new ArrayList<>();
		final String[] split = inputString.split("\\s");
		String temp;
		for (final String s : split) {
			temp = s.trim();
			if (!temp.equals("")) {
				result.add(temp);
			}
		}
		return result;
	}

	public TimedSequence(String line, boolean isRti, boolean containsClassLabels) {
		String newLine = line;
		if (containsClassLabels) {
			if (line.contains(";")) {
				final String[] split = newLine.split(";");
				newLine = split[0];
				switch (split[1]) {
				case "0":
					label = ClassLabel.NORMAL;
					break;
				case "1":
					label = ClassLabel.ANOMALY;
					break;
				default:
					label = ClassLabel.NORMAL;
					break;
				}
			}
		}
		// tuples are (event, timeValue)
		final String[] split = newLine.split("\\s+");
		int start = 0;
		if (isRti) {
			start = 1;
		}
		for (int i = start; i < split.length - start; i += 2) {
			events.add(Integer.parseInt(split[i]));
			timeValues.add(Double.parseDouble(split[i + 1]));
		}
		checkConsistency();

	}
	public int getEvent(int index){
		return getEvents().get(index);
	}
	public TIntList getEvents() {
		return events;
	}

	public TDoubleList getTimeValues() {
		return timeValues;
	}

	public double getTimeValue(int index){
		return getTimeValues().get(index);
	}

	public String getEventString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < events.size(); i++) {
			sb.append(events.get(i));
			if (i != events.size() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public String toTrebaString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < events.size(); i++) {
			sb.append(events.get(i));
			sb.append(' ');
			sb.append(timeValues.get(i));
			if (i != events.size() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public String toLabeledString() {
		return toTrebaString() + ":" + label.getClassLabel();
	}

	@Override
	public String toString() {
		return this.toTrebaString();
	}

	public void remove(int toDelete) {
		events.remove(toDelete, 1);
		timeValues.remove(toDelete, 1);

		checkConsistency();
	}

	private void checkConsistency() {
		if (events.size() != timeValues.size()) {
			logger.error("events and timevalues do not have the same cardinality.");
		}
		// if (events.size() == 0 || timeValues.size() == 0) {
		// logger.warn("Creating TimedSequence with 0 length");
		// }
	}

	/**
	 * 
	 * @param timedInputTrainFile
	 * @param isRti
	 *            set to true to skip first line
	 * @param containsClassLabels
	 * @throws IOException
	 */
	public static List<TimedSequence> parseTimedSequences(String timedInputTrainFile, boolean isRti, boolean containsClassLabels) throws IOException {
		final List<TimedSequence> result = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(timedInputTrainFile), StandardCharsets.UTF_8)) {

			String line = null;
			if (isRti) {
				// skip info with alphabet size
				br.readLine();
			}
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					result.add(new TimedSequence(line, isRti, containsClassLabels));
				}
			}
			return result;
		}
	}
}
