package sadl.run.moe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HistoryData implements Iterable<Configuration> {

	Map<Configuration, Double> history = new HashMap<>();

	@Override
	public Iterator<Configuration> iterator() {
		return history.keySet().iterator();
	}

}
