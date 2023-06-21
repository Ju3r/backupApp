package ru.jucr.backupapplicationgui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class BackupApplication extends Application {
    private ConfigurationManager configManagementLane;
    private Controller controller;
    private BackupManager.BackupType backupType;

    public BackupApplication() {
        configManagementLane = new ConfigurationManager(0);
        backupType = configManagementLane.getBackupType();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the XML file
            FXMLLoader loader = new FXMLLoader(BackupApplication.class.getResource("backup-app.fxml"));
            Parent root = loader.load();

            // Get reference to the controller
            controller = loader.getController();

            // Add tabs based on the saved count
            for (int i = 0; i < (ConfigurationManager.tabsSize - 1); i++) {
                controller.addTab();
            }

            // Create the main scene
            Scene scene = new Scene(root, 400, 500);

            // Set up the main stage
            primaryStage.setTitle("Backup Application");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        controller.saveConfiguration();
    }
}