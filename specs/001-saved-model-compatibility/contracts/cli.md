# CLI compatibility contract

Source of truth for existing behavior is the [migrated CLI spec](../../train-to-test-cli/spec.md).

| Invocation | Inputs | Success observed by the fixture |
| --- | --- | --- |
| `train RTI` | `-in` fixed timed input, `-out` temporary model, explicit RTI settings | Exit zero and nonempty model file at the requested path. |
| `test` | `-model` fresh or reference model, `-testSeqs` labeled data, `-trainSeqs` fixed training data, explicit detector settings | Exit zero and one newly written semicolon-delimited row in `sadl_test_res.csv` in the process working directory. Fields 7 through 10, numbered from 1, match TP, FP, TN, and FN. |

Apply the same evaluation assertions to the unusable stand-in and require failure. A zero exit with missing output or mismatched counts fails the fixture. The negative case establishes test sensitivity without defining a public error-message or exit-code contract.
