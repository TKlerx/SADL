# Fixed native MDI reference

The unmodified native scorer is preserved at [`c80ede9d8e3dbba090db9d494a3f43a392a8cb87`](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87) for historical reruns. The historical captures and configuration remain available at that revision. Rebuild with the recorded settings in [the original capture notes](plan.md).

This experiment compares the deterministic Java `MDI_TREBA` scorer with a corrected C reference. Historical replay of the unmodified C binary is documented separately in [historical-reference.md](historical-reference.md). The [C patch](fixed-reference.patch) and [Java capture harness patch](fixed-reference-harness.patch) apply to the pinned revision in a disposable checkout.

The patch carries the blue-only arc's conditional log probability into the candidate path and initializes its stop contribution from the blue state's conditional stopping probability. A paired state uses pooled stop and visit counts, including when only the blue state can stop. The sibling accumulator order stays the same.

## Cutoff results

| Native score probe | Fixed C score | Below, equal, above |
| --- | ---: | --- |
| Shared branch | 0.059526861155626776 | Reject, reject, accept |
| Branching with siblings | 0.025467994179750857 | Reject, reject, accept |
| Blue-only branch | 0.11552453009332421 | Reject, reject, accept |

The historical blue-only score on this build was 0.25936556631921465. The fixed score matches the independently calculated Java expectation, and the Java tests exercise the exact C cutoff and adjacent doubles for all three probes.

## Reproduce the corpus comparison

Start from the repository root on Linux. Follow the pinned checkout’s README for native prerequisites and set `JAVA_HOME`. The reference run used Temurin 25.0.4.1, GCC 13.3.0, and GSL 2.7.1.

```sh
archive="$PWD/specs/treba-mdi"
reference=$(mktemp -d)
git worktree add --detach "$reference" c80ede9d8e3dbba090db9d494a3f43a392a8cb87
cd "$reference"
git apply "$archive/fixed-reference.patch" "$archive/fixed-reference-harness.patch"
repo=$(mktemp -d)
out=$(mktemp -d)
bash trebaJni/trebaNative/swig/treba_jni.sh
mvn -B -Dmaven.repo.local="$repo" -f trebaJni/trebaNative/pom.xml install
for pair in 0:0.05 0.5:0.05 0:0 0:0.1; do
    prior=${pair%:*}
    alpha=${pair#*:}
    mvn -B -Dmaven.repo.local="$repo" -Dtest=SharedTrebaReferenceCapture \
        -Dsurefire.failIfNoSpecifiedTests=false -Dtreba.reference.output="$out/$prior-$alpha" \
        -Dtreba.reference.prior="$prior" -Dtreba.reference.alpha="$alpha" test
done
python3 "$archive/compare-reference.py" "$out"/*
```

The small shared input uses alpha 0.8 and minimum visits 1. The historical PDTTA split uses the supplied alpha and minimum visits 3. The comparator maps reachable states from the start by transition symbol and compares topology, stopping and transition probabilities, per-trace acceptance, and event and timing vectors. Numerical comparisons use absolute tolerance `1e-12`.

## Corpus results

| Historical alpha | Smoothing prior | MDI states in both engines | Acceptance, event score, and timing score mismatches |
| ---: | ---: | ---: | --- |
| 0 | 0 | 3,329 | 0, 0, 0 |
| 0.05 | 0 | 5 | 0, 0, 0 |
| 0.05 | 0.5 | 5 | 0, 0, 0 |
| 0.1 | 0 | 4 | 0, 0, 0 |

The historical topology and all transition and stopping probabilities matched in all four runs. Each run compared all 5,000 evaluation traces. The small fixture's one-state MDI automaton matched with both prior values, and ALERGIA matched its native counterpart in every run. These results establish parity with the fixed reference for the tested inputs and settings. The separate Java 25 regression run used the unmodified native artifact and passed 27 tests.
