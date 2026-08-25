/*
 * xftdpi — print the X "Xft.dpi" resource (what `xrdb -query | grep Xft.dpi` reports).
 *
 * Bundled per-arch and run by LinuxEnhancer to obtain the screen DPI when `xrdb` is unavailable
 * (notably inside a Flatpak sandbox, whose runtime ships no xrdb). libX11 is loaded at runtime with
 * dlopen — no X11 headers or link are needed, so this cross-compiles for any arch with plain gcc:
 *
 *   gcc            -O2 -o xftdpi-linux-x86_64  xftdpi.c -ldl
 *   aarch64-linux-gnu-gcc -O2 -o xftdpi-linux-aarch64 xftdpi.c -ldl
 *
 * Prints the DPI value to stdout and exits 0 on success; exits non-zero (printing nothing) when the
 * display, the resource manager string, or the Xft.dpi resource is unavailable.
 */
#include <dlfcn.h>
#include <stdio.h>

typedef void *Display;
typedef void *XrmDatabase;
typedef struct { unsigned int size; char *addr; } XrmValue;

int main(void) {
    void *x11 = dlopen("libX11.so.6", RTLD_NOW);
    if (!x11)
        return 1;

    Display *(*XOpenDisplay)(const char *) = dlsym(x11, "XOpenDisplay");
    char *(*XResourceManagerString)(Display *) = dlsym(x11, "XResourceManagerString");
    void (*XrmInitialize)(void) = dlsym(x11, "XrmInitialize");
    XrmDatabase (*XrmGetStringDatabase)(const char *) = dlsym(x11, "XrmGetStringDatabase");
    int (*XrmGetResource)(XrmDatabase, const char *, const char *, char **, XrmValue *) =
            dlsym(x11, "XrmGetResource");
    if (!XOpenDisplay || !XResourceManagerString || !XrmGetStringDatabase || !XrmGetResource)
        return 1;

    Display *dpy = XOpenDisplay(NULL);
    if (!dpy)
        return 1;
    char *rms = XResourceManagerString(dpy);
    if (!rms)
        return 1;

    if (XrmInitialize)
        XrmInitialize();
    XrmDatabase db = XrmGetStringDatabase(rms);
    char *type = NULL;
    XrmValue val = { 0, NULL };
    if (XrmGetResource(db, "Xft.dpi", "Xft.Dpi", &type, &val) && val.addr) {
        printf("%s\n", val.addr);
        return 0;
    }
    return 1;
}
