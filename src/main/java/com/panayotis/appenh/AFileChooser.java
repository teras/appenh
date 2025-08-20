package com.panayotis.appenh;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.*;

public class AFileChooser {
    private static InjectedVisuals injectedVisuals;

    private String title;
    private String saveButton;
    private String loadButton;
    private File directory = new File(System.getProperty("user.home"));
    private String file;
    private boolean rememberPath = true;
    private boolean forceExtension = false;
    private FileSelectionMode mode;
    private final List<FileNameExtensionFilter> filters = new ArrayList<>();

    public AFileChooser() {
    }

    public AFileChooser title(String title) {
        this.title = title;
        return this;
    }

    public AFileChooser loadButtonTitle(String buttonTitle) {
        this.loadButton = buttonTitle;
        return this;
    }

    public AFileChooser saveButtonTitle(String buttonTitle) {
        this.saveButton = buttonTitle;
        return this;
    }

    public AFileChooser directory(File directory) {
        if (directory != null)
            this.directory = directory;
        return this;
    }

    public AFileChooser file(String file) {
        this.file = file;
        return this;
    }

    public AFileChooser mode(FileSelectionMode mode) {
        this.mode = mode;
        return this;
    }

    public AFileChooser rememberPath(boolean rememberSelection) {
        this.rememberPath = rememberSelection;
        return this;
    }

    public AFileChooser forceExtension(boolean forceExtension) {
        this.forceExtension = forceExtension;
        return this;
    }

    public File loadSingle() {
        Collection<File> resultC = getFactory().showOpenDialog(title, loadButton, directory, false, mode, filters);
        File result = resultC == null || resultC.isEmpty() ? null : resultC.iterator().next();
        if (result != null && rememberPath && (result.isFile() || result.isDirectory()))
            directory = result.isFile() ? result.getParentFile() : result;
        return result;
    }

    public Collection<File> loadMulti() {
        Collection<File> files = getFactory().showOpenDialog(title, loadButton, directory, true, mode, filters);
        if (files != null && !files.isEmpty() && rememberPath) {
            File fileC = files.iterator().next();
            directory = fileC.isFile() ? fileC.getParentFile() : fileC;
        }
        return files;
    }

    public File save() {
        File result = getFactory().showSaveDialog(title, saveButton, directory, this.file, filters);
        if (result != null) {
            if (rememberPath)
                directory = result.isDirectory() ? result : result.getParentFile();
            if (forceExtension && !filters.isEmpty()) {
                String extension = filters.get(0).getExtensions()[0];
                if (!result.getName().toLowerCase().endsWith("." + extension))
                    result = new File(result.getPath() + "." + extension);
            }
            this.file = result.getName();
        }
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

    public AFileChooser filter(String extension, String description) {
        if (extension != null && !extension.isEmpty()) {
            if (description == null || description.isEmpty())
                description = "Files with extension " + extension;
            filters.add(new FileNameExtensionFilter(description, extension));
        }
        return this;
    }

    public enum FileSelectionMode {
        FilesOnly, DirectoriesOnly, FilesAndDirectories
    }

    private static final FileChooserFactory defaultFactory = new FileChooserFactory() {
        @Override
        public Collection<File> showOpenDialog(String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode, List<FileNameExtensionFilter> filters) {
            JFileChooser fc = new JFileChooser(directory);
            if (mode == FileSelectionMode.FilesAndDirectories)
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            else if (mode == FileSelectionMode.DirectoriesOnly)
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            else
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (buttonTitle != null) fc.setApproveButtonText(buttonTitle);
            if (title != null) fc.setDialogTitle(title);
            fc.setMultiSelectionEnabled(openMulti);
            filters.forEach(fc::addChoosableFileFilter);
            if (!filters.isEmpty()) fc.setFileFilter(filters.get(0));
            if (injectedVisuals != null) injectedVisuals.willShow(fc);
            fc.showOpenDialog(null);
            return fc.getSelectedFiles().length > 0
                    ? Arrays.asList(fc.getSelectedFiles())
                    : (fc.getSelectedFile() == null
                    ? Collections.<File>emptyList() : Collections.singletonList(fc.getSelectedFile()));
        }

        @Override
        public File showSaveDialog(String title, String buttonTitle, File directory, String file, List<FileNameExtensionFilter> filters) {
            JFileChooser fc = new JFileChooser(directory);
            if (buttonTitle != null) fc.setApproveButtonText(buttonTitle);
            if (file != null) fc.setSelectedFile(new File(directory, file));
            if (title != null) fc.setDialogTitle(title);
            filters.forEach(fc::addChoosableFileFilter);
            if (!filters.isEmpty())
                fc.setFileFilter(filters.get(0));
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
