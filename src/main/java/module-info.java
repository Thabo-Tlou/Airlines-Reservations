module groupassingment.airlinesreservations {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;


    opens groupassingment.airlinesreservations to javafx.fxml;
    exports groupassingment.airlinesreservations;
    exports groupassingment.airlinesreservations.controllers;
    opens groupassingment.airlinesreservations.controllers to javafx.fxml;
}