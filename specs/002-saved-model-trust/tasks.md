# Tasks for Saved Model Loader Trust

**Input** [Spec](spec.md), [plan](plan.md), [research](research.md), [type inventory and limits](data-model.md), [CLI contract](contracts/cli.md), and [quickstart](quickstart.md).

**Scope** Apply the trust policy to `test -model`. Preserve the saved-file format, generic serialization callers, training and scoring behavior. Keep the existing fresh and frozen compatibility test and all its resources byte-for-byte intact.

## Phase 1 — Setup

- [X] T001 Prepare the Linux/JDK 21 Maven reactor using `README.md` and `specs/002-saved-model-trust/quickstart.md`. Build and install the native artifact using `trebaJni/trebaNative/swig/treba_jni.sh` and `trebaJni/trebaNative/pom.xml`. Record the baseline revision and hashes of `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java` and every file in `PDTTA-core/testResources/saved-model-compatibility/`.

## Phase 2 — Foundation and red gate

- [X] T002 Add and run red CLI tests in `PDTTA-core/test/sadl/run/SavedModelRejectionTest.java` before any production loader edit. Use benign cases for missing and unreadable paths, malformed and truncated files, unsupported roots, and unsupported nested content under a supported root. Require nonzero exit, a concise model-load diagnostic, and absent or byte-identical pre-existing result output. Record the assertions that fail on the baseline in `specs/002-saved-model-trust/quickstart.md`. (FR-002, FR-004, FR-006)
- [X] T003 [P] Define limit and worker failure checks in `PDTTA-core/test/sadl/run/SavedModelLoadingLimitsTest.java` before implementation. Cover regular file size and bytes read from the opened stream at **16 MiB (16,777,216 bytes)**, depth **64**, stream references **250,000**, length of any filter-visible array **250,000 elements**, worker Java heap **512 MiB**, and worker startup and load to validated root **30 seconds**. Check inclusive graph and byte limits. Use benign generated inputs and lower test-only budgets for controlled collection allocation and slow-load cases. Assert fail-closed behavior for unavailable filtering, worker exit before readiness, and timeout without evaluation. Keep red evidence in `specs/002-saved-model-trust/quickstart.md`. (FR-003, SC-002, SC-003)

## Phase 3 — US1 Evaluate a trusted saved model (P1)

**Independent test** Train and evaluate representative PDTTA variants and run the unchanged fresh and frozen RTI fixture. Each RTI path must produce TP 1, FP 0, TN 1, FN 0.

- [X] T004 [P] [US1] Capture trusted models with the pre-change `train PDTTA` CLI and evaluate them with the baseline `test` CLI. Cover density and Monte Carlo, supported explicit kernels and automatic selection, and fixtures that exercise constant and variable timing distributions. Compare exact serialized descriptors and filter events on JDK 21, including runtime scratch arrays and replacement objects. Record revision, input, options, expected output, and exact admitted classes per variant in `specs/002-saved-model-trust/data-model.md`. Audit custom readers and keep source-derived candidates excluded unless supported by capture evidence. (FR-002, FR-005)
- [X] T005 [US1] Add separate positive checks for the captured PDTTA variants in `PDTTA-core/test/sadl/run/PdttaSavedModelCompatibilityTest.java`, with any new inputs under `PDTTA-core/testResources/pdtta-saved-model-trust/`. Verify baseline train-to-test behavior and freeze the expectations independently of the new loader. Run `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java` unchanged as the RTI baseline. (FR-005, SC-001)

## Phase 4 — US2 Reject unsafe or unsupported model content (P1)

**Independent test** Exercise the public CLI rejection contract with the red inputs from T002 and all budgets from T003. Rejections must occur before evaluation and leave result output untouched.

