#include "com.panayotis.appenh.MacEnhancer.h"
#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#import <Cocoa/Cocoa.h>

JavaVM* jvm = NULL;

@interface AppEnhTarget : NSObject {
@protected
    jobject callback;
    JNIEnv* env;
}
- (instancetype) initWithCallback:(jobject) callback env:(JNIEnv*) env;
@end

@interface AppEnhUpdateTarget : AppEnhTarget
- (void) callback:(id)sender;
@end

@interface AppEnhThemeChangeTarget : AppEnhTarget
- (void)themeChanged:(NSNotification *) notification;
@end

/*
 * Generic helper functions
 */
jstring getThemeName(JNIEnv * jenv) {
    NSString* nsname = [[NSUserDefaults standardUserDefaults] stringForKey:@"AppleInterfaceStyle"];
    if (nsname==nil)
        nsname = @"Light";
    const char* name = [nsname UTF8String];
    return (*jenv)->NewStringUTF(jenv, name);
}

@implementation AppEnhTarget
- (instancetype) initWithCallback:(jobject) callbackJ env:(JNIEnv*) envJ {
    self = [super init];
    if (self) {
        self->callback = callbackJ;
        self->env = envJ;
    }
    return self;
}
@end

@implementation AppEnhUpdateTarget
- (void) callback:(id)sender {
    if ((*jvm)->AttachCurrentThread(jvm, (void**)&self->env, NULL) != 0)
        return;
    jclass cls = (*env)->FindClass(env, "java/lang/Runnable");
    jmethodID mid = (*self->env)->GetMethodID(self->env, cls, "run", "()V");
    if (mid == 0)
        return;
    (*self->env)->CallVoidMethod(self->env, self->callback, mid);
//    (*jvm)->DetachCurrentThread(jvm);
}
@end

@implementation AppEnhThemeChangeTarget
- (void)themeChanged:(NSNotification *) notification {
    if ((*jvm)->AttachCurrentThread(jvm, (void**)&self->env, NULL) != 0)
        return;
    jclass cls = (*env)->FindClass(env, "com/panayotis/appenh/Enhancer$ThemeChangeListener");
    jmethodID mid = (*self->env)->GetMethodID(self->env, cls, "themeChanged", "(Ljava/lang/String;)V");
    if (mid == 0)
        return;
    (*self->env)->CallVoidMethod(self->env, self->callback, mid, getThemeName(self->env));
//    (*g_VM)->DetachCurrentThread(g_VM);
}
@end

JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_registerUpdate0
  (JNIEnv * env, jobject this, jstring menuname, jstring menushortcut, jobject callback) {
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

JNIEXPORT jstring JNICALL Java_com_panayotis_appenh_MacEnhancer_getThemeName
  (JNIEnv * env, jobject this) {
    return getThemeName(env);
}

JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_registerThemeChanged0
  (JNIEnv * env, jobject this, jobject callback) {
   if (jvm==NULL)
          (*env)->GetJavaVM(env, &jvm);
    AppEnhThemeChangeTarget* target = [[AppEnhThemeChangeTarget alloc] initWithCallback:(*env)->NewGlobalRef(env, callback) env:env];
    [NSDistributedNotificationCenter.defaultCenter addObserver:target selector:@selector(themeChanged:) name:@"AppleInterfaceThemeChangedNotification" object: nil];
}

/*
 * Class:     com_panayotis_appenh_MacEnhancer
 * Method:    showOpenDialog
 * Signature: (Ljava/lang/String;Ljava/lang/String;ZZ)Ljava/lang/String;
 */
JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_showOpenDialog
  (JNIEnv * env, jobject this, jstring title, jstring button, jstring directory, jboolean canChooseFiles,
        jboolean canChooseDirectories, jboolean openMulti, jobject callback){
    const char * title_c = title == NULL ? NULL : (*env)->GetStringUTFChars(env, title, 0);
    const char * button_c = button == NULL ? NULL : (*env)->GetStringUTFChars(env, button, 0);
    const char * directory_c = directory == NULL ? NULL : (*env)->GetStringUTFChars(env, directory, 0);
    jobject callbackG = (*env)->NewGlobalRef(env, callback);
    dispatch_async(dispatch_get_main_queue(), ^{
        NSOpenPanel* open = [NSOpenPanel openPanel];
        if (title_c)
            [open setTitle:[NSString stringWithUTF8String:title_c]];
        if (button_c)
            [open setPrompt:[NSString stringWithUTF8String:button_c]];
        if (directory_c)
            [open setDirectoryURL:[NSURL fileURLWithPath:[NSString stringWithUTF8String:directory_c]]];
        [open setCanChooseFiles:canChooseFiles];
        [open setCanChooseDirectories:canChooseDirectories];
        [open setAllowsMultipleSelection:openMulti];
        [open beginWithCompletionHandler:^(NSInteger asyncResult){
            jmethodID mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, callbackG),
                "fileSelected", "(Ljava/lang/String;)V");
            if (mid == 0)
                return;
            if (asyncResult == NSModalResponseOK) {
                for (NSURL* url  in [open URLs]) {
                    jstring result = (*env)->NewStringUTF(env, [[url path] UTF8String]);
                    (*env)->CallVoidMethod(env, callbackG, mid, result);
                    (*env)->DeleteLocalRef(env, result);
                }
            }
            (*env)->CallVoidMethod(env, callbackG, mid, NULL);
            (*env)->DeleteGlobalRef(env, callbackG);
        }];
        (*env)->ReleaseStringUTFChars(env, title, title_c);
        (*env)->ReleaseStringUTFChars(env, directory, directory_c);
    });
}

/*
 * Class:     com_panayotis_appenh_MacEnhancer
 * Method:    showSaveDialog
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT void JNICALL Java_com_panayotis_appenh_MacEnhancer_showSaveDialog
  (JNIEnv * env, jobject this, jstring title, jstring button, jstring directory, jstring filename,
        jobject callback) {
    const char * title_c = title == NULL ? NULL : (*env)->GetStringUTFChars(env, title, 0);
    const char * button_c = button == NULL ? NULL : (*env)->GetStringUTFChars(env, button, 0);
    const char * directory_c = directory == NULL ? NULL : (*env)->GetStringUTFChars(env, directory, 0);
    const char * filename_c = filename == NULL ? NULL : (*env)->GetStringUTFChars(env, filename, 0);
    jobject callbackG = (*env)->NewGlobalRef(env, callback);

    dispatch_async(dispatch_get_main_queue(), ^{
        NSSavePanel* save = [NSSavePanel savePanel];
        if (title_c)
            [save setTitle:[NSString stringWithUTF8String:title_c]];
        if (button_c)
            [save setPrompt:[NSString stringWithUTF8String:button_c]];
        if (directory_c)
            [save setDirectoryURL:[NSURL fileURLWithPath:[NSString stringWithUTF8String:directory_c]]];
        if (filename_c)
            [save setNameFieldStringValue:[NSString stringWithUTF8String:filename_c]];
        [save beginWithCompletionHandler:^(NSInteger asyncResult){
            jmethodID mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, callbackG),
                "fileSelected", "(Ljava/lang/String;)V");
            if (mid == 0)
                return;
            if (asyncResult == NSModalResponseOK) {
                jstring result = (*env)->NewStringUTF(env, [[[save URL] path] UTF8String]);
                (*env)->CallVoidMethod(env, callbackG, mid, result);
                (*env)->DeleteLocalRef(env, result);
            }
            (*env)->CallVoidMethod(env, callbackG, mid, NULL);
            (*env)->DeleteGlobalRef(env, callbackG);
        }];
        (*env)->ReleaseStringUTFChars(env, title, title_c);
        (*env)->ReleaseStringUTFChars(env, button, button_c);
        (*env)->ReleaseStringUTFChars(env, directory, directory_c);
        (*env)->ReleaseStringUTFChars(env, filename, filename_c);
    });
}
