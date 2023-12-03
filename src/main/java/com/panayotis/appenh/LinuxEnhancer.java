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

import com.formdev.flatlaf.FlatLightLaf;
import com.panayotis.appenh.Enhancer.ThemeChangeListener;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;

import static com.panayotis.appenh.EnhancerManager.getSelfExec;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedReturnValue"})
class LinuxEnhancer extends DefaultEnhancer {
    private LinuxThemeListenerThread themeListenerThread;
    private int dpi = -1;

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
                } catch (IOException ignored) {
                }
        }
    }

    // https://stackoverflow.com/a/8735707
    private static boolean writeThumbnail(File input, File output, String execpath) {
        try {
            File exec = new File(execpath);
            ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_INDEXED);

            //adding metadata
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
            addMetaData(metadata, "Thumb::URI", toURI(exec));
            addMetaData(metadata, "Thumb::MTime", String.valueOf(exec.lastModified() / 1000));

            //writing the data
            output.getParentFile().mkdirs();
            BufferedOutputStream baos = new BufferedOutputStream(new FileOutputStream(output));
            ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
            writer.setOutput(stream);
            writer.write(metadata, new IIOImage(ImageIO.read(input), null, metadata), writeParam);
            stream.close();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return false;
        }
    }

    private static String getMD5Hash(String text) {
        try {
            StringBuilder num = new StringBuilder(new BigInteger(1, MessageDigest.getInstance("MD5").digest(text.getBytes("UTF-8"))).toString(16));
            while (num.length() < 32)
                num.insert(0, "0");
            return num.toString();
        } catch (NoSuchAlgorithmException ignored) {
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
    }

    private static void addMetaData(IIOMetadata metadata, String key, String value) throws IIOInvalidTreeException {
        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", key);
        textEntry.setAttribute("value", value);
        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        text.appendChild(textEntry);
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);
        metadata.mergeTree("javax_imageio_png_1.0", root);
    }

    private static String getDesktopFileName(String basename) {
        return System.getProperty("user.home") + "/.local/share/applications/" + basename + ".desktop";
    }

    private static String getThumbFilename(String exec) {
        return System.getProperty("user.home") + "/.cache/thumbnails/normal/" + getMD5Hash(toURI(new File(exec))) + ".png";
    }

    private static String toURI(File file) {
        String icopath = file.toURI().toString();
        if (icopath.startsWith("file:/") && !icopath.startsWith("file:///"))
            icopath = "file:///" + icopath.substring(6);
        return icopath;
    }

    private static File getTempImage(Collection<BufferedImage> frameImages, int size) {
        if (frameImages != null && !frameImages.isEmpty())
            try {
                File out = File.createTempFile("image-", ".png").getAbsoluteFile();
                out.getParentFile().mkdirs();
                ImageIO.write(EnhancerManager.getImage(frameImages, size), "png", new FileOutputStream(out));
                return out;
            } catch (IOException ignored) {
            }
        return null;
    }

    private static boolean exec(String... cmd) {
        try {
            Process exec = Runtime.getRuntime().exec(cmd);
            return exec.waitFor() == 0;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    boolean setSystemLookAndFeel() {
        return FlatLightLaf.setup();
    }

    @Override
    public void registerApplication(final String name_s, final String comment_s, final String... categories) {
        new Thread(() -> {
            String name = name_s;
            String comment = comment_s;
            if (name == null || name.trim().isEmpty())
                return;
            else
                name = name.trim();
            String basename = name.toLowerCase();

            if (comment == null)
                comment = "";
            else
                comment = comment.trim();

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
            if (!comment.isEmpty())
                out.append("Comment=").append(comment).append("\n");

            if (!frameImages.isEmpty()) {
                out.append("Icon=").append(basename).append("\n");
                File img32 = getTempImage(frameImages, 32);
                File img64 = getTempImage(frameImages, 64);
                File parent = new File(System.getProperty("user.home"), ".cache/appenh");
                parent.mkdirs();
                File img128 = new File(parent, basename + ".png");
                try {
                    img128.getParentFile().mkdirs();
                    ImageIO.write(EnhancerManager.getImage(frameImages, 128), "png", new FileOutputStream(img128));
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
                exec("gio", "set", "-t", "string", exec, "metadata::custom-icon", toURI(img128));

                exec("xdg-icon-resource", "install", "--novendor", "--size", "32", img32.getAbsolutePath(), basename);
                exec("xdg-icon-resource", "install", "--novendor", "--size", "64", img64.getAbsolutePath(), basename);
                exec("xdg-icon-resource", "install", "--novendor", "--size", "128", img128.getAbsolutePath(), basename);
                writeThumbnail(img128, new File(getThumbFilename(exec)), exec);
                img32.delete();
                img64.delete();
            }

            writeFile(getDesktopFileName(basename), out.toString());
            exec("chmod", "755", getDesktopFileName(basename));

            exec("xdg-desktop-menu", "forceupdate");
        }).start();
    }

    @Override
    public void unregisterApplication(String basename) {
        basename = basename.toLowerCase();
        new File(getDesktopFileName(basename)).delete();
        exec("xdg-icon-resource uninstall", "--size", "32", basename);
        exec("xdg-icon-resource uninstall", "--size", "64", basename);
        exec("xdg-icon-resource uninstall", "--size", "128", basename);
        String exec = getSelfExec();
        if (exec != null)
            new File(getThumbFilename(exec)).delete();

        // gio will invalidate self - no need to set -t unset
        exec("xdg-desktop-menu", "forceupdate");
    }

    @Override
    public int getDPI() {
        if (dpi > 0)
            return dpi;
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"xrdb", "-q"});
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase();
                    if (line.startsWith("xft.dpi")) {
                        dpi = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                        break;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
            }
        } catch (IOException ignored) {
        }
        if (dpi < 0)
            dpi = super.getDPI();
        return dpi;
    }

    @Override
    public void registerThemeChanged(ThemeChangeListener callback) {
        if (callback == null)
            return;
        synchronized (this) {
            if (themeListenerThread == null) {
                themeListenerThread = new LinuxThemeListenerThread();
                themeListenerThread.start();
            }
        }
        themeListenerThread.addCallback(callback);
    }
}

