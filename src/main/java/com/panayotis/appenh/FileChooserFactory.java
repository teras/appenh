package com.panayotis.appenh;

import com.panayotis.appenh.AFileChooser.FileSelectionMode;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Collection;
import java.util.List;

interface FileChooserFactory {
    Collection<File> showOpenDialog(String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode, List<FileNameExtensionFilter> filters);

    File showSaveDialog(String title, String buttonTitle, File directory, String file, List<FileNameExtensionFilter> filters);

}
