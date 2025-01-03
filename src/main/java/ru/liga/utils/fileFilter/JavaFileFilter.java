package ru.liga.utils.fileFilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class JavaFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return false;
        }
        String s = f.getName().toLowerCase();
        return s.endsWith(".java");
    }

    @Override
    public String getDescription() {
        return "*.java";
    }
}
