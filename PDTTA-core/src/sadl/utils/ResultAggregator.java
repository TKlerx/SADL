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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import sadl.experiments.ExperimentResult;

/**
 * 
 * @author Timo Klerx
 *
 */
public class ResultAggregator {

	public static void main(String[] args) throws IOException {
		final Path resultFolder = Paths.get(args[0]);
		final String fileType = args[1];
		final Path resultFile = Paths.get(args[2]);
		try (final DirectoryStream<Path> ds = Files.newDirectoryStream(resultFolder, "*." + fileType);
				BufferedWriter bw = Files.newBufferedWriter(resultFile, StandardCharsets.UTF_8)){
			bw.write(ExperimentResult.CsvHeader());
			bw.append('\n');
			String line = null;
			for (final Path f : ds) {
				if (!f.equals(resultFile)) {
					try (BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
						while ((line = br.readLine()) != null) {
							bw.write(line);
							bw.append('\n');
						}
					}
				}
			}
		}
	}
}
