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
package sadl.run.data_analyzers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;

import sadl.utils.IoUtils;

public class RecursiveBargraphPlot {
	static String plotExecutable = "/home/timo/bargraph/bargraph.pl";

	public static void main(String[] args) throws IOException, InterruptedException {
		plot(Paths.get(args[0]));
	}

	public static void plot(Path inputDir) throws IOException, InterruptedException {

		final List<Path> plotFiles = IoUtils.listFiles(inputDir, ".perf", true);

		final ExecutorService es = Executors.newSingleThreadExecutor();
		int i = 1;
		for (final Path p : plotFiles) {
			final String[] command = new String[] { plotExecutable, "-pdf", p.toString() };
			System.out.println(Arrays.toString(command));
			final ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(false);
			Process proc;

			proc = pb.start();
			final Path file = Paths.get(FilenameUtils.removeExtension(p.toString()) + ".pdf");
			Files.deleteIfExists(file);
			es.submit(() -> {
				try (InputStream is = proc.getInputStream();
						OutputStream fw = Files.newOutputStream(file)) {
					int k = 0;
					while ((k = is.read()) != -1) {
						fw.write(k);
					}
					fw.flush();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			final int exitVal = proc.waitFor();
			System.out.println("exitVal for file " + p + " = " + exitVal + " (" + i + "/" + plotFiles.size() + ")");
			i++;
		}
		es.shutdown();
	}
}
