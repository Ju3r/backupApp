module ru.jucr.backupapplicationgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires jsch;


    opens ru.jucr.backupapplicationgui to javafx.fxml;
    exports ru.jucr.backupapplicationgui;
}