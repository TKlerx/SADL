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

package sadl.run.moe;

public class NumericIntParameter implements Parameter {
	private final int min;
	private final int max;
	private final int defValue;
	private final String name;

	public NumericIntParameter(String name, int min, int max, int defValue) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.defValue = defValue;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getMax() {
		return Integer.toString(max);
	}

	@Override
	public String getMin() {
		return Integer.toString(min);
	}

	@Override
	public Double getDefault() {
		return (double) defValue;
	}
}
