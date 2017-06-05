NAME=macenh
CLASS=com.panayotis.appenh.MacEnhancer
JNI=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/include
PACKDEST=lib

TARGET=target
SOURCES_DIR=src/main/objc
CLASS_DIR=${TARGET}/classes
LIB_DIR=${CLASS_DIR}/${PACKDEST}
LIBRARY=${LIB_DIR}/lib${NAME}.dylib
    

CC=cc
CFLAGS=-Wall -dynamiclib -I${JNI} -I${JNI}/darwin -I${SOURCES_DIR} -arch i386 -arch x86_64
LIBFLAGS=-framework Foundation -framework AppKit
SOURCES=$(shell find '$(SOURCES_DIR)' -type f -name '*.m')
OBJECTS=$(SOURCES:$(SOURCES_DIR)/%.cpp=$(OBJECTS_DIR)/%.o)


all: ${LIBRARY}

javah:
	javah -o ${SOURCES_DIR}/${CLASS}.h -cp ${CLASS_DIR} ${CLASS}

${LIBRARY}: ${SOURCES}
	mkdir -p ${LIB_DIR}
	${CC} ${CFLAGS} ${LIBFLAGS} -o ${LIBRARY} ${SOURCES}

clean:
	rm -f ${LIBRARY}
