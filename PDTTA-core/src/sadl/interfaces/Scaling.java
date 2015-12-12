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

package sadl.interfaces;

import java.util.List;

public interface Scaling {

	void setFeatureCount(int length);

	/**
	 * Gathers scaling factors to scale vectors.
	 * Also scales the given vectors
	 * @param input the vectors to train with
	 * @return the scaled vectors
	 */
	public List<double[]> train(List<double[]> input);

	/**
	 * Scales a list of vectors with the values from the training phase.
	 * train must have been called before.
	 */
	public List<double[]> scale(List<double[]> input);
}
