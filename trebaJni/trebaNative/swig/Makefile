# Cygwin support
ifeq ($(OS),Windows_NT)
EXE   = .exe
SOEXT = dll
else
EXE   =
SOEXT = so
endif

CC  = gcc
LD  = gcc

# Standard definitions
CFLAGS    = -Wall -c
CXXFLAGS  = $(CFLAGS)
EASYFLAGS = -c
LDFLAGS   = -o
SOFLAGS   = -shared -Wl,-Bdynamic -o

# Because it is not defined in my PATH, I need to define by hand
ifeq ($(OS),Windows_NT)
JAVAC= /cygdrive/c/Program\ Files/Java/jdk1.5/jdk/bin/javac.exe
JAVAX= /cygdrive/c/Program\ Files/Java/jdk1.5/jdk/bin/java.exe
JINC= -I/cygdrive/c/Program\ Files/Java/jdk1.5/jdk/include -I/cygdrive/c/Program\ Files/Java/jdk1.5/jdk/include/win32
else
JAVAC=javac
JAVAX=java
JINC= -I/usr/java/latest/include -I/usr/java/latest/include/linux
endif

# My project definition (same name in .i)
MYMOD = treba

# This is inelegant but $(shell perl) did not work so ...
PERL_INC = $(shell ./perllink.pl)
PERL_LDL = $(shell ./perllink.pl LD)

# targets and sources
SRCS = $(MYMOD)_perl_wrap.c treba.c
INCS = treba.h
OBJS = $(MYMOD)_perl_wrap.o treba.o

JAVAS= test.java
JDEPS= $(MYMOD)_java_wrap.c treba.c
JOBJS= $(MYMOD)_java_wrap.o treba.o

# Main target
all: $(MYMOD).$(SOEXT) $(MYMOD)_java.$(SOEXT) $(MYMOD)_main.class
	@ echo "Try % perl test.pl"
	@ echo "Try % $(JAVAX) -Djava.library.path=./ $(MYMOD)_main"


# the needed .so for the pm to work
$(MYMOD).$(SOEXT): $(OBJS)
	$(LD) $(SOFLAGS) $@ $^ $(PERL_LDL)
        
$(MYMOD)_java.$(SOEXT): $(JOBJS)
	$(LD) $(SOFLAGS) $@ $^ 
	@ /bin/ln -s $(MYMOD)_java.$(SOEXT) ./lib$(MYMOD)_java.$(SOEXT)



#+
# SWIG dependencies
#-

# the creation of the perl module wrappers are made by swig. a .pm will be
# created as well
$(MYMOD)_perl_wrap.c: $(MYMOD).i $(INCS)
	swig -perl5 -o $@ $<

$(MYMOD)_java_wrap.c: $(MYMOD).i $(INCS)
	swig -java -o $@ $<



# XXX_wrap.cxx will be generated from the .i, .i was created
# once in "lazzy" mode. Compile using langage specific
$(MYMOD)_perl_wrap.o: $(MYMOD)_perl_wrap.c
	$(CC) $(EASYFLAGS) $< -o $@ $(PERL_INC)

$(MYMOD)_java_wrap.o: $(MYMOD)_java_wrap.c
	$(CC) $(EASYFLAGS) $(JINC) $<

$(MYMOD)_main.class: test.java $(MYMOD).java
	$(JAVAC) $?


# Global
.c.o:
	$(CC) $(CFLAGS) $< -o $@


clean:
	/bin/rm -f *.o
	/bin/rm -f *.class
	/bin/rm -f *_wrap.c
	/bin/rm -f *~ *.*~
	/bin/rm -f $(MYMOD)*.java
	/bin/rm -f *.$(SOEXT)
	/bin/rm -f $(MYMOD).pm

realclean: clean