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
package sadl.interfaces;

import sadl.input.TimedInput;
import sadl.input.TimedWord;

/**
 * 
 * @author Timo Klerx
 *
 */
@Deprecated
public interface AnomalyDetectorUnused {
	boolean isAnomaly(ProbabilisticModel model, TimedWord s);

	/**
	 * 0 indicates normal sequence, 1 indicates abnormal sequence
	 * 
	 * @param testSequences
	 */
	boolean[] areAnomalies(ProbabilisticModel model, TimedInput testSequences);

	boolean isAnomaly(TimedWord s);

	/**
	 * 0 indicates normal sequence, 1 indicates abnormal sequence
	 * 
	 * @param testSequences
	 */
	boolean[] areAnomalies(TimedInput testSequences);

	void setModel(ProbabilisticModel model);
}
