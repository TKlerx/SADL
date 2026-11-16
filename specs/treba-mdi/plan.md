# MDI Java port plan

Read [the algorithm and native scoring notes](algorithm.md) before using the captures.

## Native observations

Captured from SADL revision `8d3f802`, using Ubuntu 24.04 x86-64, GCC 13.3.0, GSL 2.7.1, and Temurin 21.0.12.1.

- [Optimized scores](native-scores-o2.tsv) and [unoptimized scores](native-scores-o0.tsv) are identical. Each file holds 20 fresh-process observations per case. Columns are case, score, and merge decisions for thresholds one representable double below, exactly at, and one above the observed score. The cutoff score was `0.059526861155626776`, with decisions reject, reject, accept. The blue-only score was `0.25936556631921465`, with the same decisions. GCC's `-Wmaybe-uninitialized` flagged `frbprob` in the blue-only path. Treat that score as a historical observation.
- [PDTTA train-to-test output](native-pdtta.tsv) was byte-identical across three fresh Maven test JVMs with the optimized library. It uses the existing saved-model fixture's training and evaluation inputs, alpha 0.05, minimum visits 3, and zero smoothing. Native training learned a one-state PDTTA, accepted the normal trace, and rejected the unseen-symbol trace. The capture records the resulting topology, stopping probability, and event and time scores. This case uses the learner and scoring APIs.

## Reproduce on Linux

From the repository root, set `JAVA_HOME` and install the dependencies listed in the project README. The alternate build recompiles only the scoring translation unit.

Run these capture commands from a checkout of the [pinned native revision `c80ede9`](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87).

```sh
native=trebaJni/trebaNative/swig
out=$(mktemp -d)
bash "$native/treba_jni.sh"
gcc -I"$native" specs/treba-mdi/score-probe.c -L"$native" \
    -Wl,--allow-shlib-undefined -ltreba -lm -o "$out/score"
for case in cutoff blue-only; do
    for i in {1..20}; do LD_LIBRARY_PATH="$native" "$out/score" "$case"; done
done > "$out/o2.tsv"
(
    cd "$native"
    gcc -O0 -fPIC -fcommon -c dffa.c -o "$out/dffa.o"
    gcc -shared treba.o treba_wrap.o "$out/dffa.o" gibbs.o observations.o io.o \
        -o "$out/libtreba.so" -lm -lpthread -lgsl -lgslcblas
)
for case in cutoff blue-only; do
    for i in {1..20}; do LD_LIBRARY_PATH="$out" "$out/score" "$case"; done
done > "$out/o0.tsv"
mvn -B -f trebaJni/trebaNative/pom.xml install
for i in 1 2 3; do
    mvn -B -Dtest=TrebaMdiReferenceCapture -Dsurefire.failIfNoSpecifiedTests=false \
        -Dtreba.reference.output="$out/pdtta-$i.tsv" test
done
cmp "$out/o2.tsv" "$out/o0.tsv"
cmp "$out/pdtta-1.tsv" "$out/pdtta-2.tsv"
cmp "$out/pdtta-1.tsv" "$out/pdtta-3.tsv"
```

## Implementation and acceptance

`train PDTTA -mergeMethod MDI_PAPER` and the PDFA factory select the Java implementation. It uses red/blue candidate ordering and the published signed score. The default cutoff is 0.05, and the default minimum visit count is 3. The recursion flag does not affect MDI. The implementation recomputes the empirical score for each candidate, with zero smoothing.

On the frozen PDTTA fixture, Java keeps four states and native Treba produces one. Both accept the normal trace and reject the unseen-symbol trace. The normal trace has Java event probabilities `[1, 1, 1, 1]` and native probabilities `[0.5, 0.25, 0.5, 0.25]`. Its Java timing scores are approximately `[0.335762, 0.335762, 0.335762]` versus native `[0.343829, 0.335762, 0.343829]` because the native model pools observations from both `a` positions. The unseen-symbol trace gives `[0]` and an empty timing vector in both runs.

1. Add failing tests for the signed divergence rule using independently calculated small cases. Include equality and adjacent thresholds, a blue-only branch, two siblings in different orders, and duplicate observations. Use the native cutoff capture to check strict comparison. Derive numerical expectations for Java from the published rule.
2. Reuse the Java frequency tree, red/blue ordering, visit cutoff, folding, and PDFA conversion where applicable. Start with a candidate copy and empirical log-probability comparison over training observations. Measure runtime on a larger corpus before adding incremental scoring. Keep smoothing in PDFA conversion and document that the ALERGIA recursion flag has no effect on MDI.
3. Compare Java topology, acceptance, and event and time scores on the frozen PDTTA case. Record deliberate deviations from native behavior, including any mismatch attributable to the native score rule. Keep the native revision pinned for historical replication if Java results differ.
4. Route the existing MDI experiment entry points to Java once the behavior is reviewed. Exercise train, save, and test with an unusable native library, rerun the full Temurin 21 and 25 suite, and verify fresh and frozen saved models plus rejection tests before removing JNI in a separate change.
