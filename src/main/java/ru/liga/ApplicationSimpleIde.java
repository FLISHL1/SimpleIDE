package ru.liga;

import ru.liga.utils.AutoSaverLastOpenFileUtil;
import ru.liga.utils.ExtractorPublicClassName;
import ru.liga.utils.RunnerJavaFile;
import ru.liga.utils.StateManager;
import ru.liga.views.MainWindow;

import javax.swing.*;

public class ApplicationSimpleIde {
    public static void main(String[] args) {
        StateManager stateManager = new StateManager();
        SwingUtilities.invokeLater(() -> new MainWindow(
                stateManager,
                new ExtractorPublicClassName(),
                new AutoSaverLastOpenFileUtil(stateManager)
        ));
    }
}