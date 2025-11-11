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
import java.util.Objects;

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
                // Step 1: Account created. Now, immediately log in to get the JWT.
                System.out.println("LOG: Signup successful. Logging in to get JWT...");
                showLoading("Securing your session...");

                // CRITICAL FIX: CHAIN LOGIN TO GET TOKEN
                supabase.login(email, password)
                        .thenAccept(loginResponse -> {
                            if (loginResponse.statusCode() == 200) {
                                try {
                                    JSONObject authResponse = new JSONObject(loginResponse.body());
                                    JSONObject user = authResponse.getJSONObject("user");

                                    String userId = user.getString("id"); // ⬅️ 1. Retrieve the userId
                                    String userToken = authResponse.getString("access_token");

                                    // Step 2: Use the JWT and userId to insert passenger info securely.
                                    showLoading("Saving profile data...");

                                    // ⬅️ 2. Pass all 6 arguments, including userId
                                    supabase.insertPassenger(firstName, lastName, email, phone, userId, userToken)
                                            .thenAccept(insertResponse -> {
                                                Platform.runLater(() -> {
                                                    hideLoading();
                                                    if (insertResponse.statusCode() == 201 || insertResponse.statusCode() == 204) {
                                                        showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully! Welcome.");
                                                        // Step 3: Navigate to dashboard with the token.
                                                        navigateToDashboard(email, userId, userToken);
                                                    } else {
                                                        // Failed to insert passenger info (e.g., 403 RLS error)
                                                        showAlert(Alert.AlertType.ERROR, "Error", "Account created but failed to save profile info securely. Status: " + insertResponse.statusCode());
                                                        // Still navigate, as the user is authenticated
                                                        navigateToDashboard(email, userId, userToken);
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

                                } catch (Exception e) {
                                    System.err.println("LOG: Failed to parse login response after signup: " + e.getMessage());
                                    Platform.runLater(() -> {
                                        hideLoading();
                                        showAlert(Alert.AlertType.ERROR, "Signup Failed", "Session error. Please log in manually.");
                                        navigateToLogin();
                                    });
                                }
                            } else {
                                System.err.println("LOG: Login failed after signup. Status: " + loginResponse.statusCode());
                                Platform.runLater(() -> {
                                    hideLoading();
                                    showAlert(Alert.AlertType.WARNING, "Success", "Account created. Please log in now.");
                                    navigateToLogin();
                                });
                            }
                        }).exceptionally(ex -> {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                hideLoading();
                                showAlert(Alert.AlertType.ERROR, "Network Error", "Login attempt failed after signup: " + ex.getMessage());
                            });
                            return null;
                        });
            } else {
                // Handle initial signup errors
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

    private void navigateToDashboard(String userEmail, String userId, String userToken) {
        try {
            Stage stage = (Stage) signUpButton.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();

            // CRITICAL HANDOFF: Initialize the dashboard with the user data and token
            dashboardController.initializeUserData(userEmail, userId, userToken);

            Scene scene = new Scene(root);
            stage.setScene(scene);
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

            // Get the DialogPane and apply custom CSS
            DialogPane dialogPane = alert.getDialogPane();

            // 1. Add a custom style class for specific styling
            dialogPane.getStyleClass().add("custom-alert");

            // 2. Load the application's main CSS file
            String cssPath = "/styles/SignUpStyles.css";

            try {
                String cssUrl = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
                dialogPane.getStylesheets().add(cssUrl);
            } catch (NullPointerException e) {
                System.err.println("WARNING: Could not load CSS file at " + cssPath + ". Alert will be unstyled.");
            }

            alert.showAndWait();
        });
    }
}