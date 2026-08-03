package com.panayotis.appenh;

import com.panayotis.appenh.AFileChooser.FileSelectionMode;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

interface FileChooserFactory {
    Result showOpenDialog(Component parent, String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode, List<FileNameExtensionFilter> filters);

    Result showSaveDialog(Component parent, String title, String buttonTitle, File directory, String file, List<FileNameExtensionFilter> filters);

    /** The chosen file(s) plus the extension filter the user had selected when confirming (null if none/unknown). */
    final class Result {
        final Collection<File> files;
        final FileNameExtensionFilter filter;

        Result(Collection<File> files, FileNameExtensionFilter filter) {
            this.files = files == null ? Collections.<File>emptyList() : files;
            this.filter = filter;
        }
    }
}
