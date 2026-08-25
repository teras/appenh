NAME=macenh
CLASS=com.panayotis.appenh.MacEnhancer
PACKDEST=com/panayotis/appenh

LIBNAME=lib${NAME}.dylib
LIBRARY_DIR=library

CLASS_DIR=target/classes
TEMP_LIB_DIR=target/lib

TEMP_LIB=${TEMP_LIB_DIR}/${LIBNAME}
LIBRARY=${LIBRARY_DIR}/${LIBNAME}
INST_LIBRARY=${CLASS_DIR}/${PACKDEST}/${LIBNAME}

JNI_INCLUDE:=include
SOURCES_DIR:=src/main/objc

# CC:=docker run --rm -v${PWD}:/root teras/nimcrossosx x86_64-apple-darwin19-clang
CC:=clang
CFLAGS:=-Wall -dynamiclib -I${JNI_INCLUDE} -I${JNI_INCLUDE}/darwin -I${SOURCES_DIR} -mmacosx-version-min=10.15
LIBFLAGS:=-framework Foundation -framework AppKit
SOURCES=$(shell find '$(SOURCES_DIR)' -type f -name '*.m')
OBJECTS=$(SOURCES:$(SOURCES_DIR)/%.cpp=$(OBJECTS_DIR)/%.o)


all: ${INST_LIBRARY}

# Linux DPI helper: tiny dlopen(libX11)-based binary, bundled per-arch as a resource and run by
# LinuxEnhancer.getDPI() when xrdb is unavailable (e.g. inside a Flatpak sandbox). No X11 headers or
# link are needed, so it cross-compiles with plain gcc. Run on a Linux host with the aarch64 cross gcc.
XFTDPI_SRC=src/main/c/xftdpi.c
XFTDPI_DIR=src/main/resources/com/panayotis/appenh

xftdpi:
	mkdir -p ${XFTDPI_DIR}
	gcc -O2 -o ${XFTDPI_DIR}/xftdpi-linux-x86_64 ${XFTDPI_SRC} -ldl
	aarch64-linux-gnu-gcc -O2 -o ${XFTDPI_DIR}/xftdpi-linux-aarch64 ${XFTDPI_SRC} -ldl
	strip ${XFTDPI_DIR}/xftdpi-linux-x86_64
	aarch64-linux-gnu-strip ${XFTDPI_DIR}/xftdpi-linux-aarch64

javah:
	javah -o ${SOURCES_DIR}/${CLASS}.h -cp ${CLASS_DIR} ${CLASS}

${LIBRARY}: ${SOURCES}
	mkdir -p ${TEMP_LIB_DIR}
	mkdir -p ${LIBRARY_DIR}
	${CC} ${CFLAGS} ${LIBFLAGS} -arch x86_64 -o ${TEMP_LIB}.intel ${SOURCES}
	${CC} ${CFLAGS} ${LIBFLAGS} -arch arm64  -o ${TEMP_LIB}.arm ${SOURCES}
	lipo ${TEMP_LIB}.intel ${TEMP_LIB}.arm -create -output ${LIBRARY}
	echo "WARNING!!! REMEMBER TO SIGN THIS LIBRARY"

sign:${LIBRARY}
	rcodesign sign --p12-file ~/.ssh/apple-developer.p12 --p12-password-file ~/.ssh/apple-sign.password --code-signature-flags runtime ${LIBRARY}

${INST_LIBRARY}: ${LIBRARY}
	mkdir -p ${CLASS_DIR}/${PACKDEST}
	cp ${LIBRARY} ${INST_LIBRARY}

distclean:
	rm -f ${LIBRARY}
