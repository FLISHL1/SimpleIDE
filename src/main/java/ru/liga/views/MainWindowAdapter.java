package ru.liga.views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindowAdapter extends WindowAdapter {
    private final MainWindow mainWindow;

    public MainWindowAdapter(MainWindow mainWindow){
        super();
        this.mainWindow = mainWindow;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        mainWindow.saveAppState();
    }
}
