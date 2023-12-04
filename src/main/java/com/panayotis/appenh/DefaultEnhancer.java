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
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DefaultEnhancer implements Enhancer {

    final List<BufferedImage> frameImages = new ArrayList<BufferedImage>();

    @SuppressWarnings("UseSpecificCatch")
    boolean setNimbusLookAndFeel() {
        if (UIManager.getLookAndFeel().getClass().getName().toLowerCase().contains("nimbus"))
            return true;
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            return true;
        } catch (UnsupportedLookAndFeelException e) {
            return false;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    boolean setSystemLookAndFeel() {
        try {
            String name = UIManager.getSystemLookAndFeelClassName();
            if (UIManager.getLookAndFeel().getClass().getName().equals("name"))
                return true;
            if (name.contains("MetalLookAndFeel"))
                return false;
            UIManager.setLookAndFeel(name);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

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
    public void registerUpdate(Runnable callback) {
        // Not supported
    }

    @Override
    public void registerUpdate(String menutext, String menushortcut, Runnable callback) {
        // Not supported
    }

    @Override
    public void registerMenu(String menutext, Runnable callback) {
        // Not supported
    }

    @Override
    public boolean providesSystemMenus() {
        return false;
    }

    @Override
    public void setApplicationIcons(String... iconNames) {
        for (String icon : iconNames)
            EnhancerManager.appendToList(frameImages, "resource " + icon, EnhancerManager.getImage(icon));
    }

    public void setApplicationIcons(BufferedImage... images) {
        for (BufferedImage img : images)
            EnhancerManager.appendToList(frameImages, "", img);
    }

    @Override
    public void setApplicationName(String name) {
        // Not supported
    }

    @Override
    public void updateFrameIcons(JFrame frame, String... iconNames) {
        List<BufferedImage> ficons = new ArrayList<BufferedImage>();
        for (String icon : iconNames)
            EnhancerManager.appendToList(ficons, "resource " + icon, EnhancerManager.getImage(icon));
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
            } catch (IOException ex) {
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
    public void setSafeLookAndFeel() {
        if (!setSystemLookAndFeel())
            setNimbusLookAndFeel();
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    @Override
    public void setDefaultLookAndFeel() {
        setSystemLookAndFeel();
    }

    @Override
    public void registerApplication(String name, String comment, String... categories) {
    }

    @Override
    public void unregisterApplication(String name) {
    }

    @Override
    public void registerThemeChanged(ThemeChangeListener callback) {
    }

    @Override
    public void toggleFullScreen(Window window) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        device.setFullScreenWindow(device.getFullScreenWindow() == window ? null : window);
    }

    @Override
    public String getThemeName() {
        return "";
    }

    @Override
    public int getDPI() {
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public boolean shouldScaleUI() {
        if (!System.getProperty("sun.java2d.uiScale", "").isEmpty())
            return false;
        return getDPI() > 100;    // 96 is the value, don't scale if the value is too close;
    }
}
