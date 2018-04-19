/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2018  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.modellearner.rtiplus.tester;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * 
 * @author Fabian Witter
 *
 */
public class LikelihoodValue {

	protected TDoubleList ratios;
	protected TIntList additionalParams;

	public LikelihoodValue(double ratio, int additionalParam) {

		this();
		ratios.add(ratio);
		additionalParams.add(additionalParam);
	}

	public LikelihoodValue() {

		ratios = new TDoubleArrayList();
		additionalParams = new TIntArrayList();
	}

	public void add(LikelihoodValue lv) {

		ratios.addAll(lv.ratios);
		additionalParams.addAll(lv.additionalParams);
	}

	public double getRatio() {
		return ratios.sum();
	}

	public int getParam() {
		return additionalParams.sum();
	}

}
