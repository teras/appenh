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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface Enhancer {

    default void setModernLookAndFeel(ThemeVariation themeVariation) {
        if (themeVariation == null || themeVariation == ThemeVariation.LIGHT)
            FlatLightLaf.setup();
        else if (themeVariation == ThemeVariation.DARK)
            FlatDarkLaf.setup();
        else if (themeVariation == ThemeVariation.AUTO) {
            if (isDarkTheme())
                FlatDarkLaf.setup();
            else
                FlatLightLaf.setup();
        }
    }

    default void blendWindowTitle(boolean blended) {
        String value = blended ? "true" : "false";
        System.setProperty("flatlaf.useWindowDecorations", value);
        System.setProperty("flatlaf.menuBarEmbedded", value);
    }

    void registerPreferences(Runnable callback);

    void registerAbout(Runnable callback);

    void registerQuit(Runnable callback);

    void registerFileOpen(FileOpenRunnable callback);

    boolean providesSystemMenus();

    void setApplicationImages(Image... images);

    default ImageIcon findSVGIcon(String absoluteResourceName, float scaleFactor) {
        try {
            if (absoluteResourceName.startsWith("/"))
                absoluteResourceName = absoluteResourceName.substring(1);
            return new FlatSVGIcon(absoluteResourceName, scaleFactor);
        } catch (Exception e) {
            System.err.println("Unable to load icon " + absoluteResourceName);
            return null;
        }
    }

    default List<Image> findFrameImages(String absoluteResourceName) {
        try {
            if (!absoluteResourceName.startsWith("/"))
                absoluteResourceName = "/" + absoluteResourceName;
            return FlatSVGUtils.createWindowIconImages(absoluteResourceName);
        } catch (Exception e) {
            System.err.println("Unable to load frame icons " + absoluteResourceName);
            return Collections.emptyList();
        }
    }

    void setApplicationName(String name);

    void registerApplication(String name, String comment, String... categories);

    void unregisterApplication(String name);

    void setProposedSystemScaling(float proposedScaling);

    int getDPI();

    /**
     * @param frame     The frame to work on
     * @param iconNames Could be empty; the set application icons will be used
     */
    void updateFrameIcons(JFrame frame, String... iconNames);

    void updateFrameIcons(JFrame frame, Collection<File> iconFiles);

    void updateFrameIconsWithImages(JFrame frame, Collection<Image> iconFiles);

    void setFrameSaveState(JFrame frame, boolean notSaved);

    void toggleFullScreen(Window window);

    boolean isDarkTheme();

    interface FileOpenRunnable {

        void openFile(File file);
    }

}
