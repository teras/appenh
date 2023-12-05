/*
 *
 * This file is part of ApplicationEnhancer.
 *
 * ApplicationEnhancer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * ApplicationEnhancer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.appenh;

import com.panayotis.appenh.AFileChooser.FileSelectionMode;
import com.panayotis.loadlib.LoadLib;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.panayotis.appenh.AFileChooser.FileSelectionMode.*;

@SuppressWarnings("UseSpecificCatch")
class MacEnhancer implements Enhancer, FileChooserFactory {

    private static final Class<?> appClass;
    private static final Object appInstance;
    private static final String packpref;
    private static final boolean libFound;

    static final String LIB_LOCATION = "/com/panayotis/appenh/libmacenh.dylib";

    static {
        Class<?> aCass = null;
        Object aInst = null;
        String ppref = null;
        try {
            //noinspection JavaReflectionMemberAccess
            Desktop.class.getMethod("setAboutHandler", Class.forName("java.awt.desktop.AboutHandler"));
            aCass = Desktop.class;
            aInst = Desktop.getDesktop();
            ppref = "java.awt.desktop.";
        } catch (Exception ex) {
            try {
                aCass = Class.forName("com.apple.eawt.Application");
                aInst = aCass.getMethod("getApplication").invoke(null);
                ppref = "com.apple.eawt.";
            } catch (Exception ignored) {
            }
        }
        appClass = aCass;
        appInstance = aInst;
        packpref = ppref;
        libFound = LoadLib.load(LIB_LOCATION);
    }

    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    @Override
    public void registerAbout(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class<?> handler = Class.forName(packpref + "AboutHandler");
                appClass.getMethod("setAboutHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method.getName().equals("handleAbout"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void registerPreferences(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName(packpref + "PreferencesHandler");
                appClass.getMethod("setPreferencesHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method.getName().equals("handlePreferences"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void registerQuit(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName(packpref + "QuitHandler");
                appClass.getMethod("setQuitHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handleQuitRequestWith")) {
                            callback.run();
                            if (args != null && args.length > 1)
                                args[1].getClass().getMethod("cancelQuit", (Class<?>[]) null).invoke(args[1]);
                        }
                        return null;
                    }
                }));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void registerFileOpen(final FileOpenRunnable callback) {
        try {
            if (appInstance != null) {
                Class<?> handler = Class.forName(packpref + "OpenFilesHandler");
                appClass.getMethod("setOpenFileHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("openFiles") && args != null && args.length > 0) {
                            Method m = args[0].getClass().getMethod("getFiles");
                            List<File> list = (List<File>) m.invoke(args[0]);
                            for (File f : list)
                                try {
                                    callback.openFile(f);
                                } catch (Exception ex) {
                                }
                        }
                        return null;
                    }
                }));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void registerMenu(String menutext, Runnable callback) {
        if (menutext == null || menutext.trim().isEmpty())
            return;
        registerUpdate(menutext, null, callback);
    }

    @Override
    public void registerUpdate(Runnable callback) {
        registerUpdate(null, null, callback);
    }

    @Override
    public void registerUpdate(String menutext, String menushortcut, final Runnable callback) {
        menutext = menutext == null || menutext.trim().isEmpty() ? "Check for Updates..." : menutext.trim();
        menushortcut = menushortcut == null ? "" : menushortcut.trim();
        if (libFound)
            registerUpdate0(menutext, menushortcut, callback == null ? null : new Runnable() {
                public void run() {
                    SwingUtilities.invokeLater(callback);
                }
            });
    }

    @Override
    public native String getThemeName();

    private native void registerUpdate0(String menutext, String menushortcut, Runnable callback);

    @Override
    public void registerThemeChanged(final ThemeChangeListener callback) {
        if (libFound)
            registerThemeChanged0(new ThemeChangeListener() {
                @Override
                public void themeChanged(final String themeName) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            callback.themeChanged(themeName);
                        }
                    });
                }
            });
    }

    @Override
    public void toggleFullScreen(Window window) {
        try {
            if (window instanceof RootPaneContainer) {
                ((RootPaneContainer) window).getRootPane().putClientProperty("apple.awt.fullscreenable", Boolean.TRUE);
                if (appInstance != null)
                    appInstance.getClass().getMethod("requestToggleFullScreen", Window.class).invoke(appInstance, window);
                return;
            }
        } catch (Exception ignored) {
        }
        // failsafe
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        device.setFullScreenWindow(device.getFullScreenWindow() == window ? null : window);
    }

    private native void registerThemeChanged0(ThemeChangeListener callback);

    @Override
    public void blendWindowTitle(boolean blended) {
    }

    @Override
    public void setSafeLookAndFeel() {
    }

    @Override
    public void setDefaultLookAndFeel() {
    }

    @Override
    public boolean providesSystemMenus() {
        return true;
    }

    @Override
    public void setApplicationIcons(String... iconNames) {
        if (appInstance == null)
            return;
        List<Image> appImages = new ArrayList<>();
        for (String icon : iconNames)
            appImages.add(EnhancerManager.getImage(icon));
        if (!appImages.isEmpty())
            try {
                appClass.getMethod("setDockIconImage", Image.class).invoke(appInstance, EnhancerManager.getImage(appImages, 1024));
            } catch (Exception ignored) {
            }
    }

    @Override
    public void setApplicationIcons(Image... icons) {
    }

    @Override
    public void setApplicationName(String name) {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
        System.setProperty("apple.awt.application.name", name);
    }

    @Override
    public void updateFrameIcons(JFrame frame, String... iconResourceNames) {
    }

    @Override
    public void updateFrameIcons(JFrame frame, Collection<File> iconFiles) {
    }

    @Override
    public void updateFrameIconsWithImages(JFrame frame, Collection<Image> iconFiles) {
    }

    @Override
    public void setFrameSaveState(JFrame frame, boolean notSaved) {
        frame.getRootPane().putClientProperty("Window.documentModified", notSaved ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void registerApplication(String name, String comment, String... categories) {
    }

    @Override
    public void unregisterApplication(String name) {
    }

    @Override
    public int getDPI() {
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public boolean shouldScaleUI() {
        return false;
    }

    @Override
    public Collection<File> showOpenDialog(final String title, final String buttonTitle, final File directory, final boolean openMulti, FileSelectionMode mode) {
        final FileSelectionMode cmode = mode == null ? FilesOnly : mode;
        return showDialogCommon(new OpenDialogLambda() {
            @Override
            public void exec(FileDialogCallback callback) {
                showOpenDialog(title, buttonTitle, directory == null ? null : directory.getAbsolutePath(),
                        cmode == FilesOnly || cmode == FilesAndDirectories,
                        cmode == DirectoriesOnly || cmode == FilesAndDirectories,
                        openMulti, callback);
            }
        });
    }

    @Override
    public File showSaveDialog(final String title, final String buttonTitle, final File directory, final String file) {
        List<File> files = showDialogCommon(new OpenDialogLambda() {
            @Override
            public void exec(FileDialogCallback callback) {
                showSaveDialog(title, buttonTitle, directory == null ? null : directory.getAbsolutePath(), file, callback);
            }
        });
        return files.isEmpty() ? null : files.get(0);
    }

    private List<File> showDialogCommon(OpenDialogLambda exec) {
        final Thread thread = Thread.currentThread();
        final AtomicBoolean finish = new AtomicBoolean(false);
        final List<File> result = new ArrayList<File>();
        exec.exec(new FileDialogCallback() {
            @Override
            public void fileSelected(String path) {
                if (path == null) {
                    finish.set(true);
                    thread.interrupt();
                } else {
                    result.add(new File(path));
                }
            }
        });
        while (true) {
            try {
                if (finish.get())
                    break;
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException ignored) {
            }
        }
        return result;
    }

    private native void showOpenDialog(String title, String buttonTitle, String path, boolean canChooseFiles, boolean canChooseDirectories, boolean openMulti, FileDialogCallback callback);

    private native void showSaveDialog(String title, String buttonTitle, String directory, String file, FileDialogCallback callback);

    @SuppressWarnings("unused")
    private interface FileDialogCallback {
        void fileSelected(String path);
    }

    private interface OpenDialogLambda {
        void exec(FileDialogCallback callback);
    }
}
