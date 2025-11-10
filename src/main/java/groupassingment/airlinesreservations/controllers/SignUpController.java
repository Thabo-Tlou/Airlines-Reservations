package groupassingment.airlinesreservations.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.IOException;

public class SignUpController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private StackPane spinnerContainer;
    @FXML private Label loadingText;
    @FXML private VBox signupContainerRight;
    @FXML private Arc progressArc;
    @FXML private ImageView planeIcon;
    @FXML private Circle trackCircle;
    @FXML private Button signUpButton;

    private final SupabaseService supabase = new SupabaseService();
    private Timeline planeAnimation;
    private Timeline progressAnimation;

    @FXML
    public void initialize() {
        setupCircularAnimation();
    }

    private void setupCircularAnimation() {
        if (planeIcon == null || progressArc == null) {
            System.err.println("WARNING: Animation elements missing; skipping setup.");
            return;
        }

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

        progressAnimation = new Timeline();
        progressAnimation.setCycleCount(Timeline.INDEFINITE);
        KeyFrame start = new KeyFrame(Duration.ZERO, new KeyValue(progressArc.lengthProperty(), 0));
        KeyFrame end = new KeyFrame(Duration.seconds(2), new KeyValue(progressArc.lengthProperty(), 360));
        progressAnimation.getKeyFrames().addAll(start, end);
    }

    private void showLoading(String message) {
        Platform.runLater(() -> {
            if (loadingText != null) loadingText.setText(message);
            if (spinnerContainer != null) spinnerContainer.setVisible(true);
            if (signupContainerRight != null && !signupContainerRight.getStyleClass().contains("form-loading"))
                signupContainerRight.getStyleClass().add("form-loading");

            if (planeAnimation != null) planeAnimation.play();
            if (progressAnimation != null) progressAnimation.play();
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            if (spinnerContainer != null) spinnerContainer.setVisible(false);
            if (signupContainerRight != null) signupContainerRight.getStyleClass().remove("form-loading");

            if (planeAnimation != null) planeAnimation.stop();
            if (progressAnimation != null) progressAnimation.stop();
        });
    }

    @FXML
    private void handleSignUp() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all required fields.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Passwords do not match!");
            return;
        }

        showLoading("Creating your account...");
        System.out.println("LOG: Signing up " + email);

        supabase.signUp(email, password).thenAccept(response -> {
            int status = response.statusCode();
            String body = response.body();

            if (status == 200 || status == 201) {
                // Insert passenger info
                supabase.insertPassenger(firstName, lastName, email, phone)
                        .thenAccept(insertResponse -> {
                            Platform.runLater(() -> {
                                hideLoading();
                                if (insertResponse.statusCode() == 201 || insertResponse.statusCode() == 204) {
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
                                    navigateToDashboard();
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Error", "Account created but failed to save info.");
                                }
                            });
                        }).exceptionally(ex -> {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                hideLoading();
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save passenger info: " + ex.getMessage());
                            });
                            return null;
                        });
            } else {
                try {
                    JSONObject errorJson = new JSONObject(body);
                    String errorMsg = errorJson.optString("msg", body);
                    Platform.runLater(() -> {
                        hideLoading();
                        showAlert(Alert.AlertType.ERROR, "Signup Failed", errorMsg);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        hideLoading();
                        showAlert(Alert.AlertType.ERROR, "Signup Failed", body);
                    });
                }
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> {
                hideLoading();
                showAlert(Alert.AlertType.ERROR, "Network Error", ex.getMessage());
            });
            return null;
        });
    }

    @FXML
    private void handleBackToLogin() {
        showLoading("Returning to login...");
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    hideLoading();
                    navigateToLogin();
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    hideLoading();
                    navigateToLogin();
                });
            }
        }).start();
    }

    private void navigateToLogin() {
        try {
            Stage stage = (Stage) signUpButton.getScene().getWindow();
            Parent loginRoot = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Login.fxml")).load();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Bokamoso Airlines - Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to load login screen.");
        }
    }

    private void navigateToDashboard() {
        try {
            Stage stage = (Stage) signUpButton.getScene().getWindow();
            Parent dashboardRoot = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Dashboard.fxml")).load();
            stage.setScene(new Scene(dashboardRoot));
            stage.setTitle("Bokamoso Airlines - Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to load dashboard.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
