package ru.liga.utils;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoSaverLastOpenFileUtil {
    private final int AUTO_SAVE_DELAY = 1000;
    private final StateManager stateManager;
    private final ExecutorService executorService;
    private Timer autoSaveTimer;

    public AutoSaverLastOpenFileUtil(StateManager stateManager) {
        this.stateManager = stateManager;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void setupAutoSaveFeature(RSyntaxTextArea textArea) {
        autoSaveTimer = new Timer(AUTO_SAVE_DELAY, e -> executorService.execute(() -> saveFileAuto(textArea)));
        autoSaveTimer.setRepeats(false);
        textArea.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                resetAutoSaveTimer();
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                resetAutoSaveTimer();
            }
        });
    }

    private void resetAutoSaveTimer() {
        if (!autoSaveTimer.isRunning()) {
            autoSaveTimer.start(); // Старт таймера
        }
        autoSaveTimer.restart(); // Перезапуск таймера
    }

    private void saveFileAuto(RSyntaxTextArea textArea) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(stateManager.getLastFile()))) {
            textArea.write(writer);
            System.out.println("Автосохранение выполнено в файл");
        } catch (IOException e) {
            System.out.println(stateManager.getLastFile());
            System.err.println("Ошибка автосохранения: " + e.getMessage());
        }
    }
}
