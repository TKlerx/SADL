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
package sadl.modellearner.rtiplus;

/**
 * 
 * @author Fabian Witter
 *
 */
@Deprecated
public class ProgressBarPrinter {

	private final int max;
	private int counter;

	private final int barLength = 20;
	private int numBarParts;

	public ProgressBarPrinter(int max) {

		this.max = max;
		this.counter = 0;
		this.numBarParts = 0;
		if (max > 0) {
			System.out.print("[");
		}
	}

	public void inc() {

		if (counter < max) {
			counter++;
			final double p = (double) counter / (double) max;
			final int x = (int) Math.rint(p * barLength);
			if (x > numBarParts) {
				for (int i = numBarParts; i < x; i++) {
					System.out.print("#");
				}
				numBarParts = x;
			}
			if (counter == max) {
				System.out.print("]");
			}
		}
		assert (numBarParts <= barLength);
	}

}
