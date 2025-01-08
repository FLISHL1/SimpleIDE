package ru.liga.utils.fileFilter;

import javax.swing.filechooser.FileNameExtensionFilter;

public class JavaFileFilter {
    private final FileNameExtensionFilter fileNameExtensionFilterForJavaFile;

    public JavaFileFilter() {
        fileNameExtensionFilterForJavaFile = new FileNameExtensionFilter("Java File", "java");
    }

    public FileNameExtensionFilter getFileNameExtensionFilterForJavaFile() {
        return fileNameExtensionFilterForJavaFile;
    }
}
