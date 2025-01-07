package ru.liga.autoCompletion;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import ru.liga.autoCompletion.competitonProvider.CustomCompetitionProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCompletionJava extends Thread {
    private final JavaParser javaParser;
    private final CustomCompetitionProvider provider;
    private final RSyntaxTextArea textArea;

    public AutoCompletionJava(JavaParser javaParser, CustomCompetitionProvider provider, RSyntaxTextArea textArea) {
        this.provider = provider;
        this.javaParser = javaParser;
        this.textArea = textArea;
    }

    @Override
    public void run() {
        textArea.addCaretListener(e -> updateProvider());
        setupAutoComplete();
    }


    private void setupAutoComplete() {
        AutoCompletion ac = new AutoCompletion(provider);
        new JavaLanguageSupport().install(textArea);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(true);
        ac.install(textArea);
    }

    private void updateProvider() {
        if (!provider.getAlreadyEnteredText(textArea).contains(".")) {
            provider.clear();
            addDefaultKeywords(provider);

        } else {
            String context = getLastWordBeforeDot(provider.getAlreadyEnteredText(textArea));

            if (context != null) {
                updateCompletionsForContext(context, textArea.getText());

            }
        }
    }


    private void addDefaultKeywords(DefaultCompletionProvider provider) {
        String[] keywords = {
                "abstract", "assert", "break", "case", "catch", "class",
                "const", "continue", "default", "do", "else", "enum", "extends", "final",
                "finally", "for", "goto", "if", "implements", "import", "instanceof",
                "interface", "new", "null", "package", "private", "protected",
                "public", "return", "static", "super", "switch", "synchronized",
                "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        };

        Arrays.stream(keywords).forEach(keyword -> provider.addCompletion(new BasicCompletion(provider, keyword)));
    }

    private String getLastWordBeforeDot(String text) {
        String regex = "^\\s*(\\w*\\.?+\\w+\\s*)\\.$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void addStaticCompletions(Class<?> cls) {
        Arrays.stream(cls.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                .forEach(this::addMethodCompletionsWithDescriptions);
        Arrays.stream(cls.getDeclaredFields())
                .filter(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .forEach(this::addFieldCompletionsWithDescriptions);
    }

    private void addMethodCompletionsWithDescriptions(Method method) {
        StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            methodSignature.append(params[i].getSimpleName());
            if (i < params.length - 1) {
                methodSignature.append(", ");
            }
        }
        methodSignature.append(")");

        StringBuilder description = new StringBuilder();
        description.append("<html><b>Method:</b> ").append(method.getName()).append("<br>");
        description.append("<b>Returns:</b> ").append(method.getReturnType().getSimpleName()).append("<br>");
        if (method.getParameterCount() > 0) {
            description.append("<b>Parameters:</b><ul>");
            for (Class<?> param : params) {
                description.append("<li>").append(param.getSimpleName()).append("</li>");
            }
            description.append("</ul>");
        } else {
            description.append("<b>Parameters:</b> None");
        }
        description.append("</html>");

        FunctionCompletion completion = new FunctionCompletion(provider, methodSignature.toString(), method.getReturnType().getSimpleName());
        completion.setShortDescription(description.toString());
        provider.addCompletion(completion);

    }

    private void addFieldCompletionsWithDescriptions(Field field) {
        VariableCompletion completion = new VariableCompletion(provider, field.getName(), field.getType().getSimpleName());
        String description = "<html><b>Field:</b> " + field.getName() + "<br>" +
                "<b>Type:</b> " + field.getType().getSimpleName() + "</html>";

        completion.setShortDescription(description);
        provider.addCompletion(completion);
    }

    private boolean isClassName(String context, CompilationUnit cu) {
        return isImportJavaClassName(context, cu)
                || isJavaLangClass(context);
    }

    private boolean isImportJavaClassName(String context, CompilationUnit cu) {
        return cu.findAll(ImportDeclaration.class).stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + context));
    }

    private String getImportJavaClassName(String context, CompilationUnit cu) {
        return cu.findAll(ImportDeclaration.class).stream()
                .filter(importDecl -> importDecl.getNameAsString().endsWith("." + context))
                .findFirst()
                .orElseThrow(ClassCastException::new).getNameAsString();
    }

    private boolean isJavaLangClass(String context) {
        try {
            Class.forName("java.lang." + context);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void updateCompletionsForContext(String context, String fullText) {

        provider.clear();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(fullText);
        CompilationUnit cu = parseResult.getResult().get();
        System.out.println(context);
        Map<String, ClassOrInterfaceDeclaration> userDefinedClasses = new HashMap<>();
        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(cls -> userDefinedClasses.put(cls.getNameAsString(), cls));

        if (isClassName(context, cu)) {
            try {
                Class<?> cls = Class.class;
                if (isJavaLangClass(context)) {
                    cls = Class.forName("java.lang." + context);
                } else if (isImportJavaClassName(context, cu)) {
                    cls = Class.forName(getImportJavaClassName(context, cu));
                } else {
                    throw new ClassNotFoundException();
                }
                addStaticCompletions(cls);
            } catch (ClassNotFoundException e) {
                System.err.println("Класс не найден для статических подсказок: " + context);
            }
        } else {
            cu.findAll(VariableDeclarator.class)
                    .stream()
                    .filter(variable -> variable.getNameAsString().equals(context))
                    .forEach(variable -> {
                        String typeName = variable.getTypeAsString();
                        if (userDefinedClasses.containsKey(typeName)) {
                            addUserDefinedClassCompletions(userDefinedClasses.get(typeName));
                        } else {
                            try {
                                Class<?> cls = Class.class;
                                if (isJavaLangClass(typeName)) {
                                    cls = Class.forName("java.lang." + typeName);
                                } else if (isImportJavaClassName(typeName, cu)) {
                                    cls = Class.forName(getImportJavaClassName(typeName, cu));
                                } else {
                                    throw new ClassNotFoundException();
                                }
                                addClassCompletions(cls);
                            } catch (ClassNotFoundException e) {
                                System.err.println("Класс не найден для контекста: " + typeName);
                            }
                        }
                    });
            addNestedClassCompletions(context, cu);
        }


    }

    private void addNestedClassCompletions(String context, CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> nestedClasses = cu.findAll(ClassOrInterfaceDeclaration.class);
        nestedClasses.stream()
                .filter(nestedClass -> nestedClass.getNameAsString().equals(context))
                .forEach(nestedClass -> {
                    nestedClass
                            .findAll(MethodDeclaration.class)
                            .forEach(method -> provider.addCompletion(new BasicCompletion(provider, method.getNameAsString() + "()")));
                    nestedClass.getFields()
                            .forEach(field -> field.getVariables()
                                    .forEach(variable -> provider.addCompletion(new BasicCompletion(provider, variable.getNameAsString()))));
                });
    }

    private void addUserDefinedClassCompletions(ClassOrInterfaceDeclaration userClass) {
        userClass.findAll(MethodDeclaration.class)
                .forEach(method -> provider.addCompletion(new BasicCompletion(provider, method.getNameAsString() + "()")));
        userClass.getFields()
                .forEach(field -> field.getVariables()
                        .forEach(variable -> provider.addCompletion(new BasicCompletion(provider, variable.getNameAsString()))));
    }

    private void addClassCompletions(Class<?> cls) {
        Arrays.stream(cls.getMethods()).forEach(this::addMethodCompletionsWithDescriptions);
        Arrays.stream(cls.getFields()).forEach(this::addFieldCompletionsWithDescriptions);
    }
}
