package ru.liga.views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConsoleWindowAdapter extends WindowAdapter {
    private final MainWindow mainWindow;
    private final ConsoleWindow consoleWindow;

    public ConsoleWindowAdapter(MainWindow mainWindow, ConsoleWindow consoleWindow){
        super();
        this.consoleWindow = consoleWindow;
        this.mainWindow = mainWindow;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        consoleWindow.close();
        mainWindow.toggleConsoleWithWindowText();
    }
}
