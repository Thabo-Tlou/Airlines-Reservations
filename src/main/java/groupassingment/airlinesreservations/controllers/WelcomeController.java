package groupassingment.airlinesreservations.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    @FXML private Button loginButton;
    @FXML private Button signupButton;
    @FXML private Circle floatingOrb1;
    @FXML private Circle floatingOrb2;
    @FXML private Circle floatingOrb3;
    @FXML private StackPane rootPane;

    @FXML private ProgressIndicator spinner;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupAnimations();
        setupSpinner();
    }

    private void setupAnimations() {
        animateOrb(floatingOrb1, 0, -30, 8);
        animateOrb(floatingOrb2, 0, 25, 6);
        animateOrb(floatingOrb3, 0, -20, 10);
    }

    private void animateOrb(Circle orb, double fromY, double toY, double durationSeconds) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(orb.translateYProperty(), fromY)),
                new KeyFrame(Duration.seconds(durationSeconds / 2), new KeyValue(orb.translateYProperty(), toY)),
                new KeyFrame(Duration.seconds(durationSeconds), new KeyValue(orb.translateYProperty(), fromY))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        timeline.play();
    }

    private void setupSpinner() {
        spinner.setVisible(false);
        spinner.setMaxSize(100, 100);
        spinner.setStyle("-fx-progress-color: #F2B418;");

        // Center spinner
        spinner.setLayoutX(rootPane.getPrefWidth() / 2 - 50);
        spinner.setLayoutY(rootPane.getPrefHeight() / 2 - 50);
    }

    @FXML
    private void handleLogin() {
        animateButtonAndNavigate(loginButton, "/groupassingment/airlinesreservations/LogIn.fxml", "Bokamoso Airlines - Login");
    }

    @FXML
    private void handleSignUp() {
        animateButtonAndNavigate(signupButton, "/groupassingment/airlinesreservations/SignUp.fxml", "Bokamoso Airlines - Sign Up");
    }

    private void animateButtonAndNavigate(Button button, String fxmlPath, String title) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
        scale.setToX(0.95);
        scale.setToY(0.95);
        scale.setOnFinished(e -> {
            showSpinner();
            PauseTransition delay = new PauseTransition(Duration.millis(100));
            delay.setOnFinished(ev -> navigateToScreen(fxmlPath, title));
            delay.play();
        });
        scale.play();
    }

    private void showSpinner() {
        spinner.setVisible(true);
        spinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        spinner.setLayoutX(rootPane.getPrefWidth() / 2 - 50);
        spinner.setLayoutY(rootPane.getPrefHeight() / 2 - 50);
    }

    private void hideSpinner() {
        spinner.setVisible(false);
    }

    private void navigateToScreen(String fxmlPath, String title) {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle(title);
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to load the requested screen. Please try again.");
            alert.showAndWait();
        } finally {
            hideSpinner();
        }
    }
}
