#include "com.panayotis.appenh.MacEnhancer.h"
#include <Foundation/NSObjCRuntime.h>
#include <AppKit/AppKit.h>

JavaVM* jvm = NULL;

@interface AppEnhUpdateTarget : NSObject {
@private
    jobject callback;
    JNIEnv* env;
}

- (instancetype) initWithCallback:(jobject) callback env:(JNIEnv*) env;
- (void) callback:(id)sender;
@end

@implementation AppEnhUpdateTarget : NSObject

- (instancetype) initWithCallback:(jobject) callbackJ env:(JNIEnv*) envJ
{
    self = [super init];
    if (self) {
        self->callback = callbackJ;
        self->env = envJ;
    }
    return self;
}

- (void) callback:(id)sender
{
    if ((*jvm)->AttachCurrentThread(jvm, (void**)&self->env, NULL) != 0)
        return;

    jclass cls = (*self->env)->GetObjectClass(self->env, self->callback);
    jmethodID mid = (*self->env)->GetMethodID(self->env, cls, "run", "()V");
    if (mid == 0)
        return;
    (*self->env)->CallVoidMethod(self->env, self->callback, mid);

//    (*jvm)->DetachCurrentThread(jvm);
}
@end

JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_registerUpdate0
  (JNIEnv * env, jobject this, jstring menuname, jstring menushortcut, jobject callback)
{
    if (jvm==NULL)
        (*env)->GetJavaVM(env, &jvm);
    AppEnhUpdateTarget* target = [[AppEnhUpdateTarget alloc] initWithCallback:(*env)->NewGlobalRef(env, callback) env:env];

    const char * menuname_c = (*env)->GetStringUTFChars(env, menuname, 0);
    const char * menushortcut_c = (*env)->GetStringUTFChars(env, menushortcut, 0);

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
    NSMenuItem* menuitem = [[NSMenuItem alloc] 
        initWithTitle:[NSString stringWithUTF8String:menuname_c]
        action:@selector(callback:)
        keyEquivalent:[NSString stringWithUTF8String:menushortcut_c]];
    [menuitem setTarget:target];
    [appmenu insertItem:menuitem atIndex:1];

    (*env)->ReleaseStringUTFChars(env, menuname, menuname_c);
    (*env)->ReleaseStringUTFChars(env, menushortcut, menushortcut_c);

    // might not need to deallocate menuitem and target - they live as long as the application lives
}

