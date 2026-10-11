#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK before building the JNI library}"

# Reuse the checked-in JNI wrapper; do not regenerate the Java API during builds.
gcc -O2 -fPIC -fcommon -ffast-math -c treba.c treba_wrap.c dffa.c gibbs.c observations.c io.c \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"
gcc -shared treba.o treba_wrap.o dffa.o gibbs.o observations.o io.o \
  -Wl,-z,noexecstack -o libtreba.so -lm -lpthread -lgsl -lgslcblas
cp -f libtreba.so ../native_libs/libtreba.so
