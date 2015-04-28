package sadl.input;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.ClassLabel;

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
