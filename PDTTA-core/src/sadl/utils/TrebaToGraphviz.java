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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import sadl.models.PDFA;

/**
 * 
 * @author Timo Klerx
 *
 */
public class TrebaToGraphviz {

	public static void transform(Path trebaPath, Path graphvizResult) throws IOException {
		final PDFA a = new PDFA(trebaPath);
		a.toGraphvizFile(graphvizResult, false);
		// Runtime.getRuntime().exec("dot -Tpdf " + graphvizResult +
		// " -o graph.pdf");
		System.out.println("dot -Tpdf " + graphvizResult + " -o graph.pdf");
		// Runtime.getRuntime().exec("dot -Tpdf " + graphvizResult +
		// " -o graph.pdf");
	}



	public static void main(String[] args) throws IOException {
		final Path trebaPath = Paths.get(args[0]);
		final Path graphvizPath = Paths.get(args[1]);
		transform(trebaPath, graphvizPath);
	}
}
