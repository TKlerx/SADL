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
legacy TreeMap serialization; production JVM settings and the type allowlist
are unchanged. JDK 8 remains supported without that module option.

For the complete native tests, use Linux with a JDK, GCC and GSL development
headers (`build-essential libgsl-dev` on Ubuntu). Set `JAVA_HOME`, then run:

```sh
bash trebaJni/trebaNative/swig/treba_jni.sh
mvn -B -f trebaJni/trebaNative/pom.xml install
mvn -B test
```

GitHub CI runs these commands without disabling tests or setting `TRAVIS`
(some historical tests return early when that variable is set). Windows can
run the Java tests but cannot validate the Linux JNI library.