class LinuxThemeListenerThread extends Thread {
    private final Collection<ThemeChangeListener> listeners = new HashSet<ThemeChangeListener>();
    final String gsettingsPath;
    private String lastTheme;

    public LinuxThemeListenerThread() {
        super("ThemeListenerThread");
        gsettingsPath = findGsettingsPath();
        setDaemon(true);
    }

    private String findGsettingsPath() {
        String PATH = System.getenv("PATH");
        if (PATH == null)
            PATH = "";
        PATH += File.pathSeparator + "/usr/bin";
        for (String location : PATH.split(File.pathSeparator)) {
            String exec = location + File.separator + "gsettings";
            if (new File(exec).isFile())
                return exec;
        }
        return null;
    }

    private void findInitialValue() {
        // Also consider org.gnome.desktop.interface color-scheme prefer-dark/prefer-light
        Process exec;
        try {
            exec = Runtime.getRuntime().exec(new String[]{gsettingsPath, "get", "org.gnome.desktop.interface", "gtk-theme"});
        } catch (IOException e) {
            return;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(exec.getInputStream(), "UTF-8"));
            setQuotedTheme(in.readLine());
        } catch (IOException ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean setQuotedTheme(String line) {
        if (line.length() < 2)
            return false;
        String newTheme = line.substring(1, line.length() - 1);
        if (!newTheme.equals(lastTheme)) {
            lastTheme = newTheme;
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        if (gsettingsPath == null)
            return;
        findInitialValue();
        fireThemeUpdate();
        BufferedReader in = null;
        try {
            Process exec = Runtime.getRuntime().exec(new String[]{gsettingsPath, "monitor", "org.gnome.desktop.interface", "gtk-theme"});
            in = new BufferedReader(new InputStreamReader(exec.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("'")) {
                    if (setQuotedTheme(line))
                        fireThemeUpdate();
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private synchronized void fireThemeUpdate() {
        if (lastTheme != null && !listeners.isEmpty())
            for (ThemeChangeListener listener : listeners)
                listener.themeChanged(lastTheme);
    }

    synchronized void addCallback(ThemeChangeListener callback) {
        listeners.add(callback);
        fireThemeUpdate();
    }
}