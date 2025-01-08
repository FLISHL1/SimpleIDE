package ru.liga.utils;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RunnerJavaFile implements Runnable {
    private final JScrollPane consoleScrollPane;
    private final ExtractorPublicClassName extractorClassName;
    private final RSyntaxTextArea textArea;
    private final ExecutorService executorService;
    private Future<?> currentTask = null;
    private Process currentProcess;

    public RunnerJavaFile(JScrollPane consoleScrollPane, ExtractorPublicClassName extractorClassName, RSyntaxTextArea textArea) {
        this.consoleScrollPane = consoleScrollPane;
        this.extractorClassName = extractorClassName;
        this.textArea = textArea;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void run() {
        runJavaFile();
    }

    public void runWithSingleThread() {
        if (currentProcess != null) {
            currentProcess.destroy(); // Завершение предыдущего процесса
        }
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = executorService.submit(this);
    }

    private void runJavaFile() {
        JTextArea consoleArea = (JTextArea) consoleScrollPane.getViewport().getView();

        consoleArea.setText("");
        String code = textArea.getText();
        String className = extractorClassName.extractClassName(code);

        if (className != null) {
            File tempFile = new File(className + ".java");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);
            } catch (IOException e) {
                consoleArea.append("Error writing file: " + e.getMessage() + "\n");
                return;
            }

            try {
                // Компиляция
                currentProcess = Runtime.getRuntime().exec("javac " + tempFile.getAbsolutePath());
                currentProcess.waitFor();
                if (currentProcess.exitValue() == 0) {
                    currentProcess = Runtime.getRuntime().exec("java " + className);

                    consoleArea.setForeground(Color.decode("#F3F3F3"));
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                    String line;
                    while ((line = inputReader.readLine()) != null) {
                        consoleArea.append(line + "\n");
                    }
                    inputReader.close();
                    currentProcess.waitFor();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream()));
                    String line;
                    consoleArea.setForeground(Color.decode("#FF0000"));
                    while ((line = errorReader.readLine()) != null) {
                        consoleArea.append(line + "\n");
                    }
                    errorReader.close();
                    consoleArea.append("Compilation failed.\n");
                }
            } catch (Exception e) {
                consoleArea.setForeground(Color.decode("#FF0000"));
                consoleArea.append("Error: " + e.getMessage() + "\n");
            }
        } else {
            consoleArea.append("No public class found.\n");
        }
    }
}