- [X] T006 [US2] Implement the CLI-only per-stream policy in `PDTTA-core/src/sadl/run/SavedModelLoader.java`. Admit exact `sadl.models.pdrta.PDRTA` and `sadl.models.PDTTA` roots and only the verified descriptors and array types in `specs/002-saved-model-trust/data-model.md`. Set release 21 in PDTTA-core/pom.xml and trebaJni/pom.xml. Apply the T003 graph and byte limits, reject nonregular inputs, proxies and unsupported content, and fail closed if the required JDK 21 filter cannot be installed. Preserve stricter process policy. Keep `PDTTA-core/src/sadl/utils/IoUtils.java` outside this change. (FR-002, FR-003)
- [X] T007 [US2] Implement bounded worker supervision in `PDTTA-core/src/sadl/run/SavedModelWorker.java` and integrate it through `PDTTA-core/src/sadl/run/SADL.java` and `PDTTA-core/src/sadl/run/commands/TestRun.java`. Use the current JDK, classpath and working directory, enforce T003’s heap and loading deadline, signal validated readiness separately from user output, and keep loading and evaluation in the worker. On loading failure, stop before detector or evaluation work, preserve existing results, and propagate a nonzero exit with a concise diagnostic. Keep the parent free of model deserialization and preserve successful CLI settings and output. (FR-003, FR-004, FR-005)
- [X] T008 [US2] Run `PDTTA-core/test/sadl/run/SavedModelRejectionTest.java` and `PDTTA-core/test/sadl/run/SavedModelLoadingLimitsTest.java` on JDK 21. Verify every negative category and budget, including cleanup after timeout or early exit and no unrestricted fallback. Record results in `specs/002-saved-model-trust/quickstart.md`. (FR-006, SC-002, SC-003)

## Phase 5 — Final verification

- [X] T009 [P] Document operator provenance responsibility, supported model contents, the T003 limits, failure behavior, and the worker heap ceiling during evaluation in `README.md`. Link the exact content inventory and state the JDK 21 minimum. (FR-001, FR-003)
- [X] T010 Run both compatibility suites on JDK 21, then the full Linux `mvn -B test` from `pom.xml`. Verify the T001 hashes, fresh and frozen counts, and representative PDTTA evaluations with the worker heap ceiling. Record the results and command evidence in `specs/002-saved-model-trust/quickstart.md`. (SC-001, SC-003)

## Dependencies & Execution Order

`T001 → T002 → (T003 ∥ T004) → T005 → T006 → T007 → (T008 ∥ T009) → T010`

- US1 baseline evidence precedes US2 policy implementation. US1’s post-change acceptance is checked in T010.
- Parallel example for US1 and the shared foundation. After the first red run, collect trusted PDTTA captures (T004) while writing the limit tests (T003).
- Parallel example for US2. After integration, run rejection tests (T008) while documenting the fixed policy (T009).

## Implementation Strategy

The MVP includes both P1 stories. A rejection-only loader could break supported models, so ship the trust boundary only after the rejection checks and both compatibility suites pass.

## Phase 6: Convergence

- [X] T011 Preserve a stricter effective serialization policy across the worker boundary, or fail closed when an in-process policy cannot be reproduced. Add a JDK 21 regression for a programmatically installed policy when invoking `SADL.run` per T006 / FR-002 (partial).
- [X] T012 Apply one monotonic deadline to worker startup and validated readiness. Reject readiness observed after expiry, terminate the child on failure, and test a late readiness signal per FR-003 / T003 / T007 (partial).
- [X] T013 Exercise each documented loading budget through the public CLI with benign inputs or controlled test budgets. Assert nonzero exit, the model-load diagnostic, and absent or byte-identical result output. Retain the component checks and rerun the unchanged compatibility fixture and full Linux suite per SC-002 / T008 / T010 (partial).

## Phase 7: Convergence

- [X] T014 Exercise the opened-stream read budget through the public CLI with benign input that passes the regular-file size check and reaches the read cap. Verify nonzero exit, a model-load diagnostic, and absent or unchanged result output. Retain the component read/skip test and rerun the unchanged fixture per FR-003 / SC-002 / T013 (partial).

## Phase 8: Convergence

- [X] T015 Run the full Linux JDK 21 Maven reactor on the current T014 implementation. Verify the opened-stream regression, unchanged fresh and frozen RTI fixture, PDTTA compatibility, and JNI checks run and pass. Record the results in specs/002-saved-model-trust/quickstart.md per SC-003 / T010 / T013 (partial).
