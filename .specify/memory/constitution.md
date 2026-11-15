# SADL Constitution

## Project and module boundaries

SADL is a sequence-based anomaly detection library and CLI. The Maven reactor builds Java modules targeting JDK 25 and packages a native C library.

- `PDTTA-core/src/sadl` contains the CLI and detection code. Core tests belong in `PDTTA-core/test/sadl`.
- `trebaJni/src/main/java/treba` contains the Java binding and native loader. JNI tests belong in `trebaJni/src/test/java/treba`.
- `trebaJni/trebaNative/swig` contains the native implementation and build script. Keep dependencies directed from core to the Java binding to the native artifact.

## Repository conventions

Follow nearby Java naming and formatting. Classes and their files generally use PascalCase, and methods and variables use lower camel case. Match the existing package layout. Branch names and commit prefixes vary.

## Test behavior first

For behavior changes and bug fixes, follow the [project TDD skill](../../.agents/skills/tdd/SKILL.md). Use the existing JUnit 4 tests. Add a failing test at the agreed public boundary, confirm the reason it fails, then make the smallest change that passes it.

## Preserve observable behavior

Treat CLI commands, timed input, and persisted models as compatibility boundaries. State any intended changes to those boundaries in the spec and verify them with representative fixtures.

## Verify the native path

Use the [CI workflow](../../.github/workflows/ci.yml) as the full validation baseline. It builds the JNI library against GSL on Linux with JDK 25 and runs the Maven reactor tests. Use targeted tests during development and the full workflow before merging.

## Governance

Record deviations in the implementation plan and review them through a pull request to protected main.

**Version** 1.0.0 | **Established** 2026-11-04
