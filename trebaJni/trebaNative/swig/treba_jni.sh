swig -java -package treba  treba.i
gcc -O2 -fpic -ffast-math -c treba.c treba_wrap.c dffa.c gibbs.c observations.c io.c -I/usr/lib/jvm/java-1.8.0-openjdk/include -I/usr/lib/jvm/java-1.8.0-openjdk/include/linux 
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/timo/git/SADL/trebaJni/trebaNative/swig
gcc -shared treba.o treba_wrap.o dffa.o gibbs.o observations.o io.o -o libtreba.so -lm -lpthread -lgsl -lgslcblas
#javac test.java
#java test
execstack -c libtreba.so
cp -f libtreba.so ../native_libs/libtreba.so
