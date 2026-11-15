# SADL repository guidance

Use the [constitution](.specify/memory/constitution.md) and [TDD skill](.agents/skills/tdd/SKILL.md) for behavior changes and bug fixes.

| Work area | Paths |
| --- | --- |
| CLI, detection, models, and core tests | `PDTTA-core/` |
| Java native binding, loader, and JNI tests | `trebaJni/src/` and `trebaJni/pom.xml` |
| C sources, JNI wrapper, build script, and native artifact packaging | `trebaJni/trebaNative/` |

When work crosses modules, record the affected interface and the validation order in the plan. Follow the [README](README.md) and [CI workflow](.github/workflows/ci.yml) for the Linux native build and Maven tests.
