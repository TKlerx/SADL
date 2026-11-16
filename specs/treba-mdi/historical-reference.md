# Historical Treba MDI reproduction

The reference is the unmodified JNI build from [`c80ede9d8e3dbba090db9d494a3f43a392a8cb87`](https://github.com/TKlerx/SADL/tree/c80ede9d8e3dbba090db9d494a3f43a392a8cb87). In the experiment, the x86-64 Linux GCC 13.3.0 build produced byte-identical native output to the earlier shared-corpus capture. See [shared inputs and capture commands](shared-parity.md).

The native scorer can read `frbprob` uninitialized when a blue state has stopping observations and red has none or is absent. In the inspected optimized binary, that path reads a stack slot without assigning it first. The historical behavior therefore depends on more than the automaton and input counts.

The Java experiments restored the native accumulator reset and omitted the candidate-side log step on an unmatched arc. One used zero for the missing score. A second modeled reuse of the compiled function's stopping-score slot by recursion depth, treating the slot as zero after traversal reused it for a pointer. The third retained those slots across candidate evaluations within the same training run. These experiments were isolated from the shipped learner.

| Java scorer | Historical split states | Acceptance mismatches | Event vector mismatches | Time vector mismatches |
| --- | ---: | ---: | ---: | ---: |
| Unmodified native reference | 39 | 0 | 0 | 0 |
| Current deterministic Java `MDI_TREBA` | 5 | 12 | 5,000 | 4,558 |
| Java zero-score approximation | 32 | 0 | 4,667 | 1,015 |
| Java stack-slot approximation | 32 | 0 | 4,667 | 1,015 |
| Java cross-candidate stack-slot prototype | 39 | 0 | 0 | 0 |

The comparison covers all 5,000 evaluation traces, with a score tolerance of `1e-12`. The stack-slot approximation also reproduced the small native one-state automaton byte for byte. The historical split establishes that matching acceptance alone is insufficient to verify reproducibility. Legacy MDI now selects the historical Java mode. `MDI_PAPER` stays independent of these experiments.

In a separate decision trace, the stack-slot approximation agreed with the native trace for the first 23 rounds, including the original round-12 divergence. The first remaining difference was round 24. Both examined the arc for symbol index 13 from red insertion index 2, with 873 visits. C promoted the candidate, and Java merged it into red index 2. The uninstrumented native output was used for the numerical comparison above.

### Round 24 and cross-candidate reuse

GDB broke at the instruction that subtracts the stopping score in the original shared library. In the third candidate test of round 24, native red state 2 against blue state 205, the three stopping-score reads in red/blue pairs `5/208`, `107/210`, and `207/213` all read `0xc029460210ab8a7c` from the stack. This is `-12.636734505596216`. The same value appeared in two debugger runs with address randomization enabled. A direct C driver using the same observation IDs, cutoff, and library also learned 39 states.

Retaining the Java prototype's slots across candidates recovered all 57 traced decisions and the native 39-state result. All 5,000 evaluation traces matched in acceptance and event and timing vectors within `1e-12`. The small native automaton matched byte for byte. This demonstrates reproduction of the measured fixture with the pinned build. It does not establish reproduction for other corpora, parameters, or native toolchains.

To recreate the experiment, apply [the Java prototype patch](historical-java-prototype.patch) in a disposable checkout of the pinned revision above and run `SharedTrebaReferenceCapture` as described in [shared-parity.md](shared-parity.md). Keep the C build unmodified. The patch changes the experimental Java scorer only.

## Opt-in Java mode

`-mergeMethod MDI_TREBA_HISTORICAL` selects the cross-candidate implementation in PDTTA training and the PDFA factory. It retains a map of observed stopping-score slots per training call. A slot starts at zero, is updated by a defined paired stop, and is cleared when native arc traversal would reuse it for a pointer. The Java CLI mode uses a zero smoothing prior. The comparison harness also exercises nonzero smoothing through the learner API.

For the expanded comparison, apply both the Java prototype patch above and the [capture harness patch](historical-capture.patch) in a disposable checkout of the pinned revision. Build the unmodified native library using that checkout’s README. Copy [the comparator](compare-historical-captures.py) into that checkout’s `specs/treba-mdi` directory, then run the following from the checkout root. The native binary used in the expanded run matched the earlier GCC 13.3.0 binary byte for byte, with SHA-256 `84f71a64eae4aad8f84e658548ca75e3caa85d47cee769e633cc7531d28a2869`.

```sh
out=$(mktemp -d)
for spec in 1:0.05:0:3 2:0.05:0:3 3:0.05:0:3 4:0.05:0:3 5:0.05:0:3 1:0.1:0:3 1:0.05:0.5:3 2:0.05:0:5; do
    IFS=: read -r corpus alpha prior visits <<<"$spec"
    mvn -B -Dtest=SharedTrebaReferenceCapture -Dsurefire.failIfNoSpecifiedTests=false \
        -Dtreba.reference.historical=true -Dtreba.reference.corpus="smac_mix_type$corpus.txt" \
        -Dtreba.reference.alpha="$alpha" -Dtreba.reference.prior="$prior" \
        -Dtreba.reference.visits="$visits" -Dtreba.reference.output="$out/$corpus-$alpha-$prior-$visits" test
done
python3 specs/treba-mdi/compare-historical-captures.py "$out"/*
```

The comparator reports native and Java state counts and automaton agreement for the small and historical inputs, followed by the numbers of traces that differ in acceptance, event probabilities, and timing scores. It compares topology independently of state numbering and uses absolute tolerance `1e-12`.

On Linux Temurin 25.0.4.1, both engines learned the same reachable automaton in every run.

| Corpus | Alpha | Prior | Minimum visits | States |
| --- | ---: | ---: | ---: | ---: |
| Type 1 | 0.05 | 0 | 3 | 39 |
| Type 2 | 0.05 | 0 | 3 | 42 |
| Type 3 | 0.05 | 0 | 3 | 52 |
| Type 4 | 0.05 | 0 | 3 | 30 |
| Type 5 | 0.05 | 0 | 3 | 5 |
| Type 1 | 0.1 | 0 | 3 | 28 |
| Type 1 | 0.05 | 0.5 | 3 | 39 |
| Type 2 | 0.05 | 0 | 5 | 124 |

All eight runs had zero acceptance, event probability, or timing score mismatches. The small fixture also agreed with both prior values. The focused Java regression suite passed 28 tests, including the historical factory option and repeated training on the same learner. The scoped PMD and CPD checks passed.
