# Migrated tasks for the train-to-test CLI

Completed items reconstruct work present in the application. Coverage gaps are listed separately.

- [x] T001 Register and dispatch the train and test CLI commands in `SADL.java`.
- [x] T002 Parse training input, select a learner, and save the trained model in `TrainRun.java`.
- [x] T003 Load the saved model, evaluate labeled sequences, and write metrics in `TestRun.java`, `Evaluation.java`, and `ExperimentResult.java`.
- [x] T004 Cover an RTI model serialization round trip in [RtiPlusTest.java](../../PDTTA-core/test/sadl/modellearner/RtiPlusTest.java). The test can return early under `TRAVIS`.
- [x] T005 Cover result calculations in [ExperimentResultTest.java](../../PDTTA-core/test/sadl/experiments/ExperimentResultTest.java).

## Gaps

- No automated CLI handoff test was found. The [saved-model compatibility fixture spec](../001-saved-model-compatibility/spec.md) covers the fresh and pre-change model paths.
- The CLI loader has no documented model-file trust policy. Some I/O errors are logged without an explicit nonzero exit, so error behavior should be specified separately before changing it.
