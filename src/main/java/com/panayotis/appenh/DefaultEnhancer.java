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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class DefaultEnhancer implements Enhancer {

    final List<Image> frameImages = new ArrayList<>();

    @Override
    public void registerPreferences(Runnable callback) {
        // Not supported
    }

    @Override
    public void registerAbout(Runnable callback) {
        // Not supported
    }

    @Override
    public void registerQuit(Runnable callback) {
        // Not supported
    }

    @Override
    public void registerFileOpen(Enhancer.FileOpenRunnable callback) {
        // Not supported
    }

    @Override
    public boolean providesSystemMenus() {
        return false;
    }

    @Override
    public void setApplicationImages(Image... images) {
        Collections.addAll(frameImages, images);
    }

    @Override
    public void setApplicationName(String name) {
        // Not supported
    }

    @Override
    public void updateFrameIcons(JFrame frame, String... iconNames) {
        List<Image> ficons = new ArrayList<>();
        for (String icon : iconNames)
            ficons.add(EnhancerManager.getImage(icon));
        frame.setIconImages(ficons.isEmpty() ? frameImages : ficons);
    }

    @Override
    public void updateFrameIcons(JFrame frame, Collection<File> iconFiles) {
        if (iconFiles == null || iconFiles.isEmpty())
            return;
        List<Image> ficons = new ArrayList<Image>();
        for (File ifile : iconFiles)
            try {
                ficons.add(ImageIO.read(ifile));
            } catch (IOException ignored) {
            }
        frame.setIconImages(ficons);
    }

    @Override
    public void updateFrameIconsWithImages(JFrame frame, Collection<Image> iconFiles) {
        frame.setIconImages(new ArrayList<Image>(iconFiles));
    }

    @Override
    public void setFrameSaveState(JFrame frame, boolean notSaved) {
        // not supported
    }

    @Override
    public void blendWindowTitle(boolean blended) {
    }

    @Override
    public void registerApplication(String name, String comment, String... categories) {
    }

    @Override
    public void unregisterApplication(String name) {
    }

    @Override
    public void toggleFullScreen(Window window) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        device.setFullScreenWindow(device.getFullScreenWindow() == window ? null : window);
    }

    @Override
    public void setProposedSystemScaling(float proposedScaling) {
        if (proposedScaling < 0.1) {
            proposedScaling = getDPI() / 96f;
            if (proposedScaling < 1)
                proposedScaling = 1; // Do not scale below 1)
        }
        System.setProperty("flatlaf.uiScale", Double.toString(proposedScaling));
    }

    @Override
    public int getDPI() {
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public boolean isDarkTheme() {
        return false;
    }
}
