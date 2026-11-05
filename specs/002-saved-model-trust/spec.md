# Feature Specification for Saved Model Loader Trust

**Feature Branch** `codex/tdd-brownfield`

**Created** 2026-11-04

**Status** Draft

**Input** Define the saved-model trust boundary and preserve compatibility with freshly trained and pre-change models using the existing CLI fixture.

## User Scenarios & Testing

### User Story 1 - Evaluate a trusted saved model (Priority P1)

As a SADL operator, I can evaluate a supported model I trained or obtained through a trusted channel so I can continue using existing models after the loader changes.

**Independent Test** — Run the [saved-model CLI fixture](../001-saved-model-compatibility/spec.md) with its existing inputs, frozen artifact, and expected counts.

1. **Given** a model created by the current `train` command, **when** a separate `test` invocation loads it, **then** evaluation succeeds and produces the expected result.
2. **Given** the unchanged pre-change reference model, **when** `test` evaluates it with the same settings and labeled sequences, **then** evaluation succeeds with the same expected counts without regenerating or converting the artifact.

### User Story 2 - Reject unsafe or unsupported model content (Priority P1)

As a SADL operator, I get a clear failure when the supplied file cannot safely be treated as a supported saved model.

**Independent Test** — Run `test` against benign fixtures representing each rejection category in a fresh output directory.

1. **Given** a malformed or truncated file, an unsupported model, or a file containing data outside the permitted model contents, **when** `test` loads it, **then** the command exits nonzero, reports a model-load error, and produces no evaluation result.
2. **Given** a file that exceeds a documented loading limit, **when** `test` loads it, **then** it rejects the file without continuing to evaluation.
3. **Given** a missing or unreadable model path, **when** `test` loads it, **then** the command exits nonzero with an actionable file error and produces no evaluation result.

### Edge Cases

- A supported-looking filename or local path supplies no evidence of provenance.
- A file with a supported outer model but disallowed nested content is rejected.
- Rejection leaves any pre-existing evaluation result untouched. A successful result must come from the current invocation.

## Requirements

### Functional Requirements

- **FR-001** — Document that operators may supply models they trained themselves or obtained from a trusted source with integrity they can establish. Treat the supplied file contents as untrusted at the CLI boundary regardless of provenance.
- **FR-002** — Accept only supported SADL model types and the contents needed to reconstruct those models. Reject disallowed contents, including nested contents, before they can trigger behavior outside model reconstruction. A rejected file must never be retried through an unrestricted loader.
- **FR-003** — Enforce and document finite limits on file size and reconstruction complexity. Reject inputs that exceed them during loading.
- **FR-004** — On model-load failure, exit nonzero, identify the model-load problem without dumping model contents, and leave evaluation and result output untouched.
- **FR-005** — Preserve the public `train` and `test` workflow, including successful evaluation of both models in the existing fixture with its frozen bytes, input, settings, and expected counts.
- **FR-006** — Add test-first CLI regression coverage for rejection using harmless local fixtures. Keep the positive compatibility fixture in standard validation.

### Compatibility boundaries

The boundary is the saved file supplied to `test -model`. The [migrated CLI spec](../train-to-test-cli/spec.md) describes the existing command and output behavior. Successful output remains compatible. Model-load failures now explicitly fail the command before evaluation.

### Key Entities

- **Saved model** — A file supplied by the operator, including the freshly trained and frozen pre-change artifacts used for compatibility checks.
- **Supported model contents** — The model types and nested data the loader permits, subject to documented resource limits.
- **Model-load failure** — A rejected input with a nonzero exit status, a diagnostic, and no new evaluation result.

## Success Criteria

### Measurable Outcomes

- **SC-001** — Both existing positive fixture paths pass with TP 1, FP 0, TN 1, and FN 0 in standard validation.
- **SC-002** — Every planned rejection case exits nonzero and leaves the evaluation destination absent or unchanged. Cover missing or unreadable input, malformed input, unsupported outer or nested content, and at least one case per documented resource limit.
- **SC-003** — Rejection checks demonstrate a failure against the old behavior before the loader change and pass with the positive compatibility checks in the full Linux suite.

## Assumptions

- Operators are responsible for provenance. The loader checks content and resource limits. This feature does not add model signing, source authentication, or a guarantee for arbitrary third-party models.
- Planning will inventory supported model contents and set concrete limits that accommodate supported training outputs and the frozen reference. The fixture gives direct compatibility evidence for one representative RTI model.
- Other import surfaces, model format migration, and changes to training or evaluation logic are outside this feature.
