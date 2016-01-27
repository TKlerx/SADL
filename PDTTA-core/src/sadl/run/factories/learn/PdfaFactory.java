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
package sadl.run.factories.learn;

import com.beust.jcommander.Parameter;

import sadl.constants.MergeMethod;

public class PdfaFactory implements PdfaDefaultFactory {

	@Parameter(names = "-mergeAlpha")
	double mergeAlpha = 0.05;

	@Parameter(names = "-recursiveMergeTest", arity = 1)
	boolean recursiveMergeTest = true;

	@Parameter(names = "-mergeT0")
	int mergeT0 = 3;

	@Parameter(names = "-mergeMethod")
	MergeMethod mergeMethod = MergeMethod.ALERGIA_PAPER;

	@Override
	public double getMergeAlpha() {
		return mergeAlpha;
	}

	@Override
	public boolean isRecursiveMergeTest() {
		return recursiveMergeTest;
	}

	@Override
	public int getMergeT0() {
		return mergeT0;
	}

	@Override
	public MergeMethod getMergeMethod() {
		return mergeMethod;
	}

}
