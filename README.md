# SADL
Sequence-based Anomaly Detection Library

## Implemented Models
* PDRTA
* PDTA
* PDTTA
* PDFA

## Implemented Automaton Inference Algorithms
* RTI+
* ALERGIA
* BUTLA
* ProDTAL

## Validation

Run `mvn -B test` with JDK 25. The test JVM opens `java.util` for XStream's
legacy TreeMap serialization. Production XStream settings and its type allowlist
are unchanged. JDK 25 is the minimum supported Java version for the build and CLI.

GitHub CI runs the full suite on Linux with `TRAVIS` unset. Setting
`TRAVIS=true` reports the affected historical tests as skipped.

## Treba compatibility

Legacy PDTTA and SMAC configurations use Java for `mergeTest=ALERGIA` and
`mergeTest=MDI`. The MDI selection reproduces behavior observed in the pinned
Treba build on the documented corpora and settings. For new experiments,
`MDI_TREBA` uses a deterministic rule for the blue-only case, and `MDI_PAPER`
uses the published divergence criterion.

`CHI_SQUARED`, `LR`, `BINOMIAL`, `EXACT_M`, and `EXACT_B` require the old native
build. Use the [pinned native revision](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87)
for those modes and for exact native reruns. See the
[historical MDI results](specs/treba-mdi/historical-reference.md) for the binary,
toolchain, and parity limits.

## Saved model trust

Supply models you trained or obtained from a trusted source whose integrity you can establish. SADL checks every file passed to the test command against its saved-model policy. The accepted roots are RTI PDRTA and PDTTA with reviewed nested contents. The exact inventory and capture evidence are in the [saved model policy](specs/002-saved-model-trust/data-model.md).

The file and read limits are 16 MiB. Loading also allows up to 64 levels of depth, 250,000 references, and 250,000 elements per filter-visible array. A separate JVM has a 512 MiB Java heap during loading and evaluation, and must finish loading within 30 seconds. On rejection the CLI exits nonzero before evaluation and leaves any existing result untouched.

The worker carries over the effective JDK serialization pattern, including stricter operator limits. Model loading fails closed if the parent uses a custom filter or factory whose behavior cannot be reproduced in the worker.
