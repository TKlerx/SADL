# Type 3 thesis reconstruction

This is the controlled comparison for [#37](https://github.com/TKlerx/SADL/issues/37). It reconstructs the generation mechanism for Fig. 6.7c (PDRTA without preprocessing) and Fig. 6.8d (PDTTA with preprocessing). The original exports and selected SMAC settings are still unverified. [Dissertation §6.4](https://digital.ub.uni-paderborn.de/hs/download/pdf/2445002#page=127)

## Rerun

Use Linux with JDK 25, Maven and Python 3. Run from the repository with both pinned commits available in Git history. Pass a new output directory.

```sh
bash specs/type3-thesis-reconstruction/run.sh /tmp/sadl-type3-reconstruction
```

The script builds SADL at `8ea0694`, takes `Temp.java` from `6f9fe2c` and limits that copy to model-based Type 3. It uses seeds `7222525536004714236`, `37` and `20261117`. For each seed and setup, ten held-out files supply 700 training and 300 evaluation traces each. The generator also writes ten tuning files per setup. The logs and generated traces stay in the output directory.

## Scoring

Each evaluation file is scored twice, once with exported symbols and once with aliases such as `b.2` collapsed to base symbols in both training and evaluation. Each representation gets its own ALERGIA fit with alpha 0.05, the recursive paper test, and minimum visits 3. Lower normalized event likelihood means more anomalous for ROC AUC.

The matched control copies every held-out normal trace, increases its first delay by one, and retains its complete event sequence and other delays. The scorer checks the entire ALERGIA event-likelihood vector for equality. Each file's matched path frequency distance is zero and AUC is 0.5. The reconstruction AUC is an untuned statistic. The thesis figures report SMAC-tuned MCC.

## Reference results

- [Summary by seed and file](results/results.md)
- [All file-level metrics](results/per-file.tsv). TV is total variation between normal and anomalous complete-path frequencies. The summary uses pooled TV and mean file-level AUC.
- [All file-level path counts](results/path-frequencies.tsv). Each row gives the path and observed counts by class. The `-matched` rows count the matched timing-only controls.

The tables preserve the run recorded in #37 using Maven's project dependency classpath. An exploratory run with a broader classpath and a clean rerun with Maven's classpath both produced small differences in some counts and scores. The clean rerun reproduced the findings across seeds, including exact equality for all matched event-likelihood vectors. The fixed seeds document the experiment. The saved tables are the exact outcomes of the reference run.
