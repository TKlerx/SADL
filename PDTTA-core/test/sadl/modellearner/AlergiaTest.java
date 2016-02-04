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
package sadl.modellearner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import sadl.anomalydetecion.AnomalyDetection;
import sadl.constants.MergeMethod;
import sadl.constants.MergeTest;
import sadl.constants.ProbabilityAggregationMethod;
import sadl.detectors.AnomalyDetector;
import sadl.detectors.VectorDetector;
import sadl.detectors.featureCreators.AggregatedSingleFeatureCreator;
import sadl.experiments.ExperimentResult;
import sadl.input.TimedInput;
import sadl.input.TimedWord;
import sadl.models.PDFA;
import sadl.oneclassclassifier.ThresholdClassifier;
import sadl.structure.Transition;
import sadl.utils.IoUtils;
import sadl.utils.MasterSeed;
import utils.LibraryChecker;

public class AlergiaTest {
	private static Logger logger = LoggerFactory.getLogger(AlergiaTest.class);

	private PDFA generatePdfaBig() {
		final TimedInput alphabet = new TimedInput(new String[] { "a", "b", "c" });
		final Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(0, 1, "a", 0.2));
		transitions.add(new Transition(0, 2, "b", 0.8));
		transitions.add(new Transition(1, 2, "a", 0.3));
		transitions.add(new Transition(2, 1, "b", 0.5));
		transitions.add(new Transition(1, 3, "c", 0.7));
		transitions.add(new Transition(2, 3, "c", 0.5));
		transitions.add(new Transition(3, 3, "b", 0.5));
		transitions.add(new Transition(3, 0, "a", 0.3));

