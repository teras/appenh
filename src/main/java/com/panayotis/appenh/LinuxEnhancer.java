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

import static com.panayotis.appenh.EnhancerManager.getSelfExec;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import javax.imageio.ImageIO;

public class LinuxEnhancer extends DefaultEnhancer {

    @Override
    public void registerApplication(String name, String comment, String... categories) {
        String exec = getSelfExec();
        if (exec == null)
            return;

        StringBuilder out = new StringBuilder();
        out.append("[Desktop Entry]\n");
        out.append("Type=Application\n");
        out.append("Name=").append(name).append("\n");
        out.append("Exec=").append(exec).append(" %U\n");
        out.append("TryExec=").append(exec).append("\n");
        if (categories != null && categories.length > 0) {
            StringBuilder cat = new StringBuilder();
            for (String ct : categories)
                cat.append(ct).append(';');
            if (cat.length() > 0)
                out.append("Categories=").append(cat).append("\n");
        }
        if (comment != null && !comment.trim().isEmpty())
            out.append("Comment=").append(comment.trim()).append("\n");

        if (writeImage(frameImages, name.toLowerCase(), 32)
                | writeImage(frameImages, name.toLowerCase(), 64)
                | writeImage(frameImages, name.toLowerCase(), 128))
            out.append("Icon=").append(name.toLowerCase()).append("\n");

        writeFile(getMetafileName(name), out.toString());
        exec("chmod", "755", getMetafileName(name));
        File ico = new File(getImageName(name, 128));
        if (ico.exists())
            exec("gio", "set", "-t", "string", exec, "metadata::custom-icon", toURI(ico));
    }

    @Override
    public void unregisterApplication(String name) {
        new File(getMetafileName(name)).delete();
        new File(getImageName(name, 32)).delete();
        new File(getImageName(name, 64)).delete();
        new File(getImageName(name, 128)).delete();
        
    }

    private static boolean writeFile(String path, String content) {
        Writer out = null;
        try {
            File fout = new File(path);
            fout.getParentFile().mkdirs();
            out = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
            out.append(content);
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException ex1) {
                }
        }
    }

    private static String getImageName(String name, int size) {
        return System.getProperty("user.home") + "/.local/share/icons/hicolor/"
                + size + "x" + size + "/apps/"
                + name.toLowerCase() + ".png";
    }

    private static String getMetafileName(String name) {
        return System.getProperty("user.home") + "/.local/share/applications/" + name.toLowerCase() + ".desktop";
    }

    private static String toURI(File file) {
        String icopath = file.toURI().toString();
        if (icopath.startsWith("file:/") && !icopath.startsWith("file:///"))
            icopath = "file:///" + icopath.substring(6);
        return icopath;
    }

    private static boolean writeImage(Collection<BufferedImage> frameImages, String name, int size) {
        if (frameImages != null && !frameImages.isEmpty())
            try {
                File out = new File(getImageName(name, size));
                out.getParentFile().mkdirs();
                ImageIO.write(EnhancerManager.getImage(frameImages, size), "png", new FileOutputStream(out));
                return true;
            } catch (IOException ex) {
            }
        return false;
    }

    private static boolean exec(String... cmd) {
        try {
            Process exec = Runtime.getRuntime().exec(cmd);
            return exec.waitFor() == 0;
        } catch (IOException ex) {
        } catch (InterruptedException ex) {
        }
        return false;
    }
}
