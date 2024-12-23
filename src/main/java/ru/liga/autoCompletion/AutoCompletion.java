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

import javax.swing.text.BadLocationException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCompletion {
    private final DefaultCompletionProvider provider;

    public AutoCompletion(RSyntaxTextArea textArea) {
        provider = new DefaultCompletionProvider();

        addDefaultKeywords(provider); // Добавляем ключевые слова
        addJavaStandardLibraryCompletions(provider); // Добавляем базовые классы

        // Добавляем динамическую обработку текста
        textArea.addCaretListener(e -> {
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
        });

        org.fife.ui.autocomplete.AutoCompletion ac = new org.fife.ui.autocomplete.AutoCompletion(provider);
        ac.setShowDescWindow(true);
        ac.install(textArea);
    }


    private void addDefaultKeywords(DefaultCompletionProvider provider) {
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

    private void addJavaStandardLibraryCompletions(DefaultCompletionProvider provider) {
        // Список стандартных классов Java, которые вы хотите добавить
        Class<?>[] standardClasses = {
                java.lang.System.class, java.lang.Math.class, java.lang.String.class, java.lang.Integer.class, java.util.ArrayList.class
        };

        for (Class<?> cls : standardClasses) {
            // Добавляем имя класса
            provider.addCompletion(new BasicCompletion(provider, cls.getSimpleName()));

            // Добавляем методы класса
            for (Method method : cls.getDeclaredMethods()) {
                StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
                Class<?>[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    methodSignature.append(params[i].getSimpleName());
                    if (i < params.length - 1) {
                        methodSignature.append(", ");
                    }
                }
                methodSignature.append(")");
                provider.addCompletion(new BasicCompletion(provider, methodSignature.toString()));
            }

            // Добавляем поля класса
            for (Field field : cls.getDeclaredFields()) {
                provider.addCompletion(new BasicCompletion(provider, field.getName()));
            }
        }
    }

    private String getLastWordBeforeDot(String text) {
        String regex = "\\b(\\w+)\\s*\\.$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1); // Возвращает имя переменной перед точкой
        }
        return null; // Если не найдено
    }

    private void addStaticCompletions(Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                addClassCompletionsWithDescriptions(method);
            }
        }

        for (Field field : cls.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                addFieldCompletionsWithDescriptions(field);
            }
        }
    }

    private void addClassCompletionsWithDescriptions(Method method) {
        StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            methodSignature.append(params[i].getSimpleName());
            if (i < params.length - 1) {
                methodSignature.append(", ");
            }
        }
        methodSignature.append(")");

        // Создаем FunctionCompletion
        FunctionCompletion completion = new FunctionCompletion(provider, methodSignature.toString(), method.getReturnType().getSimpleName());

        // Описание
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

        // Устанавливаем описание
        completion.setShortDescription(description.toString());
        provider.addCompletion(completion);

    }

    private void addFieldCompletionsWithDescriptions(Field field) {
        // Создаем VariableCompletion
        VariableCompletion completion = new VariableCompletion(provider, field.getName(), field.getType().getSimpleName());

        // Описание
        String description = "<html><b>Field:</b> " + field.getName() + "<br>" +
                "<b>Type:</b> " + field.getType().getSimpleName() + "</html>";

        // Устанавливаем описание
        completion.setShortDescription(description);
        provider.addCompletion(completion);
    }

    private boolean isClassName(String context, CompilationUnit cu) {
        // Проверяем среди импортов
        return cu.findAll(com.github.javaparser.ast.ImportDeclaration.class).stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + context))
                || isJavaLangClass(context);
    }

    // Проверяем среди стандартных классов java.lang
    private boolean isJavaLangClass(String context) {
        try {
            Class.forName("java.lang." + context);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void updateCompletionsForContext(String context, String fullText) {
        provider.clear(); // Очищаем старые подсказки

        try {
            // Анализируем код с использованием JavaParser
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = parser.parse(fullText);
            CompilationUnit cu = parseResult.getResult().get();

            Map<String, ClassOrInterfaceDeclaration> userDefinedClasses = new HashMap<>();
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                userDefinedClasses.put(cls.getNameAsString(), cls);
            });

            // Проверяем, является ли контекст классом
            if (isClassName(context, cu)) {
                try {
                    Class<?> cls = Class.forName("java.lang." + context); // Пробуем java.lang
                    addStaticCompletions(cls);
                } catch (ClassNotFoundException e) {
                    System.err.println("Класс не найден для статических подсказок: " + context);
                }
            } else {
                // Поиск переменной и её типа
                cu.findAll(VariableDeclarator.class).forEach(variable -> {
                    if (variable.getNameAsString().equals(context)) {
                        String typeName = variable.getTypeAsString();

                        if (userDefinedClasses.containsKey(typeName)) {
                            addUserDefinedClassCompletions(userDefinedClasses.get(typeName));
                        } else {
                            try {
                                Class<?> cls = Class.forName(typeName.startsWith("java.lang.") ? typeName : "java.lang." + typeName);
                                addClassCompletions(cls); // Добавляем методы и поля переменной
                            } catch (ClassNotFoundException e) {
                                System.err.println("Класс не найден для контекста: " + typeName);
                            }
                        }
                    }
                });

                // Обрабатываем пользовательские вложенные классы
                addNestedClassCompletions(context, cu);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addNestedClassCompletions(String context, CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> nestedClasses = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration nestedClass : nestedClasses) {
            if (nestedClass.getNameAsString().equals(context)) {
                // Добавляем методы вложенного класса
                nestedClass.findAll(MethodDeclaration.class).forEach(method -> {

                    provider.addCompletion(new BasicCompletion(provider, method.getNameAsString() + "()"));
                });

                // Добавляем поля вложенного класса
                nestedClass.getFields().forEach(field -> {
                    field.getVariables().forEach(variable -> {
                        provider.addCompletion(new BasicCompletion(provider, variable.getNameAsString()));
                    });
                });
            }
        }
    }

    private void addUserDefinedClassCompletions(ClassOrInterfaceDeclaration userClass) {
        // Добавляем методы пользовательского класса
        userClass.findAll(MethodDeclaration.class).forEach(method -> {

            provider.addCompletion(new BasicCompletion(provider, method.getNameAsString() + "()"));
        });

        // Добавляем поля пользовательского класса
        userClass.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                provider.addCompletion(new BasicCompletion(provider, variable.getNameAsString()));
            });
        });
    }

    private void addClassCompletions(Class<?> cls) {
        for (Method method : cls.getMethods()) {
            addClassCompletionsWithDescriptions(method);
        }

        for (Field field : cls.getFields()) { // Получаем публичные поля
            addFieldCompletionsWithDescriptions(field);
        }
    }
}
