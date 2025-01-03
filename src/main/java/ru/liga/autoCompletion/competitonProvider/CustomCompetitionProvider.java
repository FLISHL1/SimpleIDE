package ru.liga.autoCompletion.competitonProvider;

import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;

public class CustomCompetitionProvider extends DefaultCompletionProvider {

    public String getAlreadyEnteredText(RSyntaxTextArea comp) {
        try {
            String text = comp.getText();
            int pos = comp.getCaretPosition();
            int caretOffsetFromLineStart = comp.getCaretOffsetFromLineStart();
            return comp.getText(pos - caretOffsetFromLineStart, caretOffsetFromLineStart);

        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

    }
}
