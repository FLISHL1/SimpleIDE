package ru.liga.views;

import com.github.javaparser.JavaParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import ru.liga.adapter.MainWindowAdapter;
import ru.liga.autoCompletion.AutoCompletionJava;
import ru.liga.autoCompletion.ExtractorDescription;
import ru.liga.autoCompletion.ExtractorJavaClass;
import ru.liga.autoCompletion.JavaClassChecker;
import ru.liga.autoCompletion.competitonProvider.CustomCompetitionProvider;
import ru.liga.utils.AutoSaverLastOpenFileUtil;
import ru.liga.utils.ExtractorPublicClassName;
import ru.liga.utils.RunnerJavaFile;
import ru.liga.utils.StateManager;
import ru.liga.utils.fileFilter.JavaFileFilter;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class MainWindow {
    private final int HEIGHT = 600;
    private final int WIDTH = 800;
    private final String FAMILY_FONT = "Monospaced";
    private final String title = "Simple Java IDE";
    private final RSyntaxTextArea textArea;
    private final JSlider fontSizeSlider;
    private final AutoCompletionJava autoCompletionJava;
    private final JScrollPane consoleScrollPane;
    private final JFrame mainFrame;
    private final ExtractorPublicClassName extractorClassName;
    private final StateManager stateManager;
    private final ConsoleWindow consoleWindow;
    private final Font mainFont;

    public MainWindow(StateManager stateManager, ExtractorPublicClassName extractorClassName, AutoSaverLastOpenFileUtil autoSaverLastOpenFileUtil) {
        this.stateManager = stateManager;
        this.extractorClassName = extractorClassName;
        this.mainFont = new Font(FAMILY_FONT, Font.PLAIN, 14);

        mainFrame = new JFrame(title);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(WIDTH, HEIGHT);
        mainFrame.setLayout(new BorderLayout());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        // Верхняя панель с кнопками
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setFont(mainFont);
        JButton newButton = new JButton("New");
        JButton openButton = new JButton("Open");
        JButton saveButton = new JButton("Save");
        JButton runButton = new JButton("Run");
        JButton consoleButton = new JButton("Show Console");
        addComponentToPanel(topPanel, newButton, openButton, saveButton, runButton, consoleButton);
        mainFrame.add(topPanel, BorderLayout.NORTH);
        textArea = createTextArea();

        JavaClassChecker classChecker = new JavaClassChecker();
        autoCompletionJava = new AutoCompletionJava(new JavaParser(), new CustomCompetitionProvider(), textArea,
                classChecker, new ExtractorJavaClass(classChecker), new ExtractorDescription());
        autoCompletionJava.start();
        RTextScrollPane sp = new RTextScrollPane(textArea);
        mainFrame.add(sp, BorderLayout.CENTER);

        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setMargin(new Insets(10, 10, 10, 10));
        consoleArea.setBackground(Color.decode("#434343"));
        consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setPreferredSize(new Dimension(WIDTH, 150));
        consoleScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainFrame.add(consoleScrollPane, BorderLayout.SOUTH);
        consoleWindow = new ConsoleWindow(this);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JLabel fontSizeLabel = new JLabel("Font Size:");
        fontSizeSlider = new JSlider(10, 30, 12);
        fontSizeSlider.setMajorTickSpacing(5);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);
        addComponentToPanel(rightPanel, fontSizeLabel, fontSizeSlider);
        mainFrame.add(rightPanel, BorderLayout.EAST);

        newButton.addActionListener(e -> clearTextArea());
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveFile());
        RunnerJavaFile runnerJavaFile = new RunnerJavaFile(consoleScrollPane, extractorClassName, textArea);
        runButton.addActionListener(e -> runnerJavaFile.runWithSingleThread());
        consoleButton.addActionListener(e -> {
            consoleWindow.show(consoleScrollPane);
        });

        fontSizeSlider.addChangeListener(e -> {
            textArea.setFont(mainFont.deriveFont((float) fontSizeSlider.getValue()));
            consoleArea.setFont(mainFont.deriveFont((float) fontSizeSlider.getValue()));
        });
        loadAppState();
        mainFrame.addWindowListener(new MainWindowAdapter(this));
        autoSaverLastOpenFileUtil.setupAutoSaveFeature(textArea);
        mainFrame.setVisible(true);
    }

    private void clearTextArea() {
        textArea.setText("");
        stateManager.saveLastOpenedFile("");
    }

    private void addComponentToPanel(JPanel panel, JComponent... buttons) {
        for (JComponent button : buttons) {
            applyFontComponent(button);
            panel.add(button);
        }
    }

    private void applyFontComponent(JComponent... components) {
        for (JComponent component : components) {
            applyFontComponent(component);
        }
    }

    private void applyFontComponent(JComponent component) {
        component.setFont(mainFont);
    }

    private RSyntaxTextArea createTextArea() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        return textArea;
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new JavaFileFilter().getFileNameExtensionFilterForJavaFile());
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                textArea.read(reader, null);
                stateManager.saveLastOpenedFile(file.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new JavaFileFilter().getFileNameExtensionFilterForJavaFile());
        fileChooser.setSelectedFile(new File(extractorClassName.extractClassName(textArea.getText()) + ".java"));
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



    public void toggleConsoleWithWindowText() {
        if (consoleWindow.isOpen()) {
            mainFrame.remove(consoleScrollPane);
        } else {
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
        String fontSize = stateManager.getFontSize();
        if (fontSize != null) {
            int size = Integer.parseInt(fontSize);
            fontSizeSlider.setValue(size);
            textArea.setFont(new Font(FAMILY_FONT, Font.PLAIN, size));
        }
    }

}
