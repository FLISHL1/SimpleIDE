package ru.liga;

import ru.liga.utils.AutoSaverLastOpenFileUtil;
import ru.liga.utils.ExtractorPublicClassName;
import ru.liga.utils.StateManager;
import ru.liga.views.MainWindow;

import javax.swing.*;

public class ApplicationSimpleIde {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow(
                new StateManager(),
                new ExtractorPublicClassName(),
                new AutoSaverLastOpenFileUtil(new StateManager())
        ));
    }
}