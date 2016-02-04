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
package sadl.constants;

import treba.trebaConstants;
import treba.trebaJNI;

/**
 * 
 * @author Timo Klerx
 *
 */
public enum MergeTest {
	ALERGIA, CHI_SQUARED, LR, BINOMIAL, EXACT_M, EXACT_B, MDI;


	public int getAlgorithm() {
		if (trebaJNI.isLibraryLoaded()) {
			if (this == MergeTest.ALERGIA) {
				return trebaConstants.MERGE_TEST_ALERGIA;
			} else if (this == MergeTest.CHI_SQUARED) {
				return trebaConstants.MERGE_TEST_CHISQUARED;
			} else if (this == MergeTest.LR) {
				return trebaConstants.MERGE_TEST_LR;
			} else if (this == MergeTest.BINOMIAL) {
				return trebaConstants.MERGE_TEST_BINOMIAL;
			} else if (this == MergeTest.EXACT_M) {
				return trebaConstants.MERGE_TEST_EXACT_M;
			} else if (this == MergeTest.EXACT_B) {
				return trebaConstants.MERGE_TEST_EXACT_B;
			} else if (this == MergeTest.MDI) {
				return -1;
			} else {
				throw new IllegalStateException("Unknown Mergetest " + this);
			}
		} else {
			throw new IllegalStateException("Do not use this method if treba is not loaded.");
		}
	}
}
