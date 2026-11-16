import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import gnu.trove.list.array.TIntArrayList;
import sadl.constants.ClassLabel;
import sadl.input.TimedWord;
import sadl.constants.MergeMethod;
import sadl.detectors.AnomalyDetector;
import sadl.input.TimedInput;
import sadl.modellearner.AlergiaRedBlue;
import sadl.utils.MasterSeed;

class ScoreTypeThree {
    public static void main(String[] args) throws Exception {
        MasterSeed.reset();
        for (String file : args) {
            String[] halves = Files.readString(Path.of(file)).split("(?m)^\\?{25}\\R", 2);
            for (boolean collapse : new boolean[] {false, true}) {
                String train = halves[0];
                String test = halves[1];
                if (collapse) {
                    train = train.replaceAll("\\(([abcd])\\.\\d+,", "($1,");
                    test = test.replaceAll("\\(([abcd])\\.\\d+,", "($1,");
                }
                var learner = new AlergiaRedBlue(0.05, true, MergeMethod.ALERGIA_PAPER, 3);
                var model = learner.train(TimedInput.parse(new StringReader(train)));
                for (var word : TimedInput.parse(new StringReader(test))) {
                    var eventScores = model.calculateProbabilities(word).getFirst();
                    double score = AnomalyDetector.aggregate(eventScores);
                    if (!word.isAnomaly()) {
                        var symbols = new ArrayList<String>();
                        for (int i = 0; i < word.length(); i++) symbols.add(word.getSymbol(i));
                        var times = new TIntArrayList(word.getTimeValues());
                        times.set(0, times.get(0) + 1);
                        var paired = new TimedWord(symbols, times, ClassLabel.ANOMALY);
                        if (!eventScores.equals(model.calculateProbabilities(paired).getFirst())) {
                            throw new AssertionError("Unequal matched event likelihoods in " + file);
                        }
                        System.out.printf("PAIR\t%s\t%s\t%s%n", file, collapse, score);
                    }
                    System.out.printf("SCORE\t%s\t%s\t%s\t%s%n", file, collapse, word.isAnomaly(), score);
                }
            }
        }
    }
}
