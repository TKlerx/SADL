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

package sadl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Timo Klerx
 *
 */
public class MapUtils {
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
}
