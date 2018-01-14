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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.UIManager;

class DefaultEnhancer implements Enhancer {

    final List<BufferedImage> frameImages = new ArrayList<BufferedImage>();

    @SuppressWarnings("UseSpecificCatch")
    boolean setNimbusLookAndFeel() {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            if ("Nimbus".equals(info.getName()))
                try {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    return true;
                } catch (Exception ex) {
                }
        return false;
    }

    @SuppressWarnings("UseSpecificCatch")
    boolean setSystemLookAndFeel() {
        try {
            String name = UIManager.getSystemLookAndFeelClassName();
            if (name.contains("MetalLookAndFeel"))
                return false;
            UIManager.setLookAndFeel(name);
            return true;
        } catch (Exception ex1) {
        }
        return false;
    }

    @SuppressWarnings("UseSpecificCatch")
    boolean setPrettyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(PrettyLookAndFeelProvider.getLaF());
            return true;
        } catch (Throwable ex1) {
        }
        return false;
    }

    @Override
    public void registerPreferences(Runnable callback) {
    }

    @Override
    public void registerAbout(Runnable callback) {
    }

    @Override
    public void registerQuit(Runnable callback) {
    }

    @Override
    public void registerFileOpen(Enhancer.FileOpenRunnable callback) {
    }

    @Override
    public void registerUpdate(Runnable callback) {
    }

    @Override
    public void registerUpdate(String menutext, String menushortcut, Runnable callback) {
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
    public void setSafeLookAndFeel() {
        if (!setSystemLookAndFeel())
            if (!setPrettyLookAndFeel())
                setNimbusLookAndFeel();
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
}
