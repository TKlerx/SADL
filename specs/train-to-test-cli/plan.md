# Reconstructed implementation plan

This describes the existing Maven/JDK implementation. The feature lives in `PDTTA-core`; native requirements depend on the learner selected.

| Stage | Existing implementation |
| --- | --- |
| Dispatch | [SADL.java](../../PDTTA-core/src/sadl/run/SADL.java) registers the public commands with JCommander and dispatches their options. |
| Train | [TrainRun.java](../../PDTTA-core/src/sadl/run/commands/TrainRun.java) selects a learner factory, parses timed sequences, trains the model, and creates the output directory before saving. |
| Persist and reload | [IoUtils.java](../../PDTTA-core/src/sadl/utils/IoUtils.java) uses Java object streams for the CLI handoff. [TestRun.java](../../PDTTA-core/src/sadl/run/commands/TestRun.java) reads the model, parses evaluation input, configures the detector, and writes the result row. |
| Evaluate | [Evaluation.java](../../PDTTA-core/src/sadl/evaluation/Evaluation.java) scores labeled sequences. [ExperimentResult.java](../../PDTTA-core/src/sadl/experiments/ExperimentResult.java) formats counts and metrics. |

The direct path has six production classes, with parser, learner, and detector implementations beneath them. CLI flags, timed input, and the persisted model are the boundaries that couple the stages. Standard validation is the repository's Linux Maven CI with JNI built against GSL.

Evidence comes from source and existing tests. This migration did not replay a CLI invocation or establish which historical model artifacts remain compatible.
