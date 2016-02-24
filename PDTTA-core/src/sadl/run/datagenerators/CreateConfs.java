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
package sadl.run.datagenerators;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import sadl.constants.Algoname;
import sadl.utils.IoUtils;

public class CreateConfs {
	private static int uniqueId = 0;
	public static void main(String[] args) throws IOException {
		createConfFiles(Paths.get(args[0]), Paths.get("conf-template.txt"), Paths.get(args[1]));
	}

	public static void createConfFiles(Path dataDir, Path confTemplate, Path confDirInput) throws IOException {
		final Path confDir = confDirInput.resolve("smac-confs");
		IoUtils.cleanDir(confDirInput);
		final List<String> templateLines = Files.readAllLines(confTemplate);
		Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				final String dirName = dir.getFileName().toString();
				if (!dir.equals(dataDir) && !dirName.equals("train") && !dirName.equals("test")) {
					if (Files.notExists(dir.resolve("train"))) {
						return FileVisitResult.CONTINUE;
					}
					final Path absDataDir = dataDir.toAbsolutePath();
					final Path absDir = dir.toAbsolutePath();
					final Path difference = absDataDir.relativize(absDir);
					final Path targetDir = confDir.resolve(difference);
					for (final Algoname algo : Algoname.values()) {
						Files.createDirectories(targetDir);
						final Path targetFile = targetDir.resolve(algo.toString().toLowerCase() + "-id=" + (uniqueId++) + ".txt");
						final List<String> lines = new ArrayList<>(templateLines);
						for (int i = 0; i < lines.size(); i++) {
							String line = lines.get(i);
							line = line.replaceAll("\\$algoname", Matcher.quoteReplacement(algo.name().toLowerCase()));
							line = line.replaceAll("\\$trainFolder", Matcher.quoteReplacement(difference.resolve("train").toString())).replaceAll("\\\\", "/");
							line = line.replaceAll("\\$testFolder", Matcher.quoteReplacement(difference.resolve("test").toString())).replaceAll("\\\\", "/");
							int algoRuntime = 0;
							if (difference.toString().contains("real")) {
								algoRuntime = 604800;
							} else {
								algoRuntime = 3600;
							}
							line = line.replaceAll("\\$runtime", Integer.toString(algoRuntime));
							lines.set(i, line);
						}
						Files.write(targetFile, lines);
					}
				}
				return FileVisitResult.CONTINUE;
			};
		});
	}
}
