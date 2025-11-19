package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    public static void loadScene(String fxmlPath, Scene currentScene, String authToken, String userId, String userEmail) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Pass session data to the controller if it has the method
            Object controller = loader.getController();
            if (controller instanceof ManageReservationsController) {
                ((ManageReservationsController) controller).initializeSessionData(authToken, userId, userEmail);
            } else if (controller instanceof ReservationFormController) {
                ((ReservationFormController) controller).initializeSessionData(authToken, userId, userEmail);
            } else if (controller instanceof DashboardController) {
                ((DashboardController) controller).initializeSessionData(authToken, userId, userEmail);
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load: " + fxmlPath);
        }
    }

    private static void showErrorAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}