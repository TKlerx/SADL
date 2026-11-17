# Minimal Divergence Inference in SADL

MDI learns a probabilistic deterministic finite automaton from positive, untimed event sequences. It starts with a frequency prefix tree. Counts include both outgoing events and the decision to stop at a state. For each candidate merge, fold shared successor branches to keep the automaton deterministic, then compare the proposed model with the current model.

Let A0 be the initial prefix tree, A1 the current model, and A2 the model after a candidate merge and fold. The published merge rule is

```text
score = (KL(A0 || A2) - KL(A0 || A1)) / (states(A1) - states(A2))
accept if score < alpha
```

The KL terms compare complete sequence probabilities, including stopping. Equivalently, compute the empirical mean of log(P_A1(sequence) / P_A2(sequence)) over training observations, weighting duplicates by their counts. This gives a straightforward reference calculation for small cases. A candidate must remove at least one state. Equality at the cutoff rejects the merge.

[Original MDI publication](https://research.dial.uclouvain.be/entities/publication/efe4d816-a786-4e84-b38b-79ab112dafe8) and [thesis §2.4.2.2, equations 2.20–2.23](https://digital.ub.uni-paderborn.de/hs/download/pdf/2445002#page=40).

## SADL's paper mode

`MDI_PAPER` implements the published signed divergence criterion. SADL selects candidates in red/blue order and only considers blue states that meet the configured minimum visit count. Candidate order and the visit cutoff can affect the learned automaton even when the scoring rule is identical.

## Vendored Treba behavior

`TrebaPdfaLearner` sorts and deduplicates observations, sets the minimum visit count and smoothing prior, and dispatches MDI directly to `dffa_mdi`. That routine uses red/blue selection, tests red candidates in order, accepts the first score strictly below alpha, and folds the blue state into the selected red state. It uses the visit cutoff and has no ALERGIA-style recursion flag. Smoothing applies when converting counts into the output WFSA.

The active native scorer is `dffa_mdi_score2`. It traverses blue continuations, weights stopping contributions by blue termination counts divided by total observations, uses an absolute difference of log probabilities, and divides by its count of paired states. This differs from the signed empirical likelihood change in the published rule.

Two details need explicit reference cases. In `dffa_mdi_parallel`, a blue stopping state without matching red termination can reach a read of uninitialized `frbprob`. The sibling loop also updates path accumulators in place across siblings. Record compiler and flags, compare fresh processes and optimization levels, and reserve normative assertions for defined behavior. A repeated output from an undefined path is an observation from one build.

[Native scorer and red/blue loop](../../trebaJni/trebaNative/swig/dffa.c) and [SADL JNI dispatch](../../PDTTA-core/src/sadl/modellearner/TrebaPdfaLearner.java).

## Java Treba mode

Select `-mergeMethod MDI_TREBA` in PDTTA training or the PDFA factory. Java uses the native red/blue order, strict cutoff, and sibling accumulator order. The [branching capture](native-branching.tsv) scored 0.025467994179750857 in 20 fresh processes under each of the original two build settings. Run the existing score probe with the argument `branching` to reproduce it.

The original C scorer has a bug. When blue can stop and red is absent or cannot stop, the code resets the candidate path accumulator and then reads the uninitialized stopping value `frbprob`. The result can depend on the compiled binary and its execution state.

For a blue-only arc, Java carries the blue arc's conditional probability into the candidate path and uses the blue subtree's conditional stopping and continuation probabilities. When both states exist and only blue can stop, Java uses the pooled stopping count divided by the pooled visit count. The frozen blue-only C score remains a historical observation. Legacy `-mergeTest MDI` still selects native Treba. See [the historical reproduction experiment](historical-reference.md).

Select `-mergeMethod MDI_TREBA_HISTORICAL` to opt into the historical Java mode. It models the pinned GCC/Linux binary's observed stack-slot reuse across candidate evaluations. Its state resets for each training invocation. This mode is intended for reproduction on verified corpora and settings. Use the pinned JNI environment for exact historical replay outside that evidence.

The frozen PDTTA comparison crosses this undefined scoring path. The Java rule yields three states and an event vector of `[0.5, 0.5, 1, 1]` on the normal trace. The native capture has one state and yields `[0.5, 0.25, 0.5, 0.25]`. Both accept the normal trace and reject the unseen symbol. Java fits each observed transition separately on this fixture, so its three time scores are approximately 0.335762. Native pools the two `a` positions and assigns about 0.343829 at those positions.

The [weighted branching PDFA](native-branching-pdfa.tsv) captures two copies of `ab` and one of `ac`, with alpha zero and minimum visits one. Java and C have matching topology, arc probabilities and stopping probabilities. A zero cutoff rejects nonnegative merge scores, so this capture checks the weighted prefix tree and output conversion. The cutoff and branching score probes exercise decisions at the exact captured C score and its two adjacent doubles. The Maven capture also writes the branching PDFA using the output path with `.branching.tsv` appended.

Run `mvn -B -Ptreba-quality verify` for scoped PMD and CPD checks. PMD flags method cyclomatic complexity of 12 or higher. CPD flags duplicated spans of 75 tokens or more. Both checks cover the Java Treba and MDI learners. Run `mvn -B -Ptreba-parity-mutation '-DtargetClasses=sadl.modellearner.MdiTrebaLearner*' test-compile` for the focused MDI mutation run.

The [corrected native reference experiment](fixed-reference.md) records the separate C comparison for the deterministic Java scorer.
