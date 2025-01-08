package ru.liga.autoCompletion;

import com.github.javaparser.JavaParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.autocomplete.Completion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.liga.autoCompletion.competitonProvider.CustomCompetitionProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoCompletionJavaTest {

    private RSyntaxTextArea textArea;
    private CustomCompetitionProvider provider;

    @BeforeEach
    void setUp() {
        textArea = new RSyntaxTextArea();
        provider = new CustomCompetitionProvider();
        AutoCompletionJava autoCompletion = new AutoCompletionJava(new JavaParser(), provider, textArea, new JavaClassChecker(), new ExtractorJavaClass(new JavaClassChecker()), new ExtractorDescription());
        autoCompletion.start();
    }

    @Test
    void testDefaultKeywordsAdded() {
        // Arrange
        textArea.setText("pub"); // Симулируем ввод текста
        textArea.setCaretPosition(3); // Устанавливаем позицию курсора


        // Assert
        List<Completion> completions = provider.getCompletions(textArea);
        assertTrue(completions.stream().anyMatch(c -> c.getInputText().equals("public")),
                "Keyword 'public' should be in completions.");
    }

    @Test
    void testStaticCompletionsForMath() {
        // Arrange
        textArea.setText("Math.");
        textArea.setCaretPosition(5);


        // Assert
        List<Completion> completions = provider.getCompletions(textArea);

        assertTrue(completions.stream().anyMatch(c -> c.getInputText().startsWith("abs")),
                "Static method 'abs' from Math class should be in completions.");
        assertTrue(completions.stream().anyMatch(c -> c.getInputText().startsWith("sqrt")),
                "Static method 'sqrt' from Math class should be in completions.");
    }

    @Test
    void testUserDefinedClassCompletions() {
        // Arrange
        String code = "class CustomClass { void myMethod() {} int myField; void main(){ ";
        textArea.setText(code);
        textArea.setCaretPosition(code.length());
        textArea.append("\nCustomClass.  }}");
        textArea.setCaretPosition(78);

        // Assert
        List<Completion> completions = provider.getCompletions(textArea);
        assertTrue(completions.stream().anyMatch(c -> c.getInputText().equals("myMethod()")),
                "Method 'myMethod()' from CustomClass should be in completions.");
        assertTrue(completions.stream().anyMatch(c -> c.getInputText().equals("myField")),
                "Field 'myField' from CustomClass should be in completions.");
    }

    @Test
    void testClearProviderWhenDotNotFound() {
        // Arrange
        textArea.setText("SomeText");
        textArea.setCaretPosition(8);


        // Assert
        List<Completion> completions = provider.getCompletions(textArea);
        assertTrue(completions.isEmpty(), "Provider should be cleared if no dot is found.");
    }
}
