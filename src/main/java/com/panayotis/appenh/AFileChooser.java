package com.panayotis.appenh;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class AFileChooser {
    private static InjectedVisuals injectedVisuals;

    private String title;
    private String buttonTitle;
    private File directory = new File(System.getProperty("user.home"));
    private String file;
    private boolean rememberSelection;
    private FileSelectionMode mode;

    public AFileChooser() {
    }

    public String getTitle() {
        return title;
    }

    public AFileChooser setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getButtonTitle() {
        return buttonTitle;
    }

    public AFileChooser setButtonTitle(String buttonTitle) {
        this.buttonTitle = buttonTitle;
        return this;
    }

    public File getDirectory() {
        return directory;
    }

    public AFileChooser setDirectory(File directory) {
        this.directory = directory;
        return this;
    }

    public String getFile() {
        return file;
    }

    public AFileChooser setFile(String file) {
        this.file = file;
        return this;
    }

    public FileSelectionMode getMode() {
        return mode;
    }

    public AFileChooser setMode(FileSelectionMode mode) {
        this.mode = mode;
        return this;
    }

    public boolean shouldRememberSelection() {
        return rememberSelection;
    }

    public AFileChooser setRememberSelection(boolean rememberSelection) {
        this.rememberSelection = rememberSelection;
        return this;
    }

    public File openSingle() {
        Collection<File> resultC = getFactory().showOpenDialog(title, buttonTitle, directory, false, mode);
        File result = resultC == null || resultC.isEmpty() ? null : resultC.iterator().next();
        if (result != null && rememberSelection && (result.isFile() || result.isDirectory()))
            directory = result.isFile() ? result.getParentFile() : result;
        return result;
    }

    public Collection<File> openMulti() {
        Collection<File> files = getFactory().showOpenDialog(title, buttonTitle, directory, true, mode);
        if (files != null && !files.isEmpty() && rememberSelection) {
            File fileC = files.iterator().next();
            directory = fileC.isFile() ? fileC.getParentFile() : fileC;
        }
        return files;
    }

    public File save() {
        File result = getFactory().showSaveDialog(title, buttonTitle, directory, this.file);
        if (result != null && rememberSelection && (result.isFile() || result.isDirectory()))
            directory = result.isFile() ? result.getParentFile() : result;
        return result;
    }

    public static void injectCustomVisuals(InjectedVisuals injectedVisuals) {
        AFileChooser.injectedVisuals = injectedVisuals;
    }

    private FileChooserFactory getFactory() {
        if (EnhancerManager.getDefault() instanceof FileChooserFactory)
            return (FileChooserFactory) EnhancerManager.getDefault();
        else
            return defaultFactory;
    }

    public enum FileSelectionMode {
        FilesOnly, DirectoriesOnly, FilesAndDirectories
    }

    private static final FileChooserFactory defaultFactory = new FileChooserFactory() {
        @Override
        public Collection<File> showOpenDialog(String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode) {
            JFileChooser fc = new JFileChooser(directory);
            if (mode == FileSelectionMode.FilesAndDirectories)
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            else if (mode == FileSelectionMode.DirectoriesOnly)
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            else
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setApproveButtonText(buttonTitle);
            fc.setDialogTitle(title);
            fc.setMultiSelectionEnabled(openMulti);
            if (injectedVisuals != null)
                injectedVisuals.willShow(fc);
            fc.showOpenDialog(null);
            return Arrays.asList(fc.getSelectedFiles());
        }

        @Override
        public File showSaveDialog(String title, String buttonTitle, File directory, String file) {
            JFileChooser fc = new JFileChooser(directory);
            fc.setApproveButtonText(buttonTitle);
            fc.setSelectedFile(new File(directory, file));
            fc.setDialogTitle(title);
            if (injectedVisuals != null)
                injectedVisuals.willShow(fc);
            fc.showOpenDialog(null);
            return fc.getSelectedFile();
        }
    };

    public interface InjectedVisuals {
        void willShow(Object fileChooser);
    }
}
