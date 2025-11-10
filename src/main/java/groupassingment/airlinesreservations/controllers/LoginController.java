package groupassingment.airlinesreservations.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONObject;
import java.net.http.HttpResponse;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final SupabaseService supabase = new SupabaseService();

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // 1. Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both email and password.");
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Email and password cannot be empty.");
            return;
        }

        statusLabel.setText("Logging in...");
        System.out.println("LOG: Starting Supabase login for email: " + email);

        // 2. Call Supabase login endpoint
        supabase.login(email, password).thenAccept(response -> {
            int status = response.statusCode();
            String body = response.body();

            System.out.println("LOG: Supabase Auth Response Status: " + status);

            if (status == 200) {
                System.out.println("LOG: Login successful.");
                Platform.runLater(() -> {
                    statusLabel.setText("Login successful! Welcome back.");
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome back!");
                });

                // Optional: parse returned session info or JWT
                try {
                    JSONObject json = new JSONObject(body);
                    String accessToken = json.optString("access_token", "");
                    System.out.println("LOG: Access token received: " + accessToken);
                    // TODO: store token locally if needed for authenticated requests
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to parse login response JSON.");
                    e.printStackTrace();
                }

            } else {
                // 3. Handle failed login
                try {
                    JSONObject errorJson = new JSONObject(body);
                    String errorMsg = errorJson.optString("msg", body);
                    System.err.println("ERROR: Supabase login failed (Status: " + status + "): " + errorMsg);
                    Platform.runLater(() -> {
                        statusLabel.setText("Login failed: " + errorMsg);
                        showAlert(Alert.AlertType.ERROR, "Login Failed", errorMsg);
                    });
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to parse Supabase login error body.");
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        statusLabel.setText("Login failed: " + body);
                        showAlert(Alert.AlertType.ERROR, "Login Failed", body);
                    });
                }
            }

        }).exceptionally(ex -> {
            System.err.println("FATAL ERROR: Network or unexpected exception during login attempt.");
            ex.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("Network Error: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Network Error", ex.getMessage());
            });
            return null;
        });
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
