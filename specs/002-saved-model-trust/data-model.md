# Saved model contents and limits

## Root and nested contents

| Scope | Admission rule |
| --- | --- |
| Roots | Exact `sadl.models.pdrta.PDRTA` and `sadl.models.PDTTA`. The PDTTA superclass `sadl.models.PDFA` is a nested/descriptor dependency, not an independently supported root. |
| Frozen PDRTA SADL descriptors | `sadl.models.pdrta.PDRTA`, `PDRTAInput`, `PDRTAState`, `StateStatistic`, `Interval` in that package, plus `sadl.input.TimedInput`, `sadl.input.TimedWord`, and `sadl.constants.ClassLabel`. |
| Frozen PDRTA Java descriptors | `java.lang.Enum`, `Integer`, `Number` in that package, plus `java.util.ArrayList`, `java.util.TreeMap`, and `int[]` and `double[]`. Permit concrete strings as model data under the byte cap. |
| Frozen PDRTA Guava descriptors | Exact classes in `com.google.common.collect`: `TreeMultimap`, `NaturalOrdering`, `AbstractSortedKeySortedSetMultimap`, `AbstractSortedSetMultimap`, `AbstractSetMultimap`, and `AbstractMapBasedMultimap`. |
| Frozen PDRTA Trove descriptors | `gnu.trove.list.array.TIntArrayList`, `gnu.trove.list.linked.TIntLinkedList`, `gnu.trove.map.hash.TIntObjectHashMap`, `TObjectDoubleHashMap` and `TObjectIntHashMap` in that map package, and `gnu.trove.impl.hash` ancestors `THash`, `TIntHash`, `TObjectHash`, and `TPrimitiveHash`. |
| PDTTA contents to verify | `PDTTA` and `PDFA`, transition and timing-distribution classes, the identity and Monte Carlo estimator graphs, supported JSAT kernel implementations, and the exact Java and Trove storage types those graphs use. See the source-derived candidates below. |
| Runtime collection allocations | Admit only exact array types demonstrated by trusted fixture filter traces, including scratch arrays needed by approved readers. Check lengths independently and filter every nested object. |

PDTTA candidates include `sadl.structure.Transition`, `ZeroProbTransition`, `sadl.tau_estimation.IdentityEstimator`, `MonteCarloEstimator`, `sadl.integration.MonteCarloIntegration`, `MonteCarloPoint`, JSAT `SingleValueDistribution` and `MyKernelDensityEstimator` with its serialized function and kernel classes, `java.util.Random`, `HashSet`, `HashMap`, `Collections$SynchronizedMap`, and Trove `TIntDoubleHashMap`, `TIntIntHashMap`, `TIntHashSet` and their exact ancestors. Check Monte Carlo storage and supported kernel variants on real artifacts. This is an inventory to verify. An unobserved candidate is not an admission rule.

Reject all other roots, subclasses, proxy classes, unlisted descriptors and array types. Review custom readers and any replacement-object behavior before admitting new entries. Keep the final exact inventory with its fixture evidence.

## Verified PDTTA capture inventory

Baseline revision `278b53a055dd057cac98d5e56f84097bb5f316f2`, Temurin JDK 21.0.13.9. The trusted capture test ran `train PDTTA` and `test` for DENSITY and MONTE_CARLO with BIWEIGHT, EPANECHNIKOV, GAUSS, TRIWEIGHT, UNIFORM and ESTIMATE, plus one constant-timing model per estimator. It used the existing timed inputs for variable timing and twelve copies of `(a,3) (b,5) (a,3)` for constant timing. The test source records exact CLI flags. Each evaluation yielded TP 1, FP 0, TN 1, FN 0.

- **Common to all 14 PDTTA captures** — `[Ljava.lang.Object;`, `[Ljava.util.Map$Entry;`, `gnu.trove.impl.hash.THash`, `gnu.trove.impl.hash.TIntDoubleHash`, `gnu.trove.impl.hash.TIntHash`, `gnu.trove.impl.hash.TIntIntHash`, `gnu.trove.impl.hash.TObjectHash`, `gnu.trove.impl.hash.TPrimitiveHash`, `gnu.trove.list.array.TIntArrayList`, `gnu.trove.map.hash.TIntDoubleHashMap`, `gnu.trove.map.hash.TIntIntHashMap`, `gnu.trove.map.hash.TObjectIntHashMap`, `gnu.trove.set.hash.TIntHashSet`, `java.lang.Enum`, `java.util.ArrayList`, `java.util.Collections$SynchronizedMap`, `java.util.HashMap`, `java.util.HashSet`, `java.util.Random`, `jsat.distributions.ContinuousDistribution`, `jsat.distributions.Distribution`, `sadl.constants.ClassLabel`, `sadl.input.TimedInput`, `sadl.input.TimedWord`, `sadl.models.PDFA`, `sadl.models.PDTTA`, `sadl.structure.Transition`, `sadl.structure.ZeroProbTransition`.
- **DENSITY** — Adds `sadl.tau_estimation.IdentityEstimator`.
- **MONTE_CARLO** — Adds `[Ljava.util.concurrent.ConcurrentHashMap$Segment;`, `java.util.concurrent.ConcurrentHashMap`, `java.util.concurrent.ConcurrentHashMap$Segment`, `java.util.concurrent.locks.AbstractOwnableSynchronizer`, `java.util.concurrent.locks.AbstractQueuedSynchronizer`, `java.util.concurrent.locks.ReentrantLock`, `java.util.concurrent.locks.ReentrantLock$NonfairSync`, `java.util.concurrent.locks.ReentrantLock$Sync`, `sadl.integration.MonteCarloIntegration`, `sadl.tau_estimation.MonteCarloEstimator`. Variable timing also adds `sadl.integration.MonteCarloPoint` and its array type.
- **Variable timing** — Adds `double[]`, `jsat.distributions.empirical.MyKernelDensityEstimator`, its `$1` inner class, and the selected `jsat.distributions.empirical.kernelfunc` class. The five explicit selections map to `BiweightKF`, `EpanechnikovKF`, `GaussKF`, `TriweightKF`, and `UniformKF`. ESTIMATE selected `GaussKF` for this input.
- **Constant timing** — Adds `jsat.distributions.SingleValueDistribution`.
- **Filter-only arrays** — The verified traces include `Object[]` and `Map.Entry[]` for collection internals. These do not enter the wire descriptor allowlist.

The exact union of reviewed RTI and PDTTA descriptors is in `PDTTA-core/src/sadl/run/SavedModelLoader.java`. Trove custom readers and JDK collection reconstruction run only within the worker heap and loading deadline.

## Initial fixed limits

| Resource | Ceiling |
| --- | --- |
| Regular file size and bytes read from the opened stream | 16 MiB (16,777,216 bytes) |
| Deserialization depth | 64 |
| Stream references | 250,000 |
| Length of any filter-visible array | 250,000 elements |
| Worker Java heap | 512 MiB |
| Worker startup and load to validated root | 30 seconds |

Numeric graph and byte limits are inclusive. The heap is a process budget, including internal collections, and does not describe total process memory. The deadline applies through successful loading and is separate from evaluation duration. Verify the headroom with representative fresh RTI and PDTTA models and the frozen fixture. Add benign boundary tests for each limit, using injectable lower test limits where needed to keep CI bounded. Production defaults stay fixed and documented.
