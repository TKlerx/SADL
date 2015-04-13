swig -java -package treba  treba.i
gcc -fpic -ffast-math -c treba.c treba_wrap.c dffa.c gibbs.c observations.c io.c -I/usr/lib/jvm/java-1.8.0-openjdk/include -I/usr/lib/jvm/java-1.8.0-openjdk/include/linux 
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/timo/workspace/PDTTA/swig
gcc -shared treba.o treba_wrap.o dffa.o gibbs.o observations.o io.o -o libtreba.so -lm -lpthread -lgsl -lgslcblas
#javac test.java
#java test
