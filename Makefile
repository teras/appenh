NAME=macenh
CLASS=com.panayotis.appenh.MacEnhancer
PACKDEST=com/panayotis/appenh

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    CC:=cc
else
    CC:=docker run --rm -v${PWD}:/root teras/osxcross x86_64-apple-darwin19-clang
endif


TARGET=target
CLASS_DIR=${TARGET}/classes
LIB_DIR=${CLASS_DIR}/${PACKDEST}
LIBRARY=${LIB_DIR}/lib${NAME}.dylib
    
JNI_INCLUDE:=include
SOURCES_DIR:=src/main/objc

CFLAGS:=-Wall -dynamiclib -I${JNI_INCLUDE} -I${JNI_INCLUDE}/darwin -I${SOURCES_DIR} -arch x86_64
LIBFLAGS:=-framework Foundation -framework AppKit
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
