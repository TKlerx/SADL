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
package de.upb.timok.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class TrainValFileGenerator {

	public static void main(String[] args) throws IOException {
		String outputDir = "data";
		Path outputPath = Paths.get(outputDir);
		if(!Files.exists(outputPath)){
			Files.createDirectory(outputPath);
		}
		Path inputFile = Paths.get("rti_input.txt");
		Random r = new Random();
		BufferedWriter[] writers = new BufferedWriter[10];
		BufferedReader br = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
		for(int i = 0;i<writers.length;i++){
			writers[i]=Files.newBufferedWriter(outputPath.resolve("dataset_"+i+".txt"), StandardCharsets.UTF_8);
		}
		String line = "";
		while((line = br.readLine())!=null){
			int index = r.nextInt(10);
			writers[index].append(line);
			writers[index].append('\n');
		}
		for(int i = 0;i<writers.length;i++){
			writers[i].close();
		}
	}

}
