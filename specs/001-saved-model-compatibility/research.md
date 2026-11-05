# Research decisions

## Representative model

**Decision** — Start with RTI/PDRTA using fixed RTI options, initially `-sig 0.05 -hist QUANTILE`, with global `-parallel false` before the command. Evaluate with explicit `THRESHOLD_AGG_ONLY`, `MINIMAL` features, and fixed thresholds. Supply `-trainSeqs` even with thresholds because the CLI's vector detector implements the trainable interface.

**Rationale** — The existing RTI serialization tests establish a component baseline for PDRTA. Use the public CLI on small inputs to confirm the exact settings and counts before freezing the artifact. Include both normal and anomalous labels and confirm the outcome counts exercise both classes.

**Alternatives** — PDTTA with density estimation is a viable pure-Java alternative. Default SVM would add fitted-classifier behavior to the compatibility check.

## CLI isolation and oracle

**Decision** — Launch `sadl.run.SADL` in child JVMs with the running JDK and Surefire's test classpath. Normalize classpath entries and fixture paths before switching working directories. Capture combined output, enforce a timeout, and use the default result filename in a fresh directory.

**Rationale** — The public entry point calls `System.exit`. The default result name avoids relying on a result-path override whose field is declared final. Counts are stable candidates for a fixed oracle. Runtime and memory columns vary.

**Alternatives** — Direct calls to internal handlers omit CLI process behavior. Regenerating the reference in each run would lose the pre-change model baseline. Full-row comparisons would include variable fields.
