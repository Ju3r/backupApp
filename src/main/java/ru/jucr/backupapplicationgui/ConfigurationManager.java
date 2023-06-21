package ru.jucr.backupapplicationgui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManager {
    static int tabsSize;
    static final String CONFIG_FILE_PREFIX = "config_task_";
    static final String CONFIG_FILE_EXTENSION = ".properties";
    private static final String TABS_SIZE_FILE = "tabs_size.properties";
    private Properties properties;
    private int tabNumber;
    private String configFileName = getConfigFileName();

    public ConfigurationManager(int tabNumber) {
        this.tabNumber = tabNumber + 1;
        properties = new Properties();
        loadTabsSize();
        loadConfig();
    }

    private void loadTabsSize() {
        File tabsSizeFile = new File(TABS_SIZE_FILE);
        if (tabsSizeFile.exists()) {
            try (FileInputStream input = new FileInputStream(tabsSizeFile)) {
                Properties tabsSizeProperties = new Properties();
                tabsSizeProperties.load(input);
                String tabsSizeValue = tabsSizeProperties.getProperty("tabsSize");
                if (tabsSizeValue != null) {
                    tabsSize = Integer.parseInt(tabsSizeValue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Значение по умолчанию, если файл не существует
            tabsSize = 1;
            saveTabsSize(tabsSize);
        }
    }

    static void saveTabsSize(int tabsSize) {
        Properties tabsSizeProperties = new Properties();
        tabsSizeProperties.setProperty("tabsSize", String.valueOf(tabsSize));
        try (FileOutputStream output = new FileOutputStream(TABS_SIZE_FILE)) {
            tabsSizeProperties.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSourceFolder() {
        return properties.getProperty("sourceFolder", "");
    }

    public void setSourceFolder(String sourceFolder) {
        properties.setProperty("sourceFolder", sourceFolder);
        saveConfig();
    }

    public String getRemotePath() {
        return properties.getProperty("remotePath", "");
    }

    public void setRemotePath(String remotePath) {
        properties.setProperty("remotePath", remotePath);
        saveConfig();
    }

    public String getUsername() {
        return properties.getProperty("username", "");
    }

    public void setUsername(String username) {
        properties.setProperty("username", username);
        saveConfig();
    }

    public String getServerHost() {
        return properties.getProperty("serverHost", "");
    }

    public void setServerHost(String serverHost) {
        properties.setProperty("serverHost", serverHost);
        saveConfig();
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("serverPort", "0"));
    }

    public void setServerPort(int serverPort) {
        properties.setProperty("serverPort", String.valueOf(serverPort));
        saveConfig();
    }

    public String getServerPassword() {
        return properties.getProperty("serverPassword", "");
    }

    public void setServerPassword(String serverPassword) {
        properties.setProperty("serverPassword", serverPassword);
        saveConfig();
    }

    public int getTabNumber() {
        return tabNumber;
    }

    public BackupManager.BackupType getBackupType() {
        return BackupManager.BackupType.valueOf(properties.getProperty("backupType", "BACKUP"));
    }

    public void setBackupType(BackupManager.BackupType backupType) {
        properties.setProperty("backupType", String.valueOf(backupType));
        saveConfig();
    }

    public int getBackupInterval() {
        return Integer.parseInt(properties.getProperty("backupInterval", "0"));
    }

    public void setBackupInterval(int backupInterval) {
        properties.setProperty("backupInterval", String.valueOf(backupInterval));
        saveConfig();
    }

    private void loadConfig() {
        String configFileName = getConfigFileName();
        File configFile = new File(configFileName);

        if (configFile.exists()) {
            try (FileInputStream input = new FileInputStream(configFile)) {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Заполнение дефолтных значений, если файл конфигурации не существует
            setDefaultValues();
            saveConfig();
        }
    }

    private void setDefaultValues() {
        properties.setProperty("sourceFolder", "");
        properties.setProperty("remotePath", "");
        properties.setProperty("username", "");
        properties.setProperty("serverHost", "");
        properties.setProperty("serverPort", "22");
        properties.setProperty("serverPassword", "");
        properties.setProperty("backupInterval", "0");
        properties.setProperty("backupType", BackupManager.BackupType.BACKUP.toString());
    }

    private void saveConfig() {
        try (FileOutputStream output = new FileOutputStream(getConfigFileName())) {
            properties.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }
    String getConfigFileName() {
        return CONFIG_FILE_PREFIX + tabNumber + CONFIG_FILE_EXTENSION;
    }
}