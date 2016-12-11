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
package sadl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author Timo Klerx
 *
 */
public class CollectionUtils {
	public static <K, V> void putOrCreateList(Map<K, List<V>> map, K key, V value) {
		List<V> list;
		list = map.get(key);
		if (list == null) {
			list = new ArrayList<>();
			list.add(value);
			map.put(key, list);
		} else {
			list.add(value);
		}
	}

	public static <T> T chooseRandomObject(List<T> list, Random rnd) {
		return list.get(rnd.nextInt(list.size()));
	}
}
