package com.panayotis.appenh;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
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
    private Component parent;
    private FileSelectionMode mode;
    private final List<FileNameExtensionFilter> filters = new ArrayList<>();
    private FileNameExtensionFilter selectedFilter;

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

    /** The window the dialog should be modal-for / centered on (may be any component within it; null for none). */
    public AFileChooser parent(Component parent) {
        this.parent = parent;
        return this;
    }

    public File loadSingle() {
        FileChooserFactory.Result res = getFactory().showOpenDialog(parent, title, loadButton, directory, false, mode, filters);
        selectedFilter = res.filter;
        File result = res.files.isEmpty() ? null : res.files.iterator().next();
        if (result != null && rememberPath && (result.isFile() || result.isDirectory()))
            directory = result.isFile() ? result.getParentFile() : result;
        return result;
    }

    public Collection<File> loadMulti() {
        FileChooserFactory.Result res = getFactory().showOpenDialog(parent, title, loadButton, directory, true, mode, filters);
        selectedFilter = res.filter;
        Collection<File> files = res.files;
        if (!files.isEmpty() && rememberPath) {
            File fileC = files.iterator().next();
            directory = fileC.isFile() ? fileC.getParentFile() : fileC;
        }
        return files;
    }

    public File save() {
        FileChooserFactory.Result res = getFactory().showSaveDialog(parent, title, saveButton, directory, this.file, filters);
        selectedFilter = res.filter;
        File result = res.files.isEmpty() ? null : res.files.iterator().next();
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

    /** The extension filter the user had selected when confirming the last dialog, or null if none. */
    public FileNameExtensionFilter selectedFilter() {
        return selectedFilter;
    }

    static FileNameExtensionFilter asExtensionFilter(javax.swing.filechooser.FileFilter ff) {
        return ff instanceof FileNameExtensionFilter ? (FileNameExtensionFilter) ff : null;
    }

    public static void injectCustomVisuals(InjectedVisuals injectedVisuals) {
        AFileChooser.injectedVisuals = injectedVisuals;
    }

    private FileChooserFactory getFactory() {
        if (EnhancerManager.getDefault() instanceof FileChooserFactory)
            return (FileChooserFactory) EnhancerManager.getDefault();
        else
            return swingFactory;
    }

    public AFileChooser filter(String extension, String description) {
        if (extension != null && !extension.isEmpty()) {
            if (description == null || description.isEmpty())
                description = "Files with extension " + extension;
            filters.add(new FileNameExtensionFilter(description, extension));
        }
        return this;
    }

    /** Add one filter that matches several extensions at once (a leading dot in each is optional). */
    public AFileChooser filter(String[] extensions, String description) {
        if (extensions != null && extensions.length > 0) {
            String[] clean = new String[extensions.length];
            for (int i = 0; i < extensions.length; i++) {
                String e = extensions[i] == null ? "" : extensions[i];
                clean[i] = e.startsWith(".") ? e.substring(1) : e;
            }
            if (description == null || description.isEmpty())
                description = "Files";
            filters.add(new FileNameExtensionFilter(description, clean));
        }
        return this;
    }

    public enum FileSelectionMode {
        FilesOnly, DirectoriesOnly, FilesAndDirectories
    }

    /** The plain Swing implementation; also the fallback a platform {@code FileChooserFactory} delegates to. */
    static final FileChooserFactory swingFactory = new FileChooserFactory() {
        @Override
        public Result showOpenDialog(Component parent, String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode, List<FileNameExtensionFilter> filters) {
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
            fc.showOpenDialog(parent);
            Collection<File> files = fc.getSelectedFiles().length > 0
                    ? Arrays.asList(fc.getSelectedFiles())
                    : (fc.getSelectedFile() == null
                    ? Collections.<File>emptyList() : Collections.singletonList(fc.getSelectedFile()));
            return new Result(files, asExtensionFilter(fc.getFileFilter()));
        }

        @Override
        public Result showSaveDialog(Component parent, String title, String buttonTitle, File directory, String file, List<FileNameExtensionFilter> filters) {
            JFileChooser fc = new JFileChooser(directory);
            if (buttonTitle != null) fc.setApproveButtonText(buttonTitle);
            if (file != null) fc.setSelectedFile(new File(directory, file));
            if (title != null) fc.setDialogTitle(title);
            filters.forEach(fc::addChoosableFileFilter);
            if (!filters.isEmpty())
                fc.setFileFilter(filters.get(0));
            if (injectedVisuals != null)
                injectedVisuals.willShow(fc);
            fc.showOpenDialog(parent);
            File sel = fc.getSelectedFile();
            return new Result(sel == null ? Collections.<File>emptyList() : Collections.singletonList(sel),
                    asExtensionFilter(fc.getFileFilter()));
        }
    };

    public interface InjectedVisuals {
        void willShow(Object fileChooser);
    }
}
