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

import com.panayotis.loadlib.LoadLib;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("UseSpecificCatch")
class MacEnhancer implements Enhancer {

    private static final Class appClass;
    private static final Object appInstance;
    private static final String packpref;
    private static final boolean libFound;

    static final String LIB_LOCATION = "/com/panayotis/appenh/libmacenh.dylib";

    static {
        Class aCass = null;
        Object aInst = null;
        String ppref = null;
        try {
            Desktop.class.getMethod("setAboutHandler", Class.forName("java.awt.desktop.AboutHandler"));
            aCass = Desktop.class;
            aInst = Desktop.getDesktop();
            ppref = "java.awt.desktop.";
        } catch (Exception ex) {
            try {
                aCass = Class.forName("com.apple.eawt.Application");
                aInst = aCass.getMethod("getApplication", (Class[]) null).invoke(null, (Object[]) null);
                ppref = "com.apple.eawt.";
            } catch (Exception ex1) {
            }
        }
        appClass = aCass;
        appInstance = aInst;
        packpref = ppref;
        libFound = LoadLib.load(LIB_LOCATION);
    }

    @Override
    public void registerAbout(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName(packpref + "AboutHandler");
                appClass.getMethod("setAboutHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handleAbout"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void registerPreferences(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName(packpref + "PreferencesHandler");
                appClass.getMethod("setPreferencesHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handlePreferences"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
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
                                args[1].getClass().getMethod("cancelQuit", (Class[]) null).invoke(args[1], (Object[]) null);
                        }
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void registerFileOpen(final FileOpenRunnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName(packpref + "OpenFilesHandler");
                appClass.getMethod("setOpenFileHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("openFiles") && args != null && args.length > 0) {
                            Method m = args[0].getClass().getMethod("getFiles", (Class[]) null);
                            List<File> list = (List<File>) m.invoke(args[0], (Object[]) null);
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
        } catch (Exception ex) {
        }
    }

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

    private native void registerUpdate0(String menutext, String menushortcut, Runnable callback);

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
        List<BufferedImage> appImages = new ArrayList<BufferedImage>();
        for (String icon : iconNames)
            EnhancerManager.appendToList(appImages, "resource " + icon, EnhancerManager.getImage(icon));
        if (!appImages.isEmpty())
            try {
                appClass.getMethod("setDockIconImage", Image.class).invoke(appInstance, EnhancerManager.getImage(appImages, 1024));
            } catch (Exception ex) {
            }
    }

    @Override
    public void setApplicationIcons(BufferedImage... iconNames) {
    }

    @Override
    public void updateFrameIcons(JFrame frame, String... iconResourceNames) {
    }

    @Override
    public void updateFrameIcons(JFrame frame, Collection<File> iconFiles) {
    }

    @Override
    public void registerApplication(String name, String comment, String... categories) {
    }

    @Override
    public void unregisterApplication(String name) {
    }
}
