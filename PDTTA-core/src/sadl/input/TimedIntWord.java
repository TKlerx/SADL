/**
 * This file is part of SADL, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.input;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.ClassLabel;

/**
 * 
 * @author Timo Klerx
 *
 */
public class TimedIntWord extends TimedWord {
	TIntList intSymbols = new TIntArrayList();
	public TimedIntWord() {
		super();
		intSymbols = super.getIntSymbols();
		symbols = null;
	}

	public TimedIntWord(ClassLabel l) {
		super(l);
		intSymbols = super.getIntSymbols();
		symbols = null;
	}

	public TimedIntWord(TimedWord timedWord) {
		this(timedWord.transformToIntList(), timedWord.getTimeValues(), timedWord.getLabel());
	}

	public TimedIntWord(TIntList events, TIntList timeValues, ClassLabel label) {
		super(label);
		this.intSymbols = events;
		this.timeValues = timeValues;
		symbols = null;
	}


	@Override
	public int getIntSymbol(int i) {
		return intSymbols.get(i);
	}

	@Override
	public TIntList getIntSymbols() {
		return intSymbols;
	}

	@Override
	public int length() {
		return intSymbols.size();
	}

	@Override
	public int getLength() {
		return length();
	}

	@Override
	@Deprecated
	public String getSymbol(int i) {
		return Integer.toString(intSymbols.get(i));
	}

}
