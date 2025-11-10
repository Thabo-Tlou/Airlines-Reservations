module groupassingment.airlinesreservations {
    requires javafx.controls;
    requires javafx.fxml;


    opens groupassingment.airlinesreservations to javafx.fxml;
    exports groupassingment.airlinesreservations;
}