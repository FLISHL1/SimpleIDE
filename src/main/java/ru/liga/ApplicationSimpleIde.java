package ru.liga;

import ru.liga.views.MainWindow;

import javax.swing.*;

public class ApplicationSimpleIde {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}