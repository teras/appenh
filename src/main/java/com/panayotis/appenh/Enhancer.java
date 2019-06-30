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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import javax.swing.JFrame;

public interface Enhancer {

    void setSafeLookAndFeel();

    void setDefaultLookAndFeel();

    void registerPreferences(Runnable callback);

    void registerAbout(Runnable callback);

    void registerQuit(Runnable callback);

    void registerUpdate(String menutext, String menushortcut, Runnable callback);

    void registerUpdate(Runnable callback);

    void registerFileOpen(FileOpenRunnable callback);

    boolean providesSystemMenus();

    void setApplicationIcons(String... iconNames);

    void setApplicationIcons(BufferedImage... iconNames);

    void registerApplication(String name, String comment, String... categories);

    void unregisterApplication(String name);

    /**
     *
     * @param frame
     * @param iconNames Could be empty; the set application icons will be used
     */
    void updateFrameIcons(JFrame frame, String... iconNames);

    void updateFrameIcons(JFrame frame, Collection<File> iconFiles);

    interface FileOpenRunnable {

        void openFile(File file);
    }
}
