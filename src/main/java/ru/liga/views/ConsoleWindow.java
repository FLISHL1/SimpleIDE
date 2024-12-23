package ru.liga.views;


import ru.liga.adapter.ConsoleWindowAdapter;

import javax.swing.*;
import java.awt.*;

public class ConsoleWindow {
    private final JFrame consoleFrame;
    private final MainWindow mainWindow;

    public ConsoleWindow(MainWindow mainWindow) {
        consoleFrame = new JFrame("Console");
        consoleFrame.setSize(600, 400);
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
        consoleFrame.setVisible(true); // Показываем консоль
        mainWindow.toggleConsoleWithWindowText();
    }
}
