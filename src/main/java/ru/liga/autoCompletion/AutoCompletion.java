package ru.liga.autoCompletion;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCompletion {
    private final DefaultCompletionProvider provider;

    public AutoCompletion(RSyntaxTextArea textArea) {
        provider = new DefaultCompletionProvider();
        textArea.addCaretListener(createCaretListener(textArea));
        setupAutoComplete(textArea);
    }

    private void setupAutoComplete(RSyntaxTextArea textArea) {
        org.fife.ui.autocomplete.AutoCompletion ac = new org.fife.ui.autocomplete.AutoCompletion(provider);
        ac.setShowDescWindow(true);
        ac.install(textArea);
    }

    private CaretListener createCaretListener(RSyntaxTextArea textArea) {
        return e -> {
            try {
                int pos = textArea.getCaretPosition();
                String textUpToCaret = textArea.getText(0, pos);

                String context = getLastWordBeforeDot(textUpToCaret);
                if (context != null) {
                    System.out.println(context);
                    updateCompletionsForContext(context, textArea.getText());
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        };
    }


/*    private void addDefaultKeywords(DefaultCompletionProvider provider) {
        String[] keywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
                "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
                "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
                "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
                "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        };

        for (String keyword : keywords) {
            provider.addCompletion(new BasicCompletion(provider, keyword));
        }
    }
*/

    private String getLastWordBeforeDot(String text) {
        String regex = "\\b(\\w+)\\s*\\.$";
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
        return cu.findAll(com.github.javaparser.ast.ImportDeclaration.class).stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + context))
                || isJavaLangClass(context);
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
        try {
            provider.clear();
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = parser.parse(fullText);
            CompilationUnit cu = parseResult.getResult().get();

            Map<String, ClassOrInterfaceDeclaration> userDefinedClasses = new HashMap<>();
            cu.findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(cls -> userDefinedClasses.put(cls.getNameAsString(), cls));

            if (isClassName(context, cu)) {
                try {
                    Class<?> cls = Class.forName("java.lang." + context);
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
                                Class<?> cls = Class.forName(typeName.startsWith("java.lang.") ? typeName : "java.lang." + typeName);
                                addClassCompletions(cls);
                            } catch (ClassNotFoundException e) {
                                System.err.println("Класс не найден для контекста: " + typeName);
                            }
                        }
                });
                addNestedClassCompletions(context, cu);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
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
