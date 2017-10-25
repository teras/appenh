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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class EnhancerManager {

    private final static Enhancer enhancer;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac") && osName.contains("os") && osName.contains("x"))
            enhancer = new MacEnhancer();
        else
            enhancer = new DefaultEnhancer();
    }

    public static Enhancer getDefault() {
        return enhancer;
    }

    static List<Image> getImage(String... resources) {
        List<Image> images = new ArrayList<Image>();
        for (String name : resources)
            try {
                InputStream stream = EnhancerManager.class.getClassLoader().getResourceAsStream(name);
                if (stream == null)
                    stream = new URL(name).openStream();
                if (stream != null)
                    images.add(ImageIO.read(stream));
            } catch (IOException ex) {
            }
        return images;
    }
}
