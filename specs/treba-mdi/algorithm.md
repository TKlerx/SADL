# Minimal Divergence Inference in SADL

MDI learns a probabilistic deterministic finite automaton from positive, untimed event sequences. It starts with a frequency prefix tree. Counts include both outgoing events and the decision to stop at a state. For each candidate merge, fold shared successor branches to keep the automaton deterministic, then compare the proposed model with the current model.

Let A0 be the initial prefix tree, A1 the current model, and A2 the model after a candidate merge and fold. The published merge rule is

```text
score = (KL(A0 || A2) - KL(A0 || A1)) / (states(A1) - states(A2))
accept if score < alpha
```

The KL terms compare complete sequence probabilities, including stopping. Equivalently, compute the empirical mean of log(P_A1(sequence) / P_A2(sequence)) over training observations, weighting duplicates by their counts. This gives a straightforward reference calculation for small cases. A candidate must remove at least one state. Equality at the cutoff rejects the merge.

[Original MDI publication](https://research.dial.uclouvain.be/entities/publication/efe4d816-a786-4e84-b38b-79ab112dafe8) and [thesis §2.4.2.2, equations 2.20–2.23](https://digital.ub.uni-paderborn.de/hs/download/pdf/2445002#page=40).

## Vendored Treba behavior

`TrebaPdfaLearner` sorts and deduplicates observations, sets the minimum visit count and smoothing prior, and dispatches MDI directly to `dffa_mdi`. That routine uses red/blue selection, tests red candidates in order, accepts the first score strictly below alpha, and folds the blue state into the selected red state. It uses the visit cutoff and has no ALERGIA-style recursion flag. Smoothing applies when converting counts into the output WFSA.

The active native scorer is `dffa_mdi_score2`. It traverses blue continuations, weights stopping contributions by blue termination counts divided by total observations, uses an absolute difference of log probabilities, and divides by its count of paired states. This differs from the signed empirical likelihood change in the published rule.

Two details need explicit reference cases. In `dffa_mdi_parallel`, a blue stopping state without matching red termination can reach a read of uninitialized `frbprob`. The sibling loop also updates path accumulators in place across siblings. Record compiler and flags, compare fresh processes and optimization levels, and reserve normative assertions for defined behavior. A repeated output from an undefined path is an observation from one build.

[Native scorer and red/blue loop](../../trebaJni/trebaNative/swig/dffa.c) and [SADL JNI dispatch](../../PDTTA-core/src/sadl/modellearner/TrebaPdfaLearner.java).
