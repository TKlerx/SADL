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
package de.upb.timok.run;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import de.upb.timok.constants.AnomalyInsertionType;
import de.upb.timok.models.TauPTA;
import de.upb.timok.structure.TimedSequence;
import de.upb.timok.utils.MasterSeed;

public class DataGenerator implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(DataGenerator.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -6230657726489919272L;

	// just for parsing the one silly smac parameter
	@Parameter()
	private final List<String> rest = new ArrayList<>();

	String dataString;


	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final DataGenerator sp = new DataGenerator();
		new JCommander(sp, args);
		sp.dataString = args[0];
		logger.info("Running DataGenerator with args" + Arrays.toString(args));
		MasterSeed.setSeed(1234);
		sp.run();
	}

	private void run() throws IOException, InterruptedException {
		// parse timed sequences
		final List<TimedSequence> trainingTimedSequences = TimedSequence.parseTimedSequences(dataString, true, false);

		final TauPTA pta = new TauPTA(trainingTimedSequences);
		@SuppressWarnings("unused")
		final TimedSequence s1 = pta.sampleSequence();
		@SuppressWarnings("unused")
		final TimedSequence s2 = pta.sampleSequence();
		// logger.info("{}", s1);
		// logger.info("{}", s2);
		// pta.toGraphvizFile(Paths.get("pta.gv"), false);
		// IoUtils.xmlSerialize(pta, Paths.get("pta.xml"));
		// logger.info("{}", pta.getStates(0));
		// logger.info("{}", pta.getStates(1));
		// logger.info("{}", pta.getStates(2));
		// logger.info("{}", pta.getStates(3));
		// logger.info("{}", pta.getStates(4));
		final TauPTA anomaly1 = SerializationUtils.clone(pta);
		anomaly1.setAnomalyType(AnomalyInsertionType.TYPE_ONE);
		for (int i = 0; i < 1000; i++) {
			anomaly1.sampleSequence();
		}
		final TauPTA anomaly2 = SerializationUtils.clone(pta);
		anomaly2.setAnomalyType(AnomalyInsertionType.TYPE_TWO);

		final TauPTA anomaly3 = SerializationUtils.clone(pta);
		anomaly3.setAnomalyType(AnomalyInsertionType.TYPE_THREE);

		final TauPTA anomaly4 = SerializationUtils.clone(pta);
		anomaly4.setAnomalyType(AnomalyInsertionType.TYPE_FOUR);

		// TODO for smac change the input format s.t. it contains unlabeled train and labeled test set

		// also create type 5: sequence stops
		// insert final state probabilities in states that do not have final state probabilities


	}












}
