# Native Treba ALERGIA captures

To use the Java parity learner, add `-mergeMethod TREBA_NATIVE_PARITY` to `train PDTTA`. It uses the existing `-mergeAlpha`, `-recursiveMergeTest`, and `-mergeT0` options and a zero smoothing prior. The PDFA experiment path accepts the same selector. Run its focused checks with `mvn -B '-Dtest=TrebaNativeParity*Test' -Dsurefire.failIfNoSpecifiedTests=false test`.

Run the opt-in PIT pilot from the repository root after installing the native Maven artifact as described below:

Run these capture commands from a checkout of the [pinned native revision `c80ede9`](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87).

```sh
mvn -B -Ptreba-parity-mutation test-compile
```

PIT mutates only the Java parity learner and its nested classes. It runs the in-process tests and writes HTML and XML reports under `PDTTA-core/target/pit-reports/`. The forked CLI round trip runs in the regular Maven test suite.

On Linux with Temurin 21, the first run found five survivors. The minimum-visit cutoff, three Hoeffding equality cases, and model immutability assertions killed them. The final run produced 67 mutants, with 66 killed by tests and one timeout. The timed-out mutation reverses the visited-state check during conversion to a PDFA. These runs used the default PIT mutators.

Native source revision `6f17ad71f29b8740dd89c1c6cb2fc6effd1fe195`. Captured on Ubuntu 24.04 x86-64 with GCC 13.3.0, GSL 2.7.1, and Temurin 21.0.12.1. The benchmark reads the existing `PDTTA-core/testResources/pdtta/smac_mix_type1.txt` resource (Git blob `a98e8d39553e4130891729eb0bd7add0e04939d5`). The capture trains through `TrebaPdfaLearner`, which exercises the JNI library and SADL's conversion of the resulting WFSA into a `PDFA`.

To regenerate from the repository root on Linux with the dependencies listed in the project README, set `JAVA_HOME` and run:

```sh
capture=specs/treba-alergia-parity
native=trebaJni/trebaNative/swig
out=$(mktemp -d)
bash "$native/treba_jni.sh"
gcc -I"$native" "$capture/depth-probe.c" -L"$native" \
    -Wl,--allow-shlib-undefined -ltreba -o "$out/depth-probe"
LD_LIBRARY_PATH="$native" "$out/depth-probe" > "$out/native-depth.csv"
mvn -B -f trebaJni/trebaNative/pom.xml install
mvn -B -Dtest=TrebaAlergiaReferenceCapture -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtreba.reference.output="$out" test
diff -u "$capture/native-depth.csv" "$out/native-depth.csv"
diff -u "$capture/native-benchmark.tsv" "$out/native-benchmark.tsv"
diff -u "$capture/native-smoothing.tsv" "$out/native-smoothing.tsv"
diff -u "$capture/native-pdtta-smoothing.tsv" "$out/native-pdtta-smoothing.tsv"
```

The linker flag permits unresolved spinlock references in unrelated functions of the shared library. The C probe invokes the library's own ALERGIA functions directly. The explicit Maven capture requires working JNI and fails if it cannot load Treba. The benchmark rows use zero-based indices within the evaluation split. Probability vectors include the stopping probability when the sequence can be traversed in full. A missing transition ends the vector with zero.

The smoothing references use two copies of `(a,1) (b,2)`, alpha 0.05, recursion enabled, a minimum of three visits, and a prior of 0.5. The PDFA capture includes native smoothing self-loops. The PDTTA capture records the same native learner after the existing timing fit removes unobserved transitions and normalizes the remaining probabilities. A dash denotes an absent transition.

The public `PdttaLearner` overloads that select `MergeTest.ALERGIA` now delegate to Java, including calls from `Pipeline` and `NewSmacPipeline`. Their existing prior, alpha, recursion, and minimum-visit settings carry through. The six other native merge tests remain available through the same overloads.
