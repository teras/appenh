#include "com.panayotis.appenh.MacEnhancer.h"
#include <Foundation/NSObjCRuntime.h>
#include <AppKit/AppKit.h>

JavaVM* g_VM;
JNIEnv* env = NULL;
jobject callback = NULL;

@interface AppEnhUpdateTarget : NSObject
- (void) callback:(id)sender;
@end

@implementation AppEnhUpdateTarget : NSObject
- (void) callback:(id)sender
{
    if (callback==NULL)
        return;
    if ((*g_VM)->AttachCurrentThread(g_VM, (void**)&env, NULL) != 0)
        return;

    jclass cls = (*env)->GetObjectClass(env, callback);
    jmethodID mid = (*env)->GetMethodID(env, cls, "run", "()V");
    if (mid == 0)
        return;
    (*env)->CallVoidMethod(env, callback, mid);

//    (*g_VM)->DetachCurrentThread(g_VM);
}
@end

AppEnhUpdateTarget* target = nil;

JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_registerUpdate0
  (JNIEnv * genv, jobject this, jobject gcallback)
{
    if (callback)   // Alread registered object, free the old one
        (*env)->DeleteGlobalRef(env, callback);    // use env since 'env' is the environment of old 'callback'
    callback = (*genv)->IsSameObject(genv, gcallback, NULL)  // use genv to test the new object since this is the environment of 'gcallback'
        ? NULL
        : (*genv)->NewGlobalRef(genv, gcallback);  // create reference to callback if not null with genv as environment
    env = genv;
    (*env)->GetJavaVM(env, &g_VM);
    if (target!=nil)
        return; // Already registered procedure, no need to do something more since the callback has already been defined

    NSApplication* app = [NSApplication sharedApplication];
    if (!app)
        return;
    NSMenu* mainMenu = [app mainMenu];
    if(!mainMenu)
        return;
    NSArray* items = [mainMenu itemArray];
    if(!items || [items count]<1)
        return;
    NSMenuItem* appmenuitem = [items objectAtIndex:0];
    if (!appmenuitem)
        return;
    NSMenu* appmenu = [appmenuitem submenu];
    if (!appmenu)
        return;
    NSMenuItem* menuitem = [[NSMenuItem alloc] initWithTitle:@"Update" action:@selector(callback:) keyEquivalent:@""];
    target = [[AppEnhUpdateTarget alloc] init];
    [menuitem setTarget:target];
    [appmenu insertItem:menuitem atIndex:1];
}

