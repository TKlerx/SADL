import java.nio.file.Path;
import sadl.run.datagenerators.AlgoWeakTwo;
import sadl.run.datagenerators.Temp;
import sadl.utils.MasterSeed;

public class ReconstructTypeThree {
    public static void main(String[] args) throws Exception {
        MasterSeed.setSeed(Long.parseLong(args[0]));
        final var generator = new AlgoWeakTwo();
        final var pdrta = Path.of("tk-pdrta-normal.txt");
        final var pntta = Path.of("tk-pntta-nonDet-normal.txt");
        final var pdtta = Path.of("tk-pdtta-det-normal.txt");
        AlgoWeakTwo.writeNormalData(pdrta, generator::sampleNormalPdrtaSequence);
        AlgoWeakTwo.writeNormalData(pntta, () -> generator.sampleNormalPndtta(false));
        AlgoWeakTwo.writeNormalData(pdtta, () -> generator.sampleNormalPndtta(true));
        Temp.main(new String[]{pdrta.toString(), pntta.toString(), pdtta.toString()});
    }
}
