package com.panayotis.appenh;

import com.panayotis.appenh.AFileChooser.FileSelectionMode;

import java.io.File;
import java.util.Collection;

interface FileChooserFactory {
    Collection<File> showOpenDialog(String title, String buttonTitle, File directory, boolean openMulti, FileSelectionMode mode);

    File showSaveDialog(String title, String buttonTitle, File directory, String file);

}
