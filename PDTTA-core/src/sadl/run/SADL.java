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
package sadl.run;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import sadl.experiments.ExperimentResult;
import sadl.run.commands.SmacRun;
import sadl.run.commands.TestRun;
import sadl.run.commands.TrainRun;
import sadl.utils.Settings;

/**
 * 
 * @author Fabian Witter
 *
 */
public class SADL {

	private static final Logger logger = LoggerFactory.getLogger(SADL.class);

	private static final String test = "test";
	private static final String train = "train";
	private static final String smac = "smac";

	@Parameter
	private final List<String> mainParams = new ArrayList<>();
	@Parameter(names = "-parallel", arity = 1)
	boolean parallel = true;
	@Parameter(names = "-debug")
	boolean debug = false;
	static boolean crash = false;
	public static void main(String[] args) throws Exception {

		try {
			if (args.length < 1) {
				logger.error("Not enough params!");
				System.exit(1);
			}

			// final String[] reducedArgs = Arrays.copyOfRange(args, 1, args.length);

			final SADL main = new SADL();
			final JCommander jc = new JCommander(main);
			jc.setAcceptUnknownOptions(true);

			final TestRun testRun = new TestRun(false);
			final TrainRun trainRun = new TrainRun(false);
			final SmacRun smacRun = new SmacRun();

			jc.addCommand(test, testRun);
			jc.addCommand(train, trainRun);
			jc.addCommand(smac, smacRun);

			jc.parse(args);

			// Debug/parallel param has to be in front of commands: JCommander specific
			Settings.setDebug(main.debug);
			Settings.setParallel(main.parallel);

			switch (jc.getParsedCommand()) {
				case test:
					testRun.run();
					break;
				case train:
					trainRun.run(jc.getCommands().get(train));
					break;
				case smac:
					logger.info("Starting SMAC with params=" + Arrays.toString(args));
					boolean fileExisted = true;
					final ExperimentResult result = smacRun.run(jc.getCommands().get(smac));
					logger.info("Finished SMAC run.");
					Path p = Paths.get(result.getQualifier()).getParent().getParent();
					final Path smacData = Paths.get("smac-data");
					String fileName = result.getAlgorithm() + "-";
					while (!p.getFileName().equals(smacData)) {
						fileName += p.getFileName() + "-";
						p = p.getParent();
					}
					fileName += "result.csv";
					final Path resultPath = Paths.get("results").resolve(fileName);
					Files.createDirectories(resultPath.getParent());
					if (!Files.exists(resultPath)) {
						Files.createFile(resultPath);
						fileExisted = false;
					}
					final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

					try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
						if (!fileExisted) {
							bw.append("time");
							bw.append(" ; ");
							bw.append("arg array");
							bw.append(" ; ");
							bw.append(ExperimentResult.CsvHeader());
							bw.append('\n');
						}
						bw.append(df.format(new Date()));
						bw.append(" ; ");
						bw.append(Arrays.toString(args));
						bw.append("; ");
						bw.append(result.toCsvString());
						bw.append('\n');
					}
					break;
				default:
					// TODO Print usage
					jc.usage();
					logger.error("Wrong mode param!");
					System.exit(1);
					break;
			}
		} catch (final Throwable e) {
			logger.error("Unexpected exception with parameters" + Arrays.toString(args), e);
			e.printStackTrace();
			crash = true;
			Thread.sleep(1000);
		} finally {
			System.exit(crash ? 1 : 0);
		}
	}

	private SADL() {
		// Disable initialization
	}

}
