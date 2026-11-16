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

The supported `smac PDTTA` and `train PDTTA` CLI commands accept
`-mergeMethod MDI_TREBA_HISTORICAL` for historical MDI and
`-mergeMethod TREBA_NATIVE_PARITY` for Treba-style ALERGIA. Replace the old
`-mergeTest MDI` and `-mergeTest ALERGIA` flags when migrating SMAC settings.
Use the [current PDTTA parameter file](PDTTA-core/smac/pdtta/pdtta-params.pcs)
as the starting point for a SMAC search and add the desired mode to its
`mergeMethod` choices. The [old Treba parameter file](PDTTA-core/smac/pdtta/treba-params.pcs)
records the historical settings.

The legacy Java `PdttaLearner` constructors that accept a `MergeTest` still
accept `ALERGIA` and `MDI` and route them to the Java learners. Those constructors
also accept a smoothing prior. The historical MDI CLI mode uses a zero prior.
For new experiments, `MDI_TREBA` uses a deterministic rule for the blue-only
case, and `MDI_PAPER` uses the published divergence criterion.

Use the [pinned native revision](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87)
for exact native reruns or for the retired `CHI_SQUARED`, `LR`, `BINOMIAL`,
`EXACT_M`, and `EXACT_B` modes. The
[historical MDI results](specs/treba-mdi/historical-reference.md) document the
binary, toolchain, corpora, and settings used to verify parity.

## Saved model trust

Supply models you trained or obtained from a trusted source whose integrity you can establish. SADL checks every file passed to the test command against its saved-model policy. The accepted roots are RTI PDRTA and PDTTA with reviewed nested contents. The exact inventory and capture evidence are in the [saved model policy](specs/002-saved-model-trust/data-model.md).

The file and read limits are 16 MiB. Loading also allows up to 64 levels of depth, 250,000 references, and 250,000 elements per filter-visible array. A separate JVM has a 512 MiB Java heap during loading and evaluation, and must finish loading within 30 seconds. On rejection the CLI exits nonzero before evaluation and leaves any existing result untouched.

The worker carries over the effective JDK serialization pattern, including stricter operator limits. Model loading fails closed if the parent uses a custom filter or factory whose behavior cannot be reproduced in the worker.
