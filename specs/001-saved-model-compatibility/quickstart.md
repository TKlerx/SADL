# Validate the saved-model fixture

Use Linux, JDK 21, Maven, GCC, and GSL development headers. Set `JAVA_HOME` and follow the [repository validation steps](../../README.md).

```sh
bash trebaJni/trebaNative/swig/treba_jni.sh
mvn -B -f trebaJni/trebaNative/pom.xml install
mvn -B -pl PDTTA-core -am -Dtest=SavedModelCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl PDTTA-core -am -Dtest=SavedModelCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B test
```

Both targeted runs must match the four frozen counts for each model in fresh directories. Before declaring the fixture green, demonstrate that its reference-path assertions fail against the unusable temporary stand-in. The full run must discover the fixture through normal Surefire execution. See the [CLI contract](contracts/cli.md) for the expected artifacts.

## Recorded validation

- Red came first. The unusable model reached the loader, which rejected stream header `00010203`. The targeted check failed at the CLI exit assertion. An additional run with empty evaluation input exited zero and failed at the missing-result assertion.
- Two consecutive targeted runs passed both paths. The frozen counts were TP 1, FP 0, TN 1, FN 0.
- The full Linux reactor passed in [GitHub CI](https://github.com/TKlerx/SADL/actions/runs/41923981680). It ran 60 tests (59 core and one JNI), including both fixture cases, with zero failures, errors, or skips. CI completed the full run after the local Docker backend disconnected.
