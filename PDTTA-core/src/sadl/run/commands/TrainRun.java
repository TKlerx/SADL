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
package sadl.run.commands;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sadl.constants.Algoname;
import sadl.input.TimedInput;
import sadl.interfaces.ProbabilisticModel;
import sadl.interfaces.ProbabilisticModelLearner;
import sadl.models.pdrta.PDRTA;
import sadl.run.factories.LearnerFactory;
import sadl.run.factories.learn.ButlaFactory;
import sadl.run.factories.learn.PdttaFactory;
import sadl.run.factories.learn.RTIFactory;
import sadl.utils.IoUtils;

@Parameters(commandDescription = "Performs a training of a model from an input")
public class TrainRun {

	private static final Logger logger = LoggerFactory.getLogger(TrainRun.class);

	private final boolean smacMode;

	@Parameter
	private List<String> mainParams;

	@Parameter(names = "-in", arity = 1)
	private Path in;
	TimedInput trainSeqs;

	@Parameter(names = "-out", arity = 1)
	Path out = Paths.get("sadl_train_out.model");

	public TrainRun(boolean smacMode) {
		this.smacMode = smacMode;
	}

	public ProbabilisticModel run(JCommander jc) {

		LearnerFactory lf = null;

		if (mainParams.size() != 1) {
			logger.error("Parameter error: There must only be a single main parameter containing the algorithm name!");
			System.exit(1);
		}

		final Algoname algoName = Algoname.getAlgoname(mainParams.get(0));

		switch (algoName) {
			case RTI:
				lf = new RTIFactory();
				break;
			case PDTTA:
				lf = new PdttaFactory();
				break;
			case BUTLA:
				lf = new ButlaFactory();
				break;
				// TODO Add other learning algorithms
			default:
				logger.error("Unknown algo param {}!", algoName);
				SmacRun.smacErrorAbort();
				break;
		}

		final JCommander subjc = new JCommander(lf);
		subjc.parse(jc.getUnknownOptions().toArray(new String[0]));

		@SuppressWarnings("null")
		final ProbabilisticModelLearner ml = lf.create();

		if (!smacMode) {
			try {
				trainSeqs = TimedInput.parse(in);
				// trainSeqs = IoUtils.readTrainTestFile(in).getFirst();
			} catch (final IOException e) {
				logger.error("Error when reading training sequences from file!", e);
			}
		}

		final ProbabilisticModel m = ml.train(trainSeqs);

		if (!smacMode) {
			try {
				final Path parent = out.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
					if (m instanceof PDRTA) {
						// TODO Remove this some time
						final Path out2 = Paths.get(out.toAbsolutePath().toString() + ".gv");
						try (final BufferedWriter bw = Files.newBufferedWriter(out2)) {
							((PDRTA) m).toDOTLang(bw);
						}
						final String[] args = { "dot", "-Tpng", out2.toAbsolutePath().toString(), "-o", out.toAbsolutePath().toString() + ".png" };
						Process pr = null;
						try {
							pr = Runtime.getRuntime().exec(args);
						} catch (final Exception e) {
							logger.warn("Could not create plot of PDRTA: {}", e.getMessage());
						} finally {
							if (pr != null) {
								try {
									pr.waitFor();
								} catch (final InterruptedException e) {
								}
							}
						}
					}
					IoUtils.serialize(m, out);
				}
			} catch (final IOException e) {
				logger.error("Error when storing model in file!", e);
			}
		}

		return m;
	}

}
