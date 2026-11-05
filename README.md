# SADL
Sequence-based Anomaly Detection Library

## Implemented Models
* PDRTA
* PDTA
* PDTTA
* PDFA

## Implemented Automaton Inference Algorithms
* RTI+
* ALERGIA
* BUTLA
* ProDTAL

## Validation

Run `mvn -B test` with JDK 21. The test JVM opens `java.util` for XStream's
legacy TreeMap serialization. Production XStream settings and its type allowlist
are unchanged. JDK 21 is the minimum supported Java version for the build and CLI.

For the complete native tests, use Linux with a JDK, GCC and GSL development
headers (`build-essential libgsl-dev` on Ubuntu). Set `JAVA_HOME`, then run:

```sh
bash trebaJni/trebaNative/swig/treba_jni.sh
mvn -B -f trebaJni/trebaNative/pom.xml install
mvn -B test
```

GitHub CI runs the full suite with `TRAVIS` unset. Setting `TRAVIS=true`
reports the affected historical tests as skipped. Windows can
run the Java tests but cannot validate the Linux JNI library.

## Saved model trust

Supply models you trained or obtained from a trusted source whose integrity you can establish. SADL checks every file passed to the test command against its saved-model policy. The accepted roots are RTI PDRTA and PDTTA with reviewed nested contents. The exact inventory and capture evidence are in the [saved model policy](specs/002-saved-model-trust/data-model.md).

The file and read limits are 16 MiB. Loading also allows up to 64 levels of depth, 250,000 references, and 250,000 elements per filter-visible array. A separate JVM has a 512 MiB Java heap during loading and evaluation, and must finish loading within 30 seconds. On rejection the CLI exits nonzero before evaluation and leaves any existing result untouched.

The worker carries over the effective JDK serialization pattern, including stricter operator limits. Model loading fails closed if the parent uses a custom filter or factory whose behavior cannot be reproduced in the worker.
