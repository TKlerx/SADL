# Tasks for Saved Model Compatibility

**Input** [Spec](spec.md), [plan](plan.md), [research](research.md), [data model](data-model.md), [CLI contract](contracts/cli.md), and [quickstart](quickstart.md)

**Scope** Test code, fixture resources, and validation evidence. Loader behavior, model format, CLI behavior, and trust policy are out of scope.

## Phase 1 — Setup

- [X] T001 Prepare the Linux JDK 21, GCC, GSL, and Maven baseline from `README.md`. Build with `trebaJni/trebaNative/swig/treba_jni.sh` and install using `trebaJni/trebaNative/pom.xml`. Record the pre-change source revision for artifact capture.

## Phase 2 — Foundation

- [X] T002 [P] Create small, fixed timed inputs in `PDTTA-core/testResources/saved-model-compatibility/train.inp` and `PDTTA-core/testResources/saved-model-compatibility/evaluate.inp`. Use the standard CLI format, with both normal and anomalous labels in the evaluation file. Reuse the training input as the detector training argument.

## Phase 3 — US1 Verify the saved-model workflow (P1)

**Independent test** Run the fixture with an unusable reference and observe the expected CLI assertion failure. Then run with the valid reference twice from fresh directories and verify that both model paths match all four frozen counts.

- [X] T003 [P] [US1] Add the JUnit 4 subprocess harness in `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java`. Use the running JDK and absolute Surefire classpath entries, a fresh working directory per evaluation, and a bounded timeout with captured diagnostics.
- [X] T004 [US1] In `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java`, exercise the public `test` command first with temporary unusable model bytes and the fixed inputs. Use the same exit and result assertions as the valid reference path. Run the targeted check and record the expected failure at a CLI result assertion, after the child process reaches model loading. Confirm that zero exit with no newly written result also fails the assertions. Complete this red check before either green run.
- [X] T005 [US1] From the recorded pre-change CLI, train once with the fixed input and verify one evaluation to capture the oracle. Save the unchanged artifact as `PDTTA-core/testResources/saved-model-compatibility/reference.model` and record revision, JDK, input paths, exact train/test options, and fixed TP, FP, TN, and FN in `PDTTA-core/testResources/saved-model-compatibility/README.md`. Verify the counts account for every evaluation sequence.
- [X] T006 [US1] Complete both paths in `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java`. Verify that a separate `train` invocation produces a nonempty temporary model and that separate `test` invocations for the fresh and frozen models exit zero and write one new result row each. Compare fields 7–10 to the recorded counts. Reuse the same assertions for the unusable-model check and preserve the reference bytes.

## Phase 4 — Final verification

- [X] T007 After T004–T006, run the targeted Maven check twice in fresh output directories, then run the normal Linux reactor `mvn -B test`. Confirm both paths run in the standard suite. Record the red and green outcomes in `specs/001-saved-model-compatibility/quickstart.md` and verify the diff contains only tests, fixture resources, and feature docs.

## Dependencies & Execution Order

`T001 → (T002 ∥ T003) → T004 (red) → T005 (baseline capture) → T006 → T007 (two greens, then full CI)`

T002 and T003 can run in parallel because the harness can use the agreed resource paths while the inputs are prepared. US1 has no other story dependencies.

## Implementation Strategy

US1 is the entire MVP. Complete one representative model and one labeled dataset through both CLI paths before considering broader model coverage.
