# Validate saved model loading

Use Linux, JDK 21, Maven, GCC, and GSL headers. Set `JAVA_HOME` and follow the [repository setup](../../README.md).

```sh
bash trebaJni/trebaNative/swig/treba_jni.sh
mvn -B -f trebaJni/trebaNative/pom.xml install
mvn -B -pl PDTTA-core -am -Dtest=SavedModelCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B test
```

Before changing the loader, run the planned JUnit 4 CLI rejection cases against the baseline and record the expected failures where old behavior differs. Use benign inputs, exercise every rejection category and limit, and verify that existing result output is preserved. Repeat after the change and confirm the unchanged compatibility fixture passes both paths.

Record trusted PDTTA training and evaluation results for the supported variants before completing the allowlist. JDK 21 is the supported minimum for the targeted checks and full suite. See the [CLI contract](contracts/cli.md).

Baseline red on JDK 21: all six CLI rejection tests failed their assertions, with zero test errors, before production loader changes. Baseline revision: 278b53a055dd057cac98d5e56f84097bb5f316f2.

Fixture SHA-256 values matched before and after the loader change.

- SavedModelCompatibilityTest.java a59dac5ae8793bf1120d134e31405dfac28dc2edf43a82f1cebd58f676b987f6
- evaluate.inp 351ae27836a9c6182269fb71855d62ef30976e6d3185161486e4f701ed441c9d
- README.md b8ca860467020d7314e3cd8d2184ade2914cfd12893d54d9b760498a8360d279
- reference.model 0250ec3d6c2456cf19cf45b8b626016ebfc52a84cd48693a25c20776cfb6a473
- train.inp 924532a34a0195fcfbcb8ded89170552f37a391acb972f5920515b21ebfc1e82

Linux JDK 21 targeted validation passed 18 checks, including the unchanged fresh and frozen RTI fixture, all 14 PDTTA capture variants, eight rejection checks, and six limit and worker checks. The full Linux Maven reactor passed 76 tests (75 core and one JNI) with zero failures, errors, or skips.

## Convergence validation

The in-memory filter and late-readiness regressions both failed before the convergence changes. After the changes, Windows JDK 21 passed 24 targeted tests, including the unchanged RTI fixture. All five original fixture hashes still match.

Linux JDK 21 passed all 26 targeted checks and all 84 reactor tests (83 core and one JNI), with no failures or skips. CLI coverage exercises the file/byte budget, depth, references, array length, heap exhaustion, and the startup-to-readiness deadline. The read/skip cap also has a component test. The original fresh and frozen RTI fixture and its resources remain byte-for-byte identical.

T014: The CLI regression failed before the change because the valid 4,292-byte reference was accepted. With a test-only 4,096-byte opened-stream budget, the same file passes the 16 MiB file-size check, exits nonzero with the stream-limit diagnostic, and preserves the pre-existing result byte-for-byte. Linux JDK 21 passed all 25 targeted tests, including both unchanged RTI cases and the component read/skip check. All five original fixture hashes matched.

T015: The full Linux Maven reactor on the T014 implementation passed on Temurin JDK 21.0.12.1. Maven reported 85 tests (84 core and one JNI), with zero failures, errors, or skips. Surefire reports confirmed the opened-stream CLI regression, fresh and frozen RTI cases, both PDTTA compatibility cases, and the native library load test. All five original RTI fixture hashes matched.

PDRTA root validation: The new CLI regression first failed because a PDRTA with a missing root state reached scoring and threw in `PDRTA.testSeqHisto`. With validation before worker readiness, the CLI exits nonzero with a model-load diagnostic and preserves the existing result file byte-for-byte. The targeted regression and both unchanged RTI fixture cases passed. The full Linux `mvn -B test` reactor on Temurin JDK 21.0.12.1 passed 86 tests (85 core and one JNI), with zero failures, errors, or skips. It included both PDTTA compatibility cases, the opened-stream regression, and the native library load test.
