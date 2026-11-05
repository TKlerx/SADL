# Implementation Plan for Saved Model Loader Trust

**Branch** `codex/tdd-brownfield` | **Date** 2026-11-04 | **Spec** [Saved model loader trust](spec.md)

## Summary

Apply a fail-closed policy to `test -model`. Preserve the existing saved-file representation and reuse the existing CLI compatibility fixture unchanged. Treat origin verification as the operator's responsibility.

## Technical Context

- JDK 21 minimum for both Java modules and Linux CI. Use JUnit 4 and the existing Maven reactor.
- Admit only PDRTA and PDTTA as exact top-level classes. Keep the class-level policy closed by default and scoped to this load. See the [type inventory and limits](data-model.md).
- Check regular file size before reconstruction and cap actual reads on the opened stream. Install a per-stream class and graph filter before reading the first object. Reject proxies and unlisted classes, including nested classes. Verify the exact root type before detector or evaluation work. A missing filter capability fails closed.
- Keep reconstruction and evaluation in the same bounded child JVM, so the parent never deserializes a worker-produced copy. The public CLI supervises startup and loading with a deadline, waits for explicit validation success before treating the worker as ready, and propagates the worker exit status. Use the current JDK and classpath and preserve working directory, successful output, and CLI settings. Keep readiness signaling separate from user output.
- Put the loader policy and CLI integration in `PDTTA-core`. Leave generic serialization callers, XML imports, training, and scoring logic outside the change.

## Constitution Check

Pass before research and after design. Write rejection tests at the public CLI boundary first. Run the fresh and frozen RTI fixture unchanged and add acceptance evidence for every newly admitted PDTTA variant. Validate the JDK 21 filter and the full Linux suite. The worker is justified by unfiltered allocation inside legacy readers. Its heap ceiling also applies during evaluation, so measure representative evaluations and document that ceiling as part of the CLI contract.

## Project Structure

- `PDTTA-core/src/sadl/run/commands/TestRun.java` and `PDTTA-core/src/sadl/run/SADL.java` for the CLI boundary and failure propagation.
- `PDTTA-core/src/sadl/run/` for the CLI-specific model policy and bounded worker.
- `PDTTA-core/test/sadl/run/` for JUnit checks, including the existing `SavedModelCompatibilityTest`.
- `README.md` for the operator-facing trust boundary, supported contents, and loading limits.

## Complexity Tracking

| Need | Decision |
| --- | --- |
| Legacy collections can allocate inside custom readers | Use a bounded worker plus a stream filter. A stream filter alone cannot bound those allocations. |
| JDK minimum | Compile both Java modules with release 21 and use the standard stream-filter API directly. |
