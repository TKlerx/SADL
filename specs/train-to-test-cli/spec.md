---
status: migrated
---

# Existing train-to-test CLI

**Baseline** 2026-11-04, application tree at `f286f09`

## User Scenarios & Testing

1. An operator supplies timed training sequences and one supported algorithm to `train`. With valid nonempty input, the command trains a model and saves it to the requested location.
2. An operator runs `test` separately with that saved model and labeled timed sequences. Evaluation writes one result row and prints the F-measure.
3. If either command parses an empty dataset, it warns and returns without producing a new artifact. Some I/O failures are logged inside the command, so exit status alone cannot establish that an artifact was written.

## Requirements

- **FR-001** — `train` accepts RTI, PDTTA, or BUTLA, a training input through `-in`, and an output through `-out` (default `sadl_train_out.model`).
- **FR-002** — `test` accepts a saved model through required `-model` and evaluation data through `-testSeqs`. The command exposes detector settings and a training-data option for its conditional detector-training path.
- **FR-003** — Evaluation compares anomaly decisions with input labels. The result row includes true positives, false positives, true negatives, false negatives, and summary metrics. The result destination is exposed through `-resOut`, with a declared default of `sadl_test_res.csv`.
- **FR-004** — For a successful run on valid inputs, the saved model is usable by a separate test invocation and the result counts account for the evaluated sequences.

## Evidence and Limits

The existing learner, detector, and metric tests cover parts of the flow. The full two-command handoff, option overrides, and failure outcomes lack direct CLI test evidence. This baseline records current behavior. The [saved-model compatibility fixture](../001-saved-model-compatibility/spec.md) defines the separate regression check.
