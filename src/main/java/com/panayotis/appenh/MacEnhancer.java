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

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UseSpecificCatch")
class MacEnhancer implements Enhancer {

    private static final Class<?> appClass;
    private static final Object appInstance;
    private static final String packpref;

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

    @Override
    public boolean isDarkTheme() {
        try {
            Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                return line != null && line.trim().equalsIgnoreCase("Dark");
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean providesSystemMenus() {
        return true;
    }

    @Override
    public void setApplicationImages(Image... images) {
        if (appInstance == null)
            return;
        if (images != null && images.length > 0)
            try {
                appClass.getMethod("setDockIconImage", Image.class).invoke(appInstance, selectBestDockIcon(images));
            } catch (Exception ignored) {
            }
    }

    private static Image selectBestDockIcon(Image... images) {
        List<Image> allVariants = new ArrayList<>();
        for (Image img : images) {
            if (img == null) continue;
            // Try to detect and unpack MultiResolutionImage via reflection
            Class<?> clazz = img.getClass();
            if (clazz.getName().contains("MultiResolutionImage")) {
                try {
                    Method getVariants = clazz.getMethod("getResolutionVariants");
                    @SuppressWarnings("unchecked")
                    List<Image> variants = (List<Image>) getVariants.invoke(img);
                    allVariants.addAll(variants);
                    continue;
                } catch (Exception ignored) {
                    // fall back to using the base image
                }
            }
            allVariants.add(img); // not a MultiResolutionImage
        }

        // Select largest area image
        return allVariants.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(i -> i.getWidth(null) * i.getHeight(null)))
                .orElse(null);
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

    @SuppressWarnings("unused")
    private interface FileDialogCallback {
        void fileSelected(String path);
    }

    private interface OpenDialogLambda {
        void exec(FileDialogCallback callback);
    }

}
