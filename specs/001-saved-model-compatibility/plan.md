# Implementation Plan for Saved Model Compatibility

**Branch** `codex/tdd-brownfield` | **Date** 2026-11-04 | **Spec** [Saved model compatibility](spec.md)

## Summary

Use the [migrated train-to-test CLI](../train-to-test-cli/spec.md) as the behavioral baseline. Add one JUnit 4 integration fixture covering a freshly trained model and a checked-in model produced by the pre-change CLI. Loader, serialization format, CLI behavior, and trust policy are outside this test-only change.

## Technical Context

- Java 8 source/target, Maven Surefire and JUnit 4, with JDK 21 on Linux as the CI baseline.
- Use a separate JVM for each public CLI command. Run in fresh temporary directories with fixed inputs and options. Verify exit status and newly written artifacts.
- Use one RTI/PDRTA model and fixed aggregate thresholds. Record reference provenance and expected confusion counts after a verified baseline run. Apply a bounded subprocess timeout with diagnostic output on failure.
- Keep model and sequence resources in `PDTTA-core/testResources/` and the test in `PDTTA-core/test/sadl/run/`. Existing Maven test discovery and the Linux reactor remain the validation entry points.

## Constitution Check

Pass before research and after design. The agreed seam is the public CLI and its on-disk artifacts. Demonstrate red by substituting an unusable reference in temporary storage, then restore the valid artifact for green. Full validation uses the existing Linux/GSL workflow. No deviations.

## Project Structure

- `PDTTA-core/test/sadl/run/SavedModelCompatibilityTest.java` — CLI regression test.
- `PDTTA-core/testResources/saved-model-compatibility/` — timed inputs, frozen model, and provenance with expected counts.
- [Research](research.md), [data model](data-model.md), [CLI contract](contracts/cli.md), and [quickstart](quickstart.md) describe the design and validation.

## Complexity Tracking

No new dependency or module. A subprocess is required because the public entry point exits the JVM.
