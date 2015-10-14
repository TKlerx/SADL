/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package sadl.run;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

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

	@Parameter(names = "-debug")
	boolean debug = false;

	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				logger.error("Not enough params!");
				System.exit(1);
			}

			// FIXME parse MasterSeed

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

			// TODO Debug param has to be in front of commands: JCommander specific
			if (main.debug) {
				Settings.setDebug(main.debug);
			}

			switch (jc.getParsedCommand()) {
			case test:
				testRun.run();
				break;
			case train:
				trainRun.run(jc.getCommands().get(train));
				break;
			case smac:
				smacRun.run(jc.getCommands().get(smac));
				break;
			default:
				// TODO Print usage
				logger.error("Wrong mode param!");
				System.exit(1);
				break;
			}
		} catch (final Exception e) {
				logger.error("Unexpected Exception!", e);
				throw e;
			}
	}

	private SADL() {
		// Disable initialization
	}

}
