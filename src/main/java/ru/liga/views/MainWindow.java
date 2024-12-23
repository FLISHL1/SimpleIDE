package ru.liga.views;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import ru.liga.adapter.MainWindowAdapter;
import ru.liga.autoCompletion.AutoCompletion;
import ru.liga.utils.ExtractorPublicClassName;
import ru.liga.utils.StateManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class MainWindow {
    private final String FAMILY_FONT = "Monospaced";
    private final String title = "Simple Java IDE";
    private final RSyntaxTextArea textArea;
    private final JSlider fontSizeSlider;
    private final AutoCompletion autoCompletion;
    private final JScrollPane consoleScrollPane;
    private final JFrame mainFrame;
    private final ExtractorPublicClassName extractorClassName;
    private final StateManager stateManager;
    private ConsoleWindow consoleWindow;
    private final int AUTO_SAVE_DELAY = 1000; // 5 секунд
    private Timer autoSaveTimer; // Таймер для автосохранения

    public MainWindow() {
        mainFrame = new JFrame(title);
        stateManager = new StateManager();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLayout(new BorderLayout());
        extractorClassName = new ExtractorPublicClassName();

        // Верхняя панель с кнопками
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newButton = new JButton("New");
        JButton openButton = new JButton("Open");
        JButton saveButton = new JButton("Save");
        JButton runButton = new JButton("Run");
        JButton consoleButton = new JButton("Show Console"); // Кнопка для открытия консоли

        topPanel.add(newButton);
        topPanel.add(openButton);
        topPanel.add(saveButton);
        topPanel.add(runButton);
        topPanel.add(consoleButton); // Добавление кнопки консоли
        mainFrame.add(topPanel, BorderLayout.NORTH);

        // Текстовая область с подсветкой синтаксиса
        textArea = createTextArea();
        autoCompletion = new AutoCompletion(textArea);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        mainFrame.add(sp, BorderLayout.CENTER);

        // Область консоли
        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setPreferredSize(new Dimension(800, 150)); // Определите высоту консоли
        mainFrame.add(consoleScrollPane, BorderLayout.SOUTH);
        consoleWindow = new ConsoleWindow(this);

        // Правая панель для настроек
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JLabel fontSizeLabel = new JLabel("Font Size:");
        fontSizeSlider = new JSlider(10, 30, 12);
        fontSizeSlider.setMajorTickSpacing(5);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);

        rightPanel.add(fontSizeLabel);
        rightPanel.add(fontSizeSlider);

        mainFrame.add(rightPanel, BorderLayout.EAST);


        // Обработчики событий для кнопок
        newButton.addActionListener(e -> textArea.setText(""));
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveFile());
        runButton.addActionListener(e -> runJavaFile());
        consoleButton.addActionListener(e -> {
            consoleWindow.show(consoleScrollPane);
        }); // Обработчик для кнопки консоли
        fontSizeSlider.addChangeListener(e -> textArea.setFont(new Font(FAMILY_FONT, Font.PLAIN, fontSizeSlider.getValue())));

        // Загрузка состояния приложения
        loadAppState();

        // Сохранение состояния при закрытии
        mainFrame.addWindowListener(new MainWindowAdapter(this));

        mainFrame.setVisible(true);
        setupAutoSaveFeature();
    }

    private RSyntaxTextArea createTextArea() {
        final RSyntaxTextArea textArea;
        textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        return textArea;
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                textArea.read(reader, null);
                stateManager.saveLastOpenedFile(file.getAbsolutePath()); // Сохранить путь к файлу
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                textArea.write(writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setupAutoSaveFeature() {
        autoSaveTimer = new Timer(AUTO_SAVE_DELAY, e -> saveFileAuto());
        autoSaveTimer.setRepeats(false); // Таймер срабатывает только один раз
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
        if (autoSaveTimer.isRunning()) {
            autoSaveTimer.restart(); // Перезапуск таймера
        } else {
            autoSaveTimer.start(); // Старт таймера
        }
    }

    private void saveFileAuto() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(stateManager.getLastFile()))) {
            textArea.write(writer);
            System.out.println("Автосохранение выполнено в файл");
        } catch (IOException e) {
            System.err.println("Ошибка автосохранения: " + e.getMessage());
        }
    }

    private void runJavaFile() {
        JTextArea consoleArea = (JTextArea) consoleScrollPane.getViewport().getView();
        try {
            consoleArea.setText("");
            String code = textArea.getText();
            String className = extractorClassName.extractClassName(code);

            if (className != null) {
                File tempFile = new File(className + ".java");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    textArea.write(writer);
                }
                Process compileProcess = Runtime.getRuntime().exec("javac " + tempFile.getAbsolutePath());
                compileProcess.waitFor();

                if (compileProcess.exitValue() == 0) {
                    Process runProcess = Runtime.getRuntime().exec("java " + className);
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                    String line;
                    while ((line = inputReader.readLine()) != null) {
                        consoleArea.append(line + "\n"); // Вывод в консоль
                    }
                    inputReader.close();
                    runProcess.waitFor();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        consoleArea.append(line + "\n"); // Вывод ошибок в консоль
                    }
                    errorReader.close();
                    consoleArea.append("Compilation failed.\n");
                }

            } else {
                consoleArea.append("No public class found.\n");
            }

        } catch (Exception e) {
            consoleArea.append("Error: " + e.getMessage() + "\n");
        }
    }

    public void toggleConsoleWithWindowText() {
        // Получаем родительский JFrame
        if (consoleWindow.isOpen()) {
            // Удаление консоли из родительского контейнера
            mainFrame.remove(consoleScrollPane);
        } else {
            // Добавление консоли обратно в родительский контейнер
            mainFrame.add(consoleScrollPane, BorderLayout.SOUTH);
        }
        repaintMainFrame();
    }


    private void repaintMainFrame() {
        mainFrame.revalidate(); // Перерасчет компоновки
        mainFrame.repaint(); // Обновление интерфейса
    }

    public void saveAppState() {
        stateManager.saveAppState(fontSizeSlider.getValue());
    }

    private void loadAppState() {
        loadStateFont();
        // Восстановить последний открытый файл
        loadStateFile();

    }

    private void loadStateFile() {
        String lastFile = stateManager.getLastFile();
        if (lastFile != null) {
            File file = new File(lastFile);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    textArea.read(reader, null);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void loadStateFont() {
        // Восстановить размер шрифта
        String fontSize = stateManager.getFontSize();
        if (fontSize != null) {
            int size = Integer.parseInt(fontSize);
            fontSizeSlider.setValue(size);
            textArea.setFont(new Font(FAMILY_FONT, Font.PLAIN, size));
        }
    }

}
