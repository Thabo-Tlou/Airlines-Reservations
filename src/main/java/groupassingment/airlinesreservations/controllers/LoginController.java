package groupassingment.airlinesreservations.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.json.JSONObject;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button signInButton;
    @FXML private Button createAccountButton;
    @FXML private StackPane spinnerContainer;
    @FXML private Label loadingText;
    @FXML private VBox loginContainerRight;
    @FXML private Arc progressArc;
    @FXML private ImageView planeIcon;
    @FXML private Circle trackCircle;

    private final SupabaseService supabase = new SupabaseService();
    private Timeline planeAnimation;
    private Timeline progressAnimation;

    @FXML
    public void initialize() {
        setupCircularAnimation();
    }

    private void setupCircularAnimation() {
        if (planeIcon == null || progressArc == null) return;

        planeAnimation = new Timeline();
        planeAnimation.setCycleCount(Timeline.INDEFINITE);
        for (int i = 0; i <= 360; i += 15) {
            double angle = Math.toRadians(i);
            double radius = 30.0;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 20),
                    new KeyValue(planeIcon.translateXProperty(), x),
                    new KeyValue(planeIcon.translateYProperty(), y),
                    new KeyValue(planeIcon.rotateProperty(), i + 90)
            );
            planeAnimation.getKeyFrames().add(keyFrame);
        }

        progressAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressArc.lengthProperty(), 0)),
                new KeyFrame(Duration.seconds(2), new KeyValue(progressArc.lengthProperty(), 360))
        );
        progressAnimation.setCycleCount(Timeline.INDEFINITE);
    }

    private void showLoading(String message) {
        Platform.runLater(() -> {
            if (loadingText != null) loadingText.setText(message);
            if (spinnerContainer != null) spinnerContainer.setVisible(true);
            if (loginContainerRight != null) loginContainerRight.getStyleClass().add("form-loading");
            if (progressArc != null && planeIcon != null) {
                progressArc.setLength(0);
                planeIcon.setTranslateX(0);
                planeIcon.setTranslateY(-30);
                planeIcon.setRotate(0);
                planeAnimation.play();
                progressAnimation.play();
            }
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            if (spinnerContainer != null) spinnerContainer.setVisible(false);
            if (loginContainerRight != null) loginContainerRight.getStyleClass().remove("form-loading");
            if (planeAnimation != null) planeAnimation.stop();
            if (progressAnimation != null) progressAnimation.stop();
        });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both email and password.");
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Email and password cannot be empty.");
            return;
        }

        showLoading("Signing you in...");
        statusLabel.setText("Logging in...");

        supabase.login(email, password).thenAccept(response -> {
            int status = response.statusCode();
            String body = response.body();

            if (status == 200) {
                Platform.runLater(() -> {
                    hideLoading();
                    statusLabel.setText("Login successful! Redirecting...");
                    navigateToDashboard();
                });
            } else {
                Platform.runLater(() -> {
                    hideLoading();
                    statusLabel.setText("Login failed. Please check your credentials.");
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
                });
            }

        }).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> {
                hideLoading();
                statusLabel.setText("Network Error: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Network Error", ex.getMessage());
            });
            return null;
        });
    }

    private void navigateToDashboard() {
        try {
            Stage stage = (Stage) signInButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Bokamoso Airlines - Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to load dashboard.");
        }
    }

    @FXML
    private void handleCreateAccount() {
        try {
            Stage stage = (Stage) signInButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/SignUp.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Bokamoso Airlines - Sign Up");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to load signup screen.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
