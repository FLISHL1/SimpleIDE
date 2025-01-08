package ru.liga.autoCompletion;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;

public class ExtractorJavaClass {
    private final JavaClassChecker classChecker;

    public ExtractorJavaClass(JavaClassChecker javaClassChecker){
        this.classChecker = javaClassChecker;
    }

    public Class<?> getJavaClass(String context, CompilationUnit cu) throws ClassNotFoundException {
        Class<?> cls;
        if (classChecker.isJavaLangClass(context)) {
            cls = Class.forName("java.lang." + context);
        } else if (classChecker.isImportJavaClassName(context, cu)) {
            cls = Class.forName(getImportJavaClassName(context, cu));
        } else {
            throw new ClassNotFoundException();
        }
        return cls;
    }

    public String getImportJavaClassName(String context, CompilationUnit cu) {
        return cu.findAll(ImportDeclaration.class).stream()
                .filter(importDecl -> importDecl.getNameAsString().endsWith("." + context))
                .findFirst()
                .orElseThrow(ClassCastException::new).getNameAsString();
    }
}
