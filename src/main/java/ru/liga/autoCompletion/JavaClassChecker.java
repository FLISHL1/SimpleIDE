package ru.liga.autoCompletion;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;

public class JavaClassChecker {
    public boolean isClassName(String context, CompilationUnit cu) {
        return isImportJavaClassName(context, cu)
                || isJavaLangClass(context);
    }

    public boolean isImportJavaClassName(String context, CompilationUnit cu) {
        return cu.findAll(ImportDeclaration.class).stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + context));
    }



    public boolean isJavaLangClass(String context) {
        try {
            Class.forName("java.lang." + context);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
