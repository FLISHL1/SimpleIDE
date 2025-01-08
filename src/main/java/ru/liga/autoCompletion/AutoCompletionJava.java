package ru.liga.autoCompletion;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
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
    private final JavaClassChecker classChecker;
    private final ExtractorJavaClass extractorJavaClass;
    private final ExtractorDescription extractorDescription;

    public AutoCompletionJava(JavaParser javaParser, CustomCompetitionProvider provider, RSyntaxTextArea textArea,
                              JavaClassChecker classChecker, ExtractorJavaClass extractorJavaClass,
                              ExtractorDescription extractorDescription) {
        this.provider = provider;
        this.javaParser = javaParser;
        this.textArea = textArea;
        this.classChecker = classChecker;
        this.extractorJavaClass = extractorJavaClass;
        this.extractorDescription = extractorDescription;
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
        FunctionCompletion completion = new FunctionCompletion(provider, methodSignature.toString(), method.getReturnType().getSimpleName());
        completion.setShortDescription(extractorDescription.getMethodDescription(method));
        provider.addCompletion(completion);

    }

    private void addFieldCompletionsWithDescriptions(Field field) {
        VariableCompletion completion = new VariableCompletion(provider, field.getName(), field.getType().getSimpleName());
        completion.setShortDescription(extractorDescription.getFieldDescription(field));
        provider.addCompletion(completion);
    }

    private void updateCompletionsForContext(String context, String fullText) {
        provider.clear();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(fullText);
        CompilationUnit cu = parseResult.getResult().orElseThrow(NullPointerException::new);
        System.out.println(context);
        Map<String, ClassOrInterfaceDeclaration> userDefinedClasses = new HashMap<>();
        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(cls -> userDefinedClasses.put(cls.getNameAsString(), cls));
        if (classChecker.isClassName(context, cu)) {
            try {
                addStaticCompletions(extractorJavaClass.getJavaClass(context, cu));
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
                                addClassCompletions(extractorJavaClass.getJavaClass(typeName, cu));
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
