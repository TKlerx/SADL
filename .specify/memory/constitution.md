# SADL Constitution

## Test behavior first

For behavior changes and bug fixes, follow the [project TDD skill](../../.agents/skills/tdd/SKILL.md). Use the existing JUnit 4 tests. Add a failing test at the agreed public boundary, confirm the reason it fails, then make the smallest change that passes it.

## Preserve observable behavior

Treat CLI commands, timed input, and persisted models as compatibility boundaries. State any intended changes to those boundaries in the spec and verify them with representative fixtures.

## Verify the native path

Use the [CI workflow](../../.github/workflows/ci.yml) as the full validation baseline. It builds the JNI library against GSL on Linux with JDK 21 and runs the Maven reactor tests. Use targeted tests during development and the full workflow before merging.

## Governance

Record deviations in the implementation plan and review them through a pull request to protected main.

**Version** 1.0.0 | **Established** 2026-11-04
