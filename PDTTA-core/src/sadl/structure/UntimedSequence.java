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

package sadl.structure;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sadl.constants.ClassLabel;

/**
 * 
 * @author Timo Klerx
 *
 */
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((events == null) ? 0 : events.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		final UntimedSequence other = (UntimedSequence) obj;
		if (events == null) {
			if (other.events != null) {
				return false;
			}
		} else if (!events.equals(other.events)) {
			return false;
		}
		if (label != other.label) {
			return false;
		}
		return true;
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


	@Override
	public UntimedSequence clone() throws CloneNotSupportedException {
		return new UntimedSequence(new TIntArrayList(events.toArray()),label);
	}

	public void addEvent(int symbol) {
		events.add(symbol);
	}

}
