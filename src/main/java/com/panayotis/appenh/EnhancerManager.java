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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class EnhancerManager {

    private final static Enhancer enhancer;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac") && osName.contains("os") && osName.contains("x"))
            enhancer = new MacEnhancer();
        else if (osName.contains("linux"))
            enhancer = new LinuxEnhancer();
        else
            enhancer = new DefaultEnhancer();
    }

    public static Enhancer getDefault() {
        return enhancer;
    }

    static BufferedImage getImage(String resource) {
        try {
            InputStream stream = EnhancerManager.class.getClassLoader().getResourceAsStream(resource);
            if (stream == null)
                stream = new URL(resource).openStream();
            if (stream != null)
                return ImageIO.read(stream);
        } catch (IOException ex) {
        }
        return null;
    }

    static BufferedImage getImage(Collection<BufferedImage> images, int dimension) {
        if (images == null || images.isEmpty())
            return null;
        for (BufferedImage img : images)
            if (img.getWidth() == dimension)
                return img;
        Iterator<BufferedImage> it = images.iterator();
        BufferedImage candidate = it.next();
        while (it.hasNext()) {
            BufferedImage cur = it.next();
            int width = cur.getWidth();
            double delta = width / 2.0 / dimension;
            if (delta >= 1 && delta < (candidate.getWidth() / 2.0 / dimension))
                candidate = cur;
            else if (delta < 1 && delta > (candidate.getWidth() / 2.0 / dimension))
                candidate = cur;
        }
        if (candidate.getWidth() < dimension)
            return candidate;
        BufferedImage result = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(candidate, 0, 0, dimension, dimension, null);
        g.dispose();
        return result;
    }

    static String getSelfExec() {
        String exec = System.getenv().get("APPIMAGE");
        return exec == null ? System.getProperty("self.exec", null) : exec;
    }

    static void appendToList(List<BufferedImage> images, String name, BufferedImage img) {
        if (img == null)
            return;
        if (img.getWidth() == img.getHeight())
            images.add(img);
        else
            System.err.println("Width (" + img.getWidth() + ") and height (" + img.getHeight() + ") does not match; ignoring" + (name == null ? "" : " " + name) + ".");
    }
}
