# SADL brownfield gap analysis

Baseline is main at f286f09. This inventory follows the Spec Kit Brownfield Bootstrap scan and uses tracked source, Maven configuration, and the successful [Linux CI run](https://github.com/TKlerx/SADL/actions/runs/41814278896). Coverage is based on source and CI logs. Platform behavior outside that run needs separate verification.

## Current system

Java accounts for 265 of 393 tracked files. SADL is a multi-module Maven library and CLI with a native C dependency.

| Module | Role |
| --- | --- |
| PDTTA-core | Parses timed sequences, trains automata, detects anomalies, and provides the train, test, and smac CLI commands. Depends on trebaJni. Uses src and test as Maven source roots. |
| trebaJni | Java binding and native library loader. Depends on trebaNative and carries the JUnit 4 dependency. |
| trebaJni/trebaNative | Packages libtreba.so. The Linux build compiles the checked-in C sources and JNI wrapper against GSL. |

The Java modules target Java 8 bytecode. GitHub Actions exercises JDK 21 and Linux. The baseline run reported 57 core tests and one JNI test, with zero failures or reported skips. CI is required on protected main.

## Gaps by priority

| Priority | Evidence and impact | Follow-up |
| --- | --- | --- |
| High | The test CLI loads a supplied model path through native Java deserialization. IoUtils.deserialize constructs an ObjectInputStream without an explicit filter at that boundary. The expected trust policy for model files is undocumented. | Define whether callers may supply untrusted model files. Characterize valid historical model loading, then specify and test the trust boundary. See TestRun.java and IoUtils.java. |
| High | The JNI build requires Linux headers, GCC and GSL and writes the rebuilt library over the tracked native_libs/libtreba.so. The full check currently demonstrates one Linux/JDK combination. | Decide which platforms are supported. Verify a clean checkout can build and load the library on each supported target, with generated binaries isolated from source if packaging permits. |
| Medium | Four TauPTA fixture classes contain JUnit tests, but their names miss the standard Surefire discovery patterns and they do not appear in the baseline run. Several RTI+, BUTLA and threshold tests also return early when TRAVIS is set. | Determine which historical fixtures are still valid, make intended tests discoverable and replace implicit environment skips with explicit reporting. Keep existing deterministic checks. |
| Medium | The README advertises Java 8 compatibility, while the required CI validates JDK 21 only. Maven compiler source/target flags describe bytecode and do not establish runtime compatibility. | Decide whether Java 8 remains supported and verify that runtime in CI if retained. |
| Medium | The README lists models and build commands, while the CLI and source define the actual input and persisted-model contracts. POMs also retain deployment via the mvn-repo branch, and .travis.yml references Oracle JDK 8. | Write a small train-to-test contract with representative timed input and a saved model. Confirm the supported release path before cleaning up legacy config. |

The first candidate spec is model-file compatibility and trust. Start with one train-to-test fixture at the CLI boundary, then use the result to bound any loader change. Native packaging and test discovery are separate slices.
