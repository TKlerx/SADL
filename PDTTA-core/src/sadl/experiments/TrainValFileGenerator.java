/**
 * This file is part of SADL, a library for learning Probabilistic deterministic timed-transition Automata.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * 
 * @author Timo Klerx
 *
 */
public class TrainValFileGenerator {

	public static void main(String[] args) throws IOException {
		final String outputDir = "data";
		final Path outputPath = Paths.get(outputDir);
		if(!Files.exists(outputPath)){
			Files.createDirectory(outputPath);
		}
		final Path inputFile = Paths.get("rti_input.txt");
		final Random r = new Random();
		final BufferedWriter[] writers = new BufferedWriter[10];
		final BufferedReader br = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
		for(int i = 0;i<writers.length;i++){
			writers[i]=Files.newBufferedWriter(outputPath.resolve("dataset_"+i+".txt"), StandardCharsets.UTF_8);
		}
		String line = "";
		while((line = br.readLine())!=null){
			final int index = r.nextInt(10);
			writers[index].append(line);
			writers[index].append('\n');
		}
		for(int i = 0;i<writers.length;i++){
			writers[i].close();
		}
	}

}
