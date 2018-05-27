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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.UIManager;

class LinuxEnhancer extends DefaultEnhancer {

    @SuppressWarnings("UseSpecificCatch")
    private boolean setNoUglySystemLookAndFeel() {
        try {
            String name = UIManager.getSystemLookAndFeelClassName();
            if (name.contains("MetalLookAndFeel") || name.contains("GTKLookAndFeel"))
                return false;
            UIManager.setLookAndFeel(name);
            return true;
        } catch (Exception ex1) {
        }
        return false;
    }

    @Override
    public void setSafeLookAndFeel() {
        if (!setNoUglySystemLookAndFeel())
            setNimbusLookAndFeel();
    }

    @Override
    public void registerApplication(final String name_s, final String comment_s, final String... categories) {
        new Thread() {

            @Override
            public void run() {
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
                    File img128 = new File(System.getProperty("user.home"), ".cache/appenh/" + basename + ".png");
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
            }
        }.start();
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
            String num = new BigInteger(1, MessageDigest.getInstance("MD5").digest(text.getBytes("UTF-8"))).toString(16);
            while (num.length() < 32)
                num = "0" + num;
            return num;
        } catch (NoSuchAlgorithmException ex) {
        } catch (UnsupportedEncodingException ex) {
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
            } catch (IOException ex) {
            }
        return null;
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
