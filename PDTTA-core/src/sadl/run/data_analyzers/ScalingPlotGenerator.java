package sadl.run.data_analyzers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;

import com.opencsv.CSVReader;

import sadl.run.datagenerators.ScalingDataGenerator;

public class ScalingPlotGenerator {

	static String[] SCALING_QUALIFIERS = { "data-time-transitions", "data-event-transitions", "data-states", "data-inc-samples", "data-alphabet" };
	static String[] SCALING_LEGEND = { "#time transitions", "#event transitions", "#states", "#samples", "#alphabet" };
	static Map<String, Integer> legendMap = new HashMap<>();

	private static void initLegend() {
		legendMap.put("data-time-transitions0", ScalingDataGenerator.INITIAL_TIME_BASED_TRANSITION_SIZE);
		legendMap.put("data-event-transitions0", ScalingDataGenerator.INITIAL_TRANSITION_SIZE);
		legendMap.put("data-states0", ScalingDataGenerator.INITIAL_STATE_SIZE);
		legendMap.put("data-inc-samples0", ScalingDataGenerator.INITIAL_SAMPLES);
		legendMap.put("data-alphabet0", ScalingDataGenerator.INITIAL_ALPHABET_SIZE);

		{
			final double scalingStepSize = (double) (ScalingDataGenerator.MAX_SAMPLES - ScalingDataGenerator.INITIAL_SAMPLES)
					/ (ScalingDataGenerator.SCALING_STEPS - 1);
			for (int i = 1; i <= 9; i++) {
				legendMap.put("data-inc-samples" + i, (int) (ScalingDataGenerator.INITIAL_SAMPLES + i * scalingStepSize));
			}
		}

		{
			final double eventStepSize = (double) (ScalingDataGenerator.MAX_EVENT_BASED_TRANSITION_SIZE - ScalingDataGenerator.INITIAL_TRANSITION_SIZE)
					/ (ScalingDataGenerator.SCALING_STEPS - 1);
			for (int i = 1; i <= 9; i++) {
				legendMap.put("data-event-transitions" + i, (int) (ScalingDataGenerator.INITIAL_TRANSITION_SIZE + i * eventStepSize));
			}
		}
		{
			final double timeStepSize = (double) (ScalingDataGenerator.MAX_TIME_BASED_TRANSITION_SIZE - ScalingDataGenerator.INITIAL_TIME_BASED_TRANSITION_SIZE)
					/ (ScalingDataGenerator.SCALING_STEPS - 1);
			for (int i = 1; i <= 9; i++) {
				legendMap.put("data-time-transitions" + i, (int) (ScalingDataGenerator.INITIAL_TIME_BASED_TRANSITION_SIZE + i * timeStepSize));
			}
		}
		{
			final double alphabetStepSize = (double) (ScalingDataGenerator.MAX_ALPHABET_SIZE - ScalingDataGenerator.INITIAL_ALPHABET_SIZE)
					/ (ScalingDataGenerator.SCALING_STEPS - 1);
			for (int i = 1; i <= 9; i++) {
				legendMap.put("data-alphabet" + i, (int) (ScalingDataGenerator.INITIAL_ALPHABET_SIZE + i * alphabetStepSize));
			}
		}
		{
			final double stateStepSize = (double) (ScalingDataGenerator.MAX_STATE_SIZE - ScalingDataGenerator.INITIAL_STATE_SIZE)
					/ (ScalingDataGenerator.SCALING_STEPS - 1);
			for (int i = 1; i <= 9; i++) {
				legendMap.put("data-states" + i, (int) (ScalingDataGenerator.INITIAL_STATE_SIZE + i * stateStepSize));
			}
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		run(Paths.get(args[0]), Paths.get(args[1]));

	}

	public static void run(Path inputFile, Path outputFolder) throws IOException, InterruptedException {
		initLegend();
		final List<String> runtimeTemplate = Files.readAllLines(outputFolder.resolve("runtime-template.gp"));
		final List<String> memoryTemplate = Files.readAllLines(outputFolder.resolve("memory-template.gp"));

		// Algoname, data file -> Count , (train time [minutes],avgRam)
		final Map<Pair<String, String>, Pair<Integer, Pair<Double, Double>>> data = new HashMap<>();
		// use train time (column index 3) and average ram (column index 7)
		try (final CSVReader reader = new CSVReader(Files.newBufferedReader(inputFile), ';')) {
			final String[] headline = reader.readNext();
			if (!headline[3].trim().equalsIgnoreCase("trainTime") || !headline[7].trim().equalsIgnoreCase("avgram")) {
				System.err.println("headline is not valid: " + Arrays.toString(headline));
			}
			String[] line = null;

			while ((line = reader.readNext()) != null) {
				final String algoName = line[0];
				final int numberOfRuns = (int) Double.parseDouble(line[1]);
				final String argArray = line[2];
				final double trainTime = Double.parseDouble(line[3]);
				final double ram = Double.parseDouble(line[7]);
				final String dataFileQualifier = argArray.split("/")[2];
				final Pair<String, String> qualifier = Pair.create(algoName, dataFileQualifier);
				final Pair<Integer, Pair<Double, Double>> currentEntry = data.get(qualifier);
				if (currentEntry == null || currentEntry.getKey() < numberOfRuns) {
					data.put(qualifier, Pair.create(numberOfRuns, Pair.create(trainTime, ram)));
				}
			}
			final Map<String, Double> memory = new HashMap<>();
			final Map<String, Double> runtime = new HashMap<>();
			for (final Entry<Pair<String, String>, Pair<Integer, Pair<Double, Double>>> e : data.entrySet()) {
				// algoname + scalingName
				runtime.put(e.getKey().getKey() + e.getKey().getValue(), e.getValue().getValue().getKey());
				memory.put(e.getKey().getKey() + e.getKey().getValue(), e.getValue().getValue().getValue());
			}
			createFiles(outputFolder, memory, "memory", memoryTemplate);
			createFiles(outputFolder, runtime, "runtime", runtimeTemplate);

		}

	}

	private static void createFiles(Path outputFolder, Map<String, Double> data, String criterion, List<String> runtimeTemplate)
			throws IOException, InterruptedException {
		for (int k = 0; k < SCALING_QUALIFIERS.length; k++) {
			final String scalingQualifier = SCALING_QUALIFIERS[k];
			final Path p = outputFolder.resolve(criterion + "-" + scalingQualifier + ".dat");
			try (BufferedWriter bw = Files.newBufferedWriter(p)) {
				bw.write("#\tX");
				for (int j = 0; j < BarchartGenerator.keys.length; j++) {
					bw.write('\t');
					bw.write(BarchartGenerator.values[j]);
				}
				bw.write('\n');
				bw.write(legendMap.get(scalingQualifier + "0").toString());
				for (int j = 0; j < BarchartGenerator.keys.length; j++) {
					bw.write('\t');
					bw.write(data.get(BarchartGenerator.keys[j] + "initial-data").toString());
				}
				bw.write('\n');
				for (int i = 1; i <= 9; i++) {
					bw.write(legendMap.get(scalingQualifier + i).toString());
					for (int j = 0; j < BarchartGenerator.keys.length; j++) {
						bw.write('\t');
						final Double value = data.get(BarchartGenerator.keys[j] + scalingQualifier + "-" + i);
						if (value == null) {
							bw.write(" ");
						} else {
							bw.write(value.toString());
						}
					}
					bw.write('\n');
				}
			}

			final List<String> lines = new ArrayList<>(runtimeTemplate);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				line = line.replaceAll("\\$OUTPUT_NAME", p.getFileName().toString() + ".pdf");
				line = line.replaceAll("\\$INPUT_FILE", p.getFileName().toString());
				line = line.replaceAll("\\$ATTRIBUTE", SCALING_LEGEND[k]);
				lines.set(i, line);
			}
			final String newFileName = p.toString() + ".gp";
			final Path plotFile = Paths.get(newFileName);
			Files.write(plotFile, lines);
			final Process proc = Runtime.getRuntime().exec(new String[] { "gnuplot", plotFile.toString() }, null, p.getParent().toFile());
			proc.waitFor();
			System.out.println("" + proc.exitValue());
		}
	}
}