		final TIntDoubleMap finalStates = new TIntDoubleHashMap();
		finalStates.put(3, 0.2);
		final PDFA pdfa = new PDFA(alphabet, transitions, finalStates, null);
		return pdfa;
	}

	private PDFA generatePdfaSmall() {

		final TimedInput alphabet = new TimedInput(new String[] { "a", "b" });
		final Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(0, 1, "a", 0.2));
		transitions.add(new Transition(0, 0, "b", 0.2));
		transitions.add(new Transition(1, 1, "a", 0.5));
		transitions.add(new Transition(1, 0, "b", 0.5));

		final TIntDoubleMap finalStates = new TIntDoubleHashMap();
		finalStates.put(0, 0.6);
		finalStates.put(1, 0);
		final PDFA pdfa = new PDFA(alphabet, transitions, finalStates, null);
		return pdfa;
	}

	@Before
	public void setUp() throws Exception {
		MasterSeed.reset();
	}

	@Test
	public void testArtificialBig() throws IOException {
		final PDFA a = generatePdfaBig();
		final List<TimedWord> words = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			words.add(a.sampleSequence());
		}
		final TimedInput trainAlergia = new TimedInput(words);
		final TimedInput trainTreba = SerializationUtils.clone(trainAlergia);

		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.05, true, MergeMethod.ALERGIA_PAPER);
		final PDFA pdfaAlergia = alergia.train(trainAlergia);

		assertEquals(4, pdfaAlergia.getStateCount());

		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux") && LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, true, MergeTest.ALERGIA, 0.0, 0);
			final PDFA pdfaTreba = treba.train(trainTreba);
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(120, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testArtificialSmall() throws IOException {
		final PDFA a = generatePdfaSmall();
		final List<TimedWord> words = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			words.add(a.sampleSequence());
		}
		final TimedInput trainAlergia = new TimedInput(words);
		final TimedInput trainTreba = SerializationUtils.clone(trainAlergia);

		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.8, false, MergeMethod.ALERGIA_PAPER);
		final PDFA pdfaAlergia = alergia.train(trainAlergia);

		assertEquals(3, pdfaAlergia.getStateCount());

		final String osName = System.getProperty("os.name");
		if (osName.toLowerCase().contains("linux") && LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.8, false, MergeTest.ALERGIA, 0.0, 0);
			final PDFA pdfaTreba = treba.train(trainTreba);
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(3, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testLoopBigNonRecPaperMerge() throws URISyntaxException, IOException {
		final Alergia alergia = new Alergia(0.05, false, MergeMethod.ALERGIA_PAPER);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 4531, 2, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(808, pdfa.getStateCount());
	}

	@Test
	public void testLoopBigNonRecTrebaMerge() throws URISyntaxException, IOException {
		final Alergia alergia = new Alergia(0.05, false, MergeMethod.TREBA);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 4531, 2, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(808, pdfa.getStateCount());
	}

	@Test
	public void testLoopBigRecPaperMerge() throws URISyntaxException, IOException {
		final Alergia alergia = new Alergia(0.05, true, MergeMethod.ALERGIA_PAPER);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 4525, 8, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(882, pdfa.getStateCount());
	}

	@Test
	public void testLoopBigRecTrebaMerge() throws URISyntaxException, IOException {
		final Alergia alergia = new Alergia(0.05, true, MergeMethod.TREBA);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(467, 4528, 5, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(808, pdfa.getStateCount());
	}

	@Test
	public void testRbBigNonRecPaperMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.05, false, MergeMethod.ALERGIA_PAPER);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils
				.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfaAlergia);
		final ExperimentResult expected = new ExperimentResult(0, 4533, 0, 467);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(2, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbBigNonRecTrebaMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.05, false, MergeMethod.TREBA);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfa = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfa);
		final ExperimentResult expected = new ExperimentResult(0, 4533, 0, 467);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(2, pdfa.getStateCount());

	}

	@Test
	public void testRbBigRecPaperMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.05, true, MergeMethod.ALERGIA_PAPER);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfaAlergia);
		final ExperimentResult alergiaEexpected = new ExperimentResult(467, 4532, 1, 0);
		final ExperimentResult alergiaActual = detection.test(trainTest.getValue());
		assertEquals(alergiaEexpected, alergiaActual);
		assertEquals(108, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbBigRecTrebaMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.05, true, MergeMethod.TREBA);
		final Pair<TimedInput, TimedInput> trainTest = IoUtils.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainTest.getKey());
		final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
				new ThresholdClassifier(Math.exp(-5)));
		final AnomalyDetection detection = new AnomalyDetection(detector, pdfaAlergia);
		final ExperimentResult expected = new ExperimentResult(467, 4532, 1, 0);
		final ExperimentResult actual = detection.test(trainTest.getValue());
		assertEquals(expected, actual);
		assertEquals(70, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbSmallNonRecPaperMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.8, false, MergeMethod.ALERGIA_PAPER);
		final TimedInput trainAlergia = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainAlergia);
		assertEquals(2, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbSmallNonRecTrebaMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.8, false, MergeMethod.TREBA);
		final TimedInput trainAlergia = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainAlergia);
		assertEquals(2, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbSmallRecPaperMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.8, true, MergeMethod.ALERGIA_PAPER);
		final TimedInput trainAlergia = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainAlergia);
		assertEquals(2, pdfaAlergia.getStateCount());
	}

	@Test
	public void testRbSmallRecTrebaMerge() throws URISyntaxException, IOException {
		final AlergiaRedBlue alergia = new AlergiaRedBlue(0.8, true, MergeMethod.TREBA);
		final TimedInput trainAlergia = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
		final PDFA pdfaAlergia = alergia.train(trainAlergia);
		assertEquals(2, pdfaAlergia.getStateCount());
	}

	@Test
	public void testTrebaBigNonRec() throws URISyntaxException, IOException {
		if (LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, false, MergeTest.ALERGIA, 0.0, 0);
			final Pair<TimedInput, TimedInput> trainTest = IoUtils
					.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
			final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
					new ThresholdClassifier(Math.exp(-5)));
			final PDFA pdfaTreba = treba.train(trainTest.getKey());
			final AnomalyDetection trebaDetection = new AnomalyDetection(detector, pdfaTreba);
			final ExperimentResult trebaExpected = new ExperimentResult(463, 4533, 0, 4);
			final ExperimentResult trebaActual = trebaDetection.test(trainTest.getValue());
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(trebaExpected, trebaActual);
			assertEquals(42, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testTrebaBigRec() throws URISyntaxException, IOException {
		if (LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.05, true, MergeTest.ALERGIA, 0.0, 0);
			final Pair<TimedInput, TimedInput> trainTest = IoUtils
					.readTrainTestFile(Paths.get(this.getClass().getResource("/pdtta/smac_mix_type1.txt").toURI()));
			final AnomalyDetector detector = new VectorDetector(ProbabilityAggregationMethod.NORMALIZED_MULTIPLY, new AggregatedSingleFeatureCreator(),
					new ThresholdClassifier(Math.exp(-5)));
			final PDFA pdfaTreba = treba.train(trainTest.getKey());
			final AnomalyDetection trebaDetection = new AnomalyDetection(detector, pdfaTreba);
			final ExperimentResult trebaExpected = new ExperimentResult(467, 4525, 8, 0);
			final ExperimentResult trebaActual = trebaDetection.test(trainTest.getValue());
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(trebaExpected, trebaActual);
			assertEquals(356, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testTrebaSmallNonRec() throws URISyntaxException, IOException {
		if (LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.8, false, MergeTest.ALERGIA, 0.0, 0);
			final TimedInput trainTreba = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
			final PDFA pdfaTreba = treba.train(trainTreba);
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(2, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

	@Test
	public void testTrebaSmallRec() throws URISyntaxException, IOException {
		if (LibraryChecker.trebaDepsInstalled()) {
			final TrebaPdfaLearner treba = new TrebaPdfaLearner(0.8, true, MergeTest.ALERGIA, 0.0, 0);
			final TimedInput trainTreba = TimedInput.parse(Paths.get(this.getClass().getResource("/pdfa/alergia_0.inp").toURI()));
			final PDFA pdfaTreba = treba.train(trainTreba);
			logger.info("Treba PDFA has {} states.", pdfaTreba.getStateCount());
			assertEquals(3, pdfaTreba.getStateCount());
		} else {
			System.out.println("Did not do any test because OS is not linux and treba cannot be loaded.");
		}
	}

}
