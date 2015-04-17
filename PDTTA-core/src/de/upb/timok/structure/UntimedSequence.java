package de.upb.timok.structure;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.timok.constants.ClassLabel;

public class UntimedSequence implements Cloneable {

	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(UntimedSequence.class);

	 TIntList events = new TIntArrayList();
	private ClassLabel label = ClassLabel.NORMAL;



	public void setLabel(ClassLabel label) {
		this.label = label;
	}

	public ClassLabel getLabel() {
		return label;
	}

	public UntimedSequence(TIntList events, ClassLabel label) {
		super();
		this.events = events;
		this.label = label;
	}


	public UntimedSequence() {
		this(new TIntArrayList(), ClassLabel.NORMAL);
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

	public int getEvent(int index){
		return getEvents().get(index);
	}
	public TIntList getEvents() {
		return events;
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
			sb.append(-1);
			if (i != events.size() - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public String toLabeledString() {
		return toTrebaString() + ";" + label.getClassLabel();
	}

	@Override
	public String toString() {
		return this.toTrebaString();
	}

	public void remove(int toDelete) {
		events.remove(toDelete, 1);
	}


	/**
	 * 
	 * @param timedInputTrainFile
	 * @param isRti
	 *            set to true to skip first line
	 * @param containsClassLabels
	 * @return
	 * @throws IOException
	 */
	public static List<TimedSequence> parseTimedSequences(String timedInputTrainFile, boolean isRti, boolean containsClassLabels) throws IOException {
		final List<TimedSequence> result = new ArrayList<>();
		final BufferedReader br = Files.newBufferedReader(Paths.get(timedInputTrainFile), StandardCharsets.UTF_8);

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
	
	@Override
	public UntimedSequence clone() throws CloneNotSupportedException {
		return new UntimedSequence(new TIntArrayList(events.toArray()),label);
	}

	public void addEvent(int symbol) {
		events.add(symbol);
	}

}
