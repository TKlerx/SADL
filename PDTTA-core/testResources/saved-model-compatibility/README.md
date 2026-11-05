# Saved-model compatibility reference

Captured from SADL revision `f286f099929a34ba6ccb01b618c76aeeda94133a` on Linux with Temurin JDK 21.0.12.1. The reference is an RTI/PDRTA model trained on the adjacent `train.inp`. Its SHA-256 is `0250ec3d6c2456cf19cf45b8b626016ebfc52a84cd48693a25c20776cfb6a473`.

From this resource directory, use the baseline application's runtime classpath:

```sh
java -cp "$SADL_CLASSPATH" sadl.run.SADL -parallel false train RTI -in train.inp -out /tmp/sadl-reference.model -sig 0.05 -hist QUANTILE
java -cp "$SADL_CLASSPATH" sadl.run.SADL -parallel false test -model /tmp/sadl-reference.model -trainSeqs train.inp -testSeqs evaluate.inp -detectorMethod THRESHOLD_AGG_ONLY -featureCreator MINIMAL -aggregatedEventThreshold 0.01 -aggregatedTimeThreshold 0.01
```

The verified result for `evaluate.inp` is **TP 1, FP 0, TN 1, FN 0**. The test uses these fixed counts and ignores timing and memory measurements. The commands leave the checked-in reference model untouched.
