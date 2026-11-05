# Research decisions

## Type policy

**Decision** — Use exact classes and exact root types, with reviewed entries derived from real models. The frozen RTI model is 4,292 bytes and the descriptor inventory was extracted from its bytes without loading it. For PDTTA, generate trusted models using the existing CLI with density and Monte Carlo variants and representative kernels before admitting their exact classes. Compare filter events as well as serialized descriptors, since runtime collection scratch arrays can be filter-visible.

**Rationale** — The public train command selects RTI, PDTTA, and BUTLA. RTI and PDTTA have serializable roots. BUTLA returns PDTA, whose class and interfaces do not implement serialization. The existing writer therefore supplies no valid BUTLA saved-model baseline.

**Alternatives** — Package wildcards and accepting any model interface implementation would admit classes unrelated to the CLI model graph. Restricting to RTI alone would remove the persisted PDTTA path.

## Loading controls

**Decision** — Use an exact per-stream filter with graph limits, a hard byte budget on reads, and a worker with a finite heap and loading deadline. Use the JDK 21 stream filter directly and preserve rejection from an existing process policy. JDK 21 is the agreed minimum and matches CI.

**Rationale** — Concrete strings can bypass filter callbacks, so enforce an independent read limit. Inspection of the pinned Trove 3.0.3 `TIntObjectHashMap.readExternal` bytecode found that it passes a stream-provided integer to `setUp`, which makes a filter-only array ceiling insufficient. The worker heap bounds Java-heap allocation even when a collection allocates internally. Include a harmless custom-collection case that exhausts the test worker budget and a controlled slow-load case that hits the deadline. This is resource containment. Provenance and an exact content policy remain required.

**Alternatives** — A JVM-wide property would affect unrelated readers. A post-load type check would run too late. Replacing the file format would lose the current compatibility baseline.

References: [Oracle ObjectInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputStream.html).
