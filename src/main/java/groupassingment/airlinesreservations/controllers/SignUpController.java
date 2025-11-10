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
        // Animation for the plane moving around the circle
        planeAnimation = new Timeline();
        planeAnimation.setCycleCount(Timeline.INDEFINITE);

        // Create keyframes for circular motion
        for (int i = 0; i <= 360; i += 15) {
            double angle = Math.toRadians(i);
            double radius = 30.0;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);

            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 20),
                    new KeyValue(planeIcon.translateXProperty(), x),
                    new KeyValue(planeIcon.translateYProperty(), y),
                    new KeyValue(planeIcon.rotateProperty(), i + 90) // Point plane in direction of travel
            );
            planeAnimation.getKeyFrames().add(keyFrame);
        }

        // Animation for the progress arc
        progressAnimation = new Timeline();
        progressAnimation.setCycleCount(Timeline.INDEFINITE);

        KeyFrame progressStart = new KeyFrame(Duration.ZERO,
                new KeyValue(progressArc.lengthProperty(), 0)
        );
        KeyFrame progressEnd = new KeyFrame(Duration.seconds(2),
                new KeyValue(progressArc.lengthProperty(), 360)
        );
        progressAnimation.getKeyFrames().addAll(progressStart, progressEnd);
    }

    private void showLoading(String message) {
        Platform.runLater(() -> {
            loadingText.setText(message);
            spinnerContainer.setVisible(true);
            signupContainerRight.getStyleClass().add("form-loading");

            // Reset and start animations
            progressArc.setLength(0);
            planeIcon.setTranslateX(0);
            planeIcon.setTranslateY(-30); // Start at top of circle
            planeIcon.setRotate(0);

            planeAnimation.play();
            progressAnimation.play();
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            spinnerContainer.setVisible(false);
            signupContainerRight.getStyleClass().remove("form-loading");

            // Stop animations
            if (planeAnimation != null) {
                planeAnimation.stop();
            }
            if (progressAnimation != null) {
                progressAnimation.stop();
            }
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

        // 1. Basic validation
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all required fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Passwords do not match!");
            return;
        }

        showLoading("Creating your account...");

        System.out.println("LOG: Starting Supabase signUp for email: " + email);

        // 2. Create user account on Supabase Auth
        supabase.signUp(email, password).thenAccept(response -> {
            int status = response.statusCode();
            String body = response.body();

            System.out.println("LOG: Supabase Auth Response Status: " + status);

            if (status == 200 || status == 201) {
                System.out.println("LOG: Auth successful. Proceeding to passenger insert.");
                Platform.runLater(() -> loadingText.setText("Setting up your profile..."));

                // 3. Auth successful, insert passenger info into your Supabase table
                supabase.insertPassenger(firstName, lastName, email, phone)
                        .thenAccept(insertResponse -> {
                            System.out.println("LOG: Passenger Insert Status: " + insertResponse.statusCode());
                            if (insertResponse.statusCode() == 201 || insertResponse.statusCode() == 204) {
                                Platform.runLater(() -> {
                                    hideLoading();
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully! Welcome aboard.");
                                    // Optionally navigate to login after successful signup
                                    // navigateToLogin();
                                });
                            } else {
                                System.err.println("ERROR: Insert failed body: " + insertResponse.body());
                                Platform.runLater(() -> {
                                    hideLoading();
                                    showAlert(Alert.AlertType.ERROR, "Insert Failed", "Account created but data insert failed: " + insertResponse.body());
                                });
                            }
                        })
                        .exceptionally(ex -> {
                            System.err.println("ERROR: Exception during passenger insert attempt:");
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                hideLoading();
                                showAlert(Alert.AlertType.ERROR, "Error", "Error saving passenger info: " + ex.getMessage());
                            });
                            return null;
                        });

            } else {
                // 4. Handle Supabase Auth error
                try {
                    JSONObject errorJson = new JSONObject(body);
                    String errorMsg = errorJson.optString("msg", body);
                    System.err.println("ERROR: Supabase Auth failed (Status: " + status + "): " + errorMsg);
                    Platform.runLater(() -> {
                        hideLoading();
                        showAlert(Alert.AlertType.ERROR, "Signup Failed", errorMsg);
                    });
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to parse Supabase Auth error body.");
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        hideLoading();
                        showAlert(Alert.AlertType.ERROR, "Signup Failed", body);
                    });
                }
            }
        }).exceptionally(ex -> {
            System.err.println("FATAL ERROR: Network connection failed during signUp attempt.");
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

        // Use a thread to simulate loading and then navigate
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Short delay for smooth UX
                Platform.runLater(() -> {
                    hideLoading();
                    navigateToLogin();
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    hideLoading();
                    navigateToLogin(); // Navigate even if interrupted
                });
            }
        }).start();
    }

    private void navigateToLogin() {
        try {
            // Get the current stage
            Stage currentStage = (Stage) signUpButton.getScene().getWindow();

            // Load the login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/LogIn.fxml"));
            Parent loginRoot = loader.load();

            // Create new scene
            Scene loginScene = new Scene(loginRoot);

            // Set the scene on current stage
            currentStage.setScene(loginScene);
            currentStage.setTitle("Bokamoso Airlines - Login");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("ERROR: Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to load login screen. Please try again.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}