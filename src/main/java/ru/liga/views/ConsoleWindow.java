package ru.liga.views;


import ru.liga.adapter.ConsoleWindowAdapter;

import javax.swing.*;
import java.awt.*;

public class ConsoleWindow {
    private final int WIDTH = 600;
    private final int HEIGHT = 400;
    private final String title = "Console";
    private final JFrame consoleFrame;
    private final MainWindow mainWindow;

    public ConsoleWindow(MainWindow mainWindow) {
        consoleFrame = new JFrame(title);
        consoleFrame.setSize(WIDTH, HEIGHT);
        consoleFrame.setLayout(new BorderLayout());
        consoleFrame.addWindowListener(new ConsoleWindowAdapter(mainWindow, this));
        this.mainWindow = mainWindow;
    }
    public void close(){
        consoleFrame.setVisible(false);
    }

    public boolean isOpen() {
        return consoleFrame.isVisible();
    }

    public void show(JScrollPane consoleScrollPane) {
        consoleFrame.add(consoleScrollPane, BorderLayout.CENTER);
        consoleFrame.setVisible(true);
        mainWindow.toggleConsoleWithWindowText();
    }
}
