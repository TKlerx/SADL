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
package sadl.run.moe;

public class NumericDoubleParameter implements Parameter {

	private final double min;
	private final double max;
	private final double defValue;
	private final String name;

	public NumericDoubleParameter(String name, double min, double max, double defValue) {
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
		return Double.toString(max);
	}

	@Override
	public String getMin() {
		return Double.toString(min);
	}

	@Override
	public Double getDefault() {
		return defValue;
	}

}
