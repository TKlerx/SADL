package sadl.models.pdrtaModified;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import sadl.input.TimedInput;
import sadl.modellearner.ButlaPdrtaLearner;

public class TestConsole {

	String fileName;

	public TestConsole(String fileName) {
		this.fileName = fileName;
	}

	public void start() throws IOException, URISyntaxException {

		final TimedInput input = TimedInput.parse(Paths.get(this.getClass().getResource(fileName).toURI()));
		final ButlaPdrtaLearner learner = new ButlaPdrtaLearner();
		final PDRTAModified pdrta = learner.train(input);

		if (pdrta == null) {
			System.out.println("No PDRTA created");
			return;
		}

		PDRTAStateModified currentState = pdrta.getRoot();

		final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;

		System.out.println(currentState);

		// Input format: "eventsymbol time"
		while ((line = reader.readLine()) != "") {
			final String[] commandArray = line.split(" ");

			final String eventSymbol = commandArray[0];
			final double time = Double.parseDouble(commandArray[1]);
			currentState = currentState.getNextState(eventSymbol, time);
			System.out.println(currentState);
		}
	}
}
