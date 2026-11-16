# Opt-in Java mode for native Treba ALERGIA parity

## Behavior

Proposed CLI selector `train PDTTA … -mergeMethod TREBA_NATIVE_PARITY`. Select it in `PdttaFactory` at the `PdfaLearner` boundary and expose the same learner through `PdfaDefaultFactory` for the PDFA experiment path. Reuse `-mergeAlpha`, `-recursiveMergeTest`, and `-mergeT0`. The initial parity scope uses a zero smoothing prior. Keep the existing `TREBA` and `ALERGIA_PAPER` behaviors and default. The opt-in learner returns the existing `PDFA` used by `PdttaLearner`, with no native calls or new serialized model types. Other Treba statistical tests and removal of the JNI module are separate work.

## What the references pin

- `native-depth.csv` captures the C merge decisions for three pairs in the same counted automaton. The rows place the first mismatch at depth two, one, and zero. With the recursive option on, native accepts the depth-two pair and rejects the other two. Paper mode should reject the depth-two pair when recursively checking compatibility.
- `native-benchmark.tsv` captures the existing `smac_mix_type1` split, full native transition and stopping probabilities, and acceptance and per-event probability vectors on five evaluation traces. Native Treba learns 356 states and Java `TREBA` learns 70. Native rejects evaluation trace 0 where Java accepts it. Both use `alpha=0.05`, recursive testing, and `t0=0`. The native smoothing prior is zero.

## Implementation and validation order

### Benchmark trace

Both implementations promote the state for the first symbol `9` in round one. At round two, native Treba selects the prefix `9 9` (numeric symbol 0 from the red parent, 2,106 visits) and promotes it. The existing Java `TREBA` queue instead selects `9 8`. Native first merges in round six (native blue state 3116 into red state 1670). Java first merges its fifth candidate, the prefix `9 13`, into the state for `9 10`. State identifiers are local to each learner. The different frontier order precedes any merge or fold.

1. Start with a failing JUnit 4 parity check at the `PdfaLearner` boundary and an opt-in PDTTA CLI check that trains, saves, and tests a model. Read the frozen C capture in ordinary Java tests so the parity checks run on hosts without JNI. Add the depth witness with both recursion settings and keep the paper expectation explicit.
2. Compare the first divergent merge on the pinned benchmark before changing the Java learner. Trace prefix counts, symbol indexing, state-pair frequencies and each selected red/blue pair on both sides. The C loop chooses the first eligible blue state using total visits `>= t0`, tries red states in insertion order and stops at the first match. It then folds the merged branches and rebuilds the frontier in red-state and symbol order. The current Java loop uses a queue, can keep searching red states after a merge, and applies a strict `> t0` test to an incoming edge count when extending the frontier. These are candidate causes of the benchmark gap.
3. Implement the native decisions in the opt-in learner. Reuse compatible SADL input and model types and verify how the C observation sort and duplicate counts, direct successor tests, fold update order, reachable-state pruning, and WFSA-to-PDFA conversion affect predictions. Capture cutoff, duplicate-order, and empty or unseen trace cases before claiming parity outside the two initial references.
4. Require exact acceptance and equivalent reachable topology after mapping states from the start state in symbol order. Compare transition, stopping, and evaluation probabilities with absolute tolerance `1e-12` and relative tolerance `1e-9` as the initial gate. Investigate any larger difference at the first divergent merge or probability conversion, and justify any tolerance change using measured native output. The 356-state assertion is a diagnostic in addition to these checks.
5. Exercise both factory paths, run the opt-in PDTTA CLI case without a usable native library, and rerun the unchanged saved-model fixtures and full Linux Maven reactor with the native library rebuilt. Run under Temurin 21 and 25 where the CI matrix is available.
