/*******************************************************************************
 * This file is part of PDTTA, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  Timo Klerx
 * 
 * PDTTA is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * PDTTA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with PDTTA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.upb.timok.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.upb.timok.models.PDTTA;


public class TrebaToGraphviz {

	public static void transform(Path trebaPath, Path graphvizResult) throws IOException {
		PDTTA a = new PDTTA(trebaPath);
		a.toGraphvizFile(graphvizResult, false);
		// Runtime.getRuntime().exec("dot -Tpdf " + graphvizResult +
		// " -o graph.pdf");
		System.out.println("dot -Tpdf " + graphvizResult + " -o graph.pdf");
		// Runtime.getRuntime().exec("dot -Tpdf " + graphvizResult +
		// " -o graph.pdf");
	}



	public static void main(String[] args) throws IOException {
		Path trebaPath = Paths.get(args[0]);
		Path graphvizPath = Paths.get(args[1]);
		transform(trebaPath, graphvizPath);
	}
}
