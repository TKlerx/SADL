# Shared Treba learner fixtures

Each Java learner trains on the same inputs as its native counterpart. ALERGIA uses the Java native-parity mode with the successor check enabled. MDI uses the Java Treba scorer. Both use zero smoothing.

## Small fixture with actual merges

The shared input has 12 traces of lengths one through three, with duplicate traces, shared prefixes, and stops at different depths. With alpha 0.8 and a one-visit minimum, both implementations of ALERGIA take the same five decisions and perform four merges. Both implementations of MDI take the same three decisions and perform three merges. The resulting ALERGIA automata have two states and the MDI automata have one. The Java regression compares every arc and stopping probability with an absolute tolerance of `1e-12`.

[Input](../../PDTTA-core/testResources/pdtta/treba-shared-small.inp) · [ALERGIA automaton](alergia-small-native.tsv) · [MDI automaton](mdi-small-native.tsv) · [Decision trace](shared-merge-decisions.tsv)

## Historical train/test split

The existing `smac_mix_type1.txt` corpus supplies 10,000 training traces and 5,000 evaluation traces. Evaluation lengths range from 4 to 55 events. All learners use alpha 0.05 and a three-visit minimum. A score mismatch means different vector lengths or any component differing by more than `1e-12`.

| Java vs native | States, Java / C | Acceptance mismatches | Event score mismatches | Time score mismatches | Training seconds, Java / C |
| --- | ---: | ---: | ---: | ---: | ---: |
| ALERGIA | 563 / 563 | 0 | 0 | 0 | 0.933 / 0.894 |
| MDI | 5 / 39 | 12 | 5,000 | 4,558 | 0.049 / 0.076 |

The largest ALERGIA event probability difference was `1.11e-16`; timing vectors agreed exactly. The two ALERGIA runs agreed on all of the first 200 decisions traced. For MDI the first 11 decisions agreed. At round 12 both examined the root's outgoing symbol index 6 with 78 visits. Native promoted that state and Java merged it into the root. The training timings include PDTTA timing fitting, come from one run in a Linux Temurin 21 container, and are illustrative.

### Round 12 MDI score

| Scorer | Blue-only stopping contributions | Paired states | Score | Decision at alpha 0.05 |
| --- | ---: | ---: | ---: | --- |
| Instrumented native C | 4 | 1 | 0.15634663489436218 | Promote |
| Java | 4 | 1 | 0.02121217736757223 | Merge into root |
| C with Java's blue-only handling for this candidate | 4 | 1 | 0.021212177367572228 | Merge into root |

All of the score in each run came from stops with no red counterpart. The native branch omits the blue-only arc from the candidate path and reaches the potentially uninitialized `frbprob` read at those stops. For the controlled C run, only the root candidate in round 12 used the Java rule. The experiment carried the blue-only arc's conditional log probability forward and initialized each blue-only stop with the carried candidate log probability plus its conditional log stopping probability. Earlier candidates and all observations stayed the same. This was enough to flip the decision and match Java within floating-point rounding. The baseline instrumentation reproduced all eight original output files byte for byte. These are observations of the pinned build, because the original C score depends on an uninitialized read. These results predate the historical Java MDI mode. See [historical MDI reproduction](historical-reference.md) for the rerun instructions and expanded parity results.

## Reproduce

Use a checkout of the [pinned native revision `c80ede9`](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87). Rebuild and install Treba using that checkout’s README, then run from its root.

```sh
out=$(mktemp -d)
mvn -B -Dtest=SharedTrebaReferenceCapture -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtreba.reference.output="$out" test
mvn -B -Dtest=SharedTrebaParityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The capture writes all per-trace results to the selected directory and prints training times. The regression test uses the frozen small native automata and runs with Java alone. The decision trace was collected by temporarily logging the incoming parent index, symbol, visit count and selected red-state index in both loops. Red-state indices refer to insertion order. A merge index of minus one denotes promotion. The traced run reproduced all eight output files byte for byte.
