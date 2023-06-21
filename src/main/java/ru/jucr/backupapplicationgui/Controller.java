package ru.jucr.backupapplicationgui;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    @FXML
    private TextField sourceField;
    @FXML
    private TextField remoteFolderField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField serverHostField;
    @FXML
    private TextField serverPortField;
    @FXML
    private TextField passField;
    @FXML
    private Button saveButton;
    @FXML
    private Button startButton;
    @FXML
    private TextField intervalField;
    @FXML
    private RadioButton radioBackup;
    @FXML
    private RadioButton radioTracking;
    @FXML
    private TextArea resultsTextArea;
    @FXML
    private ToggleGroup toggleGroup;
    @FXML
    private TabPane tabPane;
    List<ConfigurationManager> configurationManagers = new ArrayList<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private ConfigurationManager configManagementLane;
    private BackupManager.BackupType backupType;
    private int tabSize = 1;

    public Controller() {
        configManagementLane = new ConfigurationManager(0);
        configurationManagers.add(configManagementLane);
        backupType = configManagementLane.getBackupType();
    }

    @FXML
    private void initialize() {
        addConfiguration(configManagementLane);
    }

    @FXML
    void saveConfiguration() {
        ConfigurationManager.saveTabsSize(tabSize);

        int tabNumber = tabPane.getSelectionModel().getSelectedIndex();
        configManagementLane = configurationManagers.get(tabNumber);

        // Получение ссылки на выбранную вкладку
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Проверка, что вкладка выбрана
        if (selectedTab != null) {
            // Поиск элемента по его ID в выбранной вкладке
            String source = ((TextField) selectedTab.getContent().lookup("#sourceField")).getText();
            String remoteFolder = ((TextField) selectedTab.getContent().lookup("#remoteFolderField")).getText();
            String username = ((TextField) selectedTab.getContent().lookup("#usernameField")).getText();
            String serverHost = ((TextField) selectedTab.getContent().lookup("#serverHostField")).getText();
            String serverPort = ((TextField) selectedTab.getContent().lookup("#serverPortField")).getText();
            String password = ((TextField) selectedTab.getContent().lookup("#passField")).getText();
            String interval = ((TextField) selectedTab.getContent().lookup("#intervalField")).getText();

            // Получение ToggleGroup выбранной вкладки
            ToggleGroup toggleGroup = ((RadioButton) selectedTab.getContent().lookup("#radioBackup")).getToggleGroup();
            // Получение выбранного RadioButton
            RadioButton selectedRadioButton = (RadioButton) toggleGroup.getSelectedToggle();
            String selectedValue = selectedRadioButton.getText();

            configManagementLane.setSourceFolder(source);
            configManagementLane.setRemotePath(remoteFolder);
            configManagementLane.setUsername(username);
            configManagementLane.setServerHost(serverHost);
            configManagementLane.setServerPassword(password);
            configManagementLane.setServerPort(Integer.parseInt(serverPort));
            configManagementLane.setBackupInterval(Integer.parseInt(interval));

            if (selectedValue.equals("Backup")) {
                configManagementLane.setBackupType(BackupManager.BackupType.valueOf("BACKUP"));
            } else {
                configManagementLane.setBackupType(BackupManager.BackupType.valueOf("TRACKING"));
            }

            appendResult("Configuration saved");
        }
    }

    @FXML
    void startMonitoring() {
        int tabNumber = tabPane.getSelectionModel().getSelectedIndex();
        configManagementLane = configurationManagers.get(tabNumber);

        // Получение ссылки на выбранную вкладку
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Получение ToggleGroup выбранной вкладки
        toggleGroup = ((RadioButton) selectedTab.getContent().lookup("#radioBackup")).getToggleGroup();
        // Получение выбранного RadioButton
        RadioButton selectedRadioButton = (RadioButton) toggleGroup.getSelectedToggle();
        String selectedValue = selectedRadioButton.getText();

        if (selectedValue.equals("Backup")) {
            backupType =  BackupManager.BackupType.BACKUP;
        } else {
            backupType = BackupManager.BackupType.TRACKING;
        }

        BackupManager backupTask = new BackupManager(backupType, configManagementLane, Controller.this);
        executorService.submit(backupTask);
    }

    @FXML
    void addTab() {
        // Создание новой вкладки
        Tab newTab = new Tab();
        newTab.setText("Tab " + (tabPane.getTabs().size()));
        tabSize++;

        newTab.setOnClosed(event -> deleteConfiguration());

        // Создание нового контента для новой вкладки
        AnchorPane newContent = new AnchorPane();

        // Создание нового текстового поля для Source Folder
        Label sourceLabel = new Label("Source Folder:");
        sourceField = new TextField();
        sourceField.setId("sourceField");
        sourceLabel.setLayoutX(20);
        sourceLabel.setLayoutY(23);
        sourceField.setLayoutX(137);
        sourceField.setLayoutY(20);
        sourceField.setPrefWidth(243);

        // Создание нового текстового поля для Remote Folder
        Label remoteFolderLabel = new Label("Remote Folder:");
        remoteFolderField = new TextField();
        remoteFolderField.setId("remoteFolderField");
        remoteFolderLabel.setLayoutX(20);
        remoteFolderLabel.setLayoutY(60);
        remoteFolderField.setLayoutX(137);
        remoteFolderField.setLayoutY(57);
        remoteFolderField.setPrefWidth(243);

        // Создание нового текстового поля для Username
        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();
        usernameField.setId("usernameField");
        usernameLabel.setLayoutX(20);
        usernameLabel.setLayoutY(97);
        usernameField.setLayoutX(137);
        usernameField.setLayoutY(94);
        usernameField.setPrefWidth(243);

        // Создание нового текстового поля для Server Host
        Label serverHostLabel = new Label("Server Host:");
        serverHostField = new TextField();
        serverHostField.setId("serverHostField");
        serverHostLabel.setLayoutX(20);
        serverHostLabel.setLayoutY(134);
        serverHostField.setLayoutX(137);
        serverHostField.setLayoutY(131);
        serverHostField.setPrefWidth(243);

        // Создание нового текстового поля для Server Port
        Label serverPortLabel = new Label("Server Port:");
        serverPortField = new TextField();
        serverPortField.setId("serverPortField");
        serverPortLabel.setLayoutX(20);
        serverPortLabel.setLayoutY(171);
        serverPortField.setLayoutX(137);
        serverPortField.setLayoutY(168);
        serverPortField.setPrefWidth(243);

        // Создание нового текстового поля для Password
        Label passLabel = new Label("Password:");
        passField = new TextField();
        passField.setId("passField");
        passLabel.setLayoutX(20);
        passLabel.setLayoutY(208);
        passField.setLayoutX(137);
        passField.setLayoutY(205);
        passField.setPrefWidth(243);

        // Создание нового текстового поля для Interval
        Label intervalLabel = new Label("Interval:");
        intervalField = new TextField();
        intervalField.setId("intervalField");
        intervalLabel.setLayoutX(20);
        intervalLabel.setLayoutY(245);
        intervalField.setLayoutX(137);
        intervalField.setLayoutY(242);
        intervalField.setPrefWidth(243);

        // Создание новой кнопки Save Configuration
        Button saveButton = new Button("Save Configuration");
        saveButton.setId("saveButton");
        saveButton.setLayoutX(137);
        saveButton.setLayoutY(288);
        saveButton.setOnAction(event -> saveConfiguration());
        saveButton.setMnemonicParsing(false);

        // Создание новой кнопки Start Backup
        Button startButton = new Button("Start Backup");
        startButton.setId("startButton");
        startButton.setLayoutX(270);
        startButton.setLayoutY(288);
        startButton.setOnAction(event -> startMonitoring());
        startButton.setMnemonicParsing(false);

        // Создание новой метки Results
        Label resultsLabel = new Label("Results:");
        resultsLabel.setLayoutX(20);
        resultsLabel.setLayoutY(328);

        // Создание новой текстовой области для Results
        resultsTextArea = new TextArea();
        resultsTextArea.setId("resultsTextArea");
        resultsTextArea.setLayoutX(20);
        resultsTextArea.setLayoutY(358);
        resultsTextArea.setPrefHeight(119);
        resultsTextArea.setPrefWidth(360);
        resultsTextArea.setWrapText(true);

        // Создание нового переключателя Backup
        radioBackup = new RadioButton("Backup");
        radioBackup.setId("radioBackup");
        radioBackup.setLayoutX(20);
        radioBackup.setLayoutY(270);

        // Создание нового переключателя Tracking
        radioTracking = new RadioButton("Tracking");
        radioTracking.setId("radioTracking");
        radioTracking.setLayoutX(20);
        radioTracking.setLayoutY(290);

        // Создание новой группы переключателей
        toggleGroup = new ToggleGroup();

        // Применение группы переключателей к переключателям
        radioBackup.setToggleGroup(toggleGroup);
        radioTracking.setToggleGroup(toggleGroup);

        // Установка начального выбранного переключателя
        radioBackup.setSelected(true);

        // Добавление всех элементов в новый контент
        newContent.getChildren().addAll(sourceLabel, sourceField, remoteFolderLabel, remoteFolderField, usernameLabel, usernameField,
                serverHostLabel, serverHostField, serverPortLabel, serverPortField, passLabel, passField, intervalLabel, intervalField,
                saveButton, startButton, resultsLabel, resultsTextArea, radioBackup, radioTracking);

        // Установка контента для новой вкладки
        newTab.setContent(newContent);

        // Добавление новой вкладки в TabPane
        tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);

        // Переключение на новую вкладку
        tabPane.getSelectionModel().select(newTab);

        ConfigurationManager configManager = new ConfigurationManager(tabPane.getSelectionModel().getSelectedIndex());
        configurationManagers.add(configManager);

        addConfiguration(configManager);
    }

    public void appendResult(String result) {
        // Получение ссылки на выбранную вкладку
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Поиск resultsTextArea внутри выбранной вкладки
        TextArea resultsTextArea = findResultsTextArea(selectedTab);

        if (resultsTextArea != null) {
            // Добавить текст в resultsTextArea
            resultsTextArea.appendText(result + "\n");
        }
    }

    // Метод для поиска resultsTextArea внутри вкладки
    private TextArea findResultsTextArea(Tab tab) {
        Node contentNode = tab.getContent();

        if (contentNode instanceof AnchorPane) {
            AnchorPane anchorPane = (AnchorPane) contentNode;
            ObservableList<Node> children = anchorPane.getChildren();

            for (Node child : children) {
                if (child instanceof TextArea && child.getId().equals("resultsTextArea")) {
                    // Найден resultsTextArea
                    return (TextArea) child;
                }
            }
        }

        return null; // resultsTextArea не найден
    }

    public void addConfiguration(ConfigurationManager configManagementLane) {
        sourceField.setText(configManagementLane.getSourceFolder());
        remoteFolderField.setText(configManagementLane.getRemotePath());
        usernameField.setText(configManagementLane.getUsername());
        serverHostField.setText(configManagementLane.getServerHost());
        serverPortField.setText(String.valueOf(configManagementLane.getServerPort()));
        passField.setText(configManagementLane.getServerPassword());
        intervalField.setText(String.valueOf(configManagementLane.getBackupInterval()));

        backupType = configManagementLane.getBackupType();

        // Set initial RadioButton selection
        if(backupType.equals(BackupManager.BackupType.BACKUP)) {
            radioBackup.setSelected(true);
        } else {
            radioTracking.setSelected(true);
        }
    }

    @FXML
    EventHandler<Event> deleteConfiguration() {
        int tabNumber = tabPane.getSelectionModel().getSelectedIndex();

        if (tabNumber != 0) {
            tabNumber++;
        }

        ConfigurationManager configManager = configurationManagers.get(tabNumber);
        String configFileName = configManager.getConfigFileName();
        File configFile = new File(configFileName);

        if (configFile.exists()) {
            configFile.delete();
            configurationManagers.remove(configManager);
            tabSize--;

            // Обновление названий оставшихся файлов
            updateConfigFileNames();
        }
        return null;
    }

    private void updateConfigFileNames() {
        for (int i = 0; i < configurationManagers.size(); i++) {
            ConfigurationManager configManager = configurationManagers.get(i);
            String oldFileName = configManager.getConfigFileName();
            String newFileName = ConfigurationManager.CONFIG_FILE_PREFIX + (i + 1) + ConfigurationManager.CONFIG_FILE_EXTENSION;

            File oldFile = new File(oldFileName);
            File newFile = new File(newFileName);

            if (oldFile.exists()) {
                oldFile.renameTo(newFile);
            }

            configManager.setConfigFileName(newFileName);
        }
        // Удаление неиспользуемых фалов
        deleteUnusedConfigFiles();
    }

    private void deleteUnusedConfigFiles() {
        File configDir = new File("C:\\Users\\Honor\\Desktop\\BackupApplicationGui\\"); // Укажите путь к каталогу, где сохранены файлы конфигурации
        File[] configFiles = configDir.listFiles();

        if (configFiles != null) {
            List<String> usedFileNames = new ArrayList<>();

            for (int i = 0; i < tabSize; i++) {
                String fileName = ConfigurationManager.CONFIG_FILE_PREFIX + (i + 1) + ConfigurationManager.CONFIG_FILE_EXTENSION;
                usedFileNames.add(fileName);
            }

            for (File configFile : configFiles) {
                String fileName = configFile.getName();

                if (fileName.startsWith(ConfigurationManager.CONFIG_FILE_PREFIX) && fileName.endsWith(ConfigurationManager.CONFIG_FILE_EXTENSION)) {
                    if (!usedFileNames.contains(fileName)) {
                        configFile.delete();
                    }
                }
            }
        }
    }
}