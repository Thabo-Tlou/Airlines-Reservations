package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import java.util.prefs.Preferences;

public class SettingsController {

    // Profile Fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    // Security Fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Preference Checkboxes
    @FXML private CheckBox emailNotificationsCheck;
    @FXML private CheckBox smsNotificationsCheck;
    @FXML private CheckBox specialOffersCheck;
    @FXML private CheckBox newsletterCheck;

    // ComboBoxes
    @FXML private ComboBox<String> languageCombo;
    @FXML private ComboBox<String> timezoneCombo;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button changeAvatarButton;

    // Navigation
    @FXML private HBox dashboardBtn;
    @FXML private HBox reservationBtn;
    @FXML private HBox feedbackBtn;
    @FXML private HBox manageBtn;
    @FXML private HBox supportBtn;
    @FXML private HBox settingsBtn;
    @FXML private HBox logoutBtn;

    private Preferences userPreferences;
    private boolean fieldsInitialized = false;

    // Session data
    private String userAuthToken;
    private String userId;
    private String userEmail;

    @FXML
    public void initialize() {
        userPreferences = Preferences.userNodeForPackage(SettingsController.class);

        // Initialize with a small delay to ensure FXML fields are injected
        javafx.application.Platform.runLater(() -> {
            initializeFields();
        });
    }

    public void initializeSessionData(String authToken, String userId, String userEmail) {
        this.userAuthToken = authToken;
        this.userId = userId;
        this.userEmail = userEmail;

        System.out.println("=== SettingsController: Session initialized ===");
        System.out.println("User Email: " + userEmail);
        System.out.println("Auth Token: " + (authToken != null ? "PRESENT" : "NULL"));
        System.out.println("User ID: " + userId);
        System.out.println("=== END SESSION INIT ===");

        // Pre-fill email field with session data
        if (emailField != null && userEmail != null) {
            emailField.setText(userEmail);
        }
    }

    private void initializeFields() {
        try {
            setupComboBoxes();
            loadUserPreferences();
            setupNavigation();
            fieldsInitialized = true;
        } catch (Exception e) {
            System.err.println("Error initializing fields: " + e.getMessage());
            // Optional: showAlert("Initialization Error", "Failed to initialize settings form.", Alert.AlertType.ERROR);
        }
    }

    private void loadUserPreferences() {
        // Safe field access with null checks
        if (firstNameField != null) {
            firstNameField.setText(userPreferences.get("firstName", ""));
        }
        if (lastNameField != null) {
            lastNameField.setText(userPreferences.get("lastName", ""));
        }
        if (emailField != null) {
            // Only set email from preferences if we don't have session email
            if (userEmail == null) {
                emailField.setText(userPreferences.get("email", ""));
            }
        }
        if (phoneField != null) {
            phoneField.setText(userPreferences.get("phone", ""));
        }

        if (emailNotificationsCheck != null) {
            emailNotificationsCheck.setSelected(userPreferences.getBoolean("emailNotifications", true));
        }
        if (smsNotificationsCheck != null) {
            smsNotificationsCheck.setSelected(userPreferences.getBoolean("smsNotifications", true));
        }
        if (specialOffersCheck != null) {
            specialOffersCheck.setSelected(userPreferences.getBoolean("specialOffers", true));
        }
        if (newsletterCheck != null) {
            newsletterCheck.setSelected(userPreferences.getBoolean("newsletter", false));
        }

        String savedLanguage = userPreferences.get("language", "English");
        String savedTimezone = userPreferences.get("timezone", "UTC");

        if (languageCombo != null) {
            languageCombo.getSelectionModel().select(savedLanguage);
        }
        if (timezoneCombo != null) {
            timezoneCombo.getSelectionModel().select(savedTimezone);
        }
    }

    private void setupComboBoxes() {
        if (languageCombo != null) {
            languageCombo.getItems().addAll(
                    "English", "Spanish", "French", "German", "Chinese", "Japanese", "Arabic"
            );
            languageCombo.setValue("English");
        }

        if (timezoneCombo != null) {
            timezoneCombo.getItems().addAll(
                    "UTC", "EST", "PST", "CET", "GMT", "IST", "JST", "AEST"
            );
            timezoneCombo.setValue("UTC");
        }
    }

    private void setupNavigation() {
        // Setup navigation handlers for each HBox
        if (dashboardBtn != null) {
            dashboardBtn.setOnMouseClicked(event -> navigateToDashboard());
        }
        if (reservationBtn != null) {
            reservationBtn.setOnMouseClicked(event -> navigateToReservation());
        }
        if (feedbackBtn != null) {
            feedbackBtn.setOnMouseClicked(event -> navigateToFeedback());
        }
        if (manageBtn != null) {
            manageBtn.setOnMouseClicked(event -> navigateToManage());
        }
        if (supportBtn != null) {
            supportBtn.setOnMouseClicked(event -> navigateToSupport());
        }
        if (settingsBtn != null) {
            settingsBtn.setOnMouseClicked(event -> navigateToSettings());
        }
        if (logoutBtn != null) {
            logoutBtn.setOnMouseClicked(event -> logout());
        }
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        if (!fieldsInitialized) {
            showAlert("Error", "Form not properly initialized.", Alert.AlertType.ERROR);
            return;
        }

        if (validateForm()) {
            savePreferences();
            showAlert("Success", "Settings saved successfully!", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (fieldsInitialized) {
            loadUserPreferences();
            showAlert("Cancelled", "Changes discarded.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleChangeAvatar(ActionEvent event) {
        showAlert("Feature", "Avatar change feature coming soon!", Alert.AlertType.INFORMATION);
    }

    private boolean validateForm() {
        if (!fieldsInitialized) {
            return false;
        }

        StringBuilder errors = new StringBuilder();

        if (firstNameField != null && firstNameField.getText().trim().isEmpty()) {
            errors.append("• First name is required\n");
        }

        // Only validate fields if they exist in the view
        if (lastNameField != null && lastNameField.getText().trim().isEmpty()) {
            errors.append("• Last name is required\n");
        }

        if (emailField != null) {
            if (emailField.getText().trim().isEmpty()) {
                errors.append("• Email is required\n");
            } else if (!isValidEmail(emailField.getText().trim())) {
                errors.append("• Please enter a valid email address\n");
            }
        }

        // Password validation only if current password is provided
        if (currentPasswordField != null && !currentPasswordField.getText().isEmpty()) {
            if (newPasswordField == null || newPasswordField.getText().isEmpty()) {
                errors.append("• New password is required when changing password\n");
            } else if (newPasswordField.getText().length() < 6) {
                errors.append("• New password must be at least 6 characters\n");
            } else if (confirmPasswordField == null || !newPasswordField.getText().equals(confirmPasswordField.getText())) {
                errors.append("• New passwords do not match\n");
            }
        }

        if (errors.length() > 0) {
            showAlert("Validation Error", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void savePreferences() {
        if (!fieldsInitialized) return;

        if (firstNameField != null) {
            userPreferences.put("firstName", firstNameField.getText().trim());
        }
        if (lastNameField != null) {
            userPreferences.put("lastName", lastNameField.getText().trim());
        }
        if (emailField != null) {
            userPreferences.put("email", emailField.getText().trim());
        }
        if (phoneField != null) {
            userPreferences.put("phone", phoneField.getText().trim());
        }

        if (emailNotificationsCheck != null) {
            userPreferences.putBoolean("emailNotifications", emailNotificationsCheck.isSelected());
        }
        if (smsNotificationsCheck != null) {
            userPreferences.putBoolean("smsNotifications", smsNotificationsCheck.isSelected());
        }
        if (specialOffersCheck != null) {
            userPreferences.putBoolean("specialOffers", specialOffersCheck.isSelected());
        }
        if (newsletterCheck != null) {
            userPreferences.putBoolean("newsletter", newsletterCheck.isSelected());
        }

        if (languageCombo != null && languageCombo.getValue() != null) {
            userPreferences.put("language", languageCombo.getValue());
        }

        if (timezoneCombo != null && timezoneCombo.getValue() != null) {
            userPreferences.put("timezone", timezoneCombo.getValue());
        }

        // Save password only if new password is provided
        if (newPasswordField != null && !newPasswordField.getText().isEmpty()) {
            userPreferences.put("password", newPasswordField.getText());
            clearPasswordFields();
        }
    }

    private void clearPasswordFields() {
        if (currentPasswordField != null) currentPasswordField.clear();
        if (newPasswordField != null) newPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== NAVIGATION METHODS ====================

    private void navigateToDashboard() {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        if (dashboardBtn != null) {
            SceneManager.navigateToDashboard(dashboardBtn.getScene());
        }
    }

    private void navigateToReservation() {
        System.out.println("=== NAVIGATING TO RESERVATION FORM ===");
        if (reservationBtn != null) {
            SceneManager.navigateToReservationForm(reservationBtn.getScene());
        }
    }

    private void navigateToFeedback() {
        System.out.println("=== NAVIGATING TO FEEDBACK ===");
        if (feedbackBtn != null) {
            SceneManager.navigateToFeedback(feedbackBtn.getScene());
        }
    }

    private void navigateToManage() {
        System.out.println("=== NAVIGATING TO MANAGE RESERVATIONS ===");
        if (manageBtn != null) {
            SceneManager.navigateToManageReservations(manageBtn.getScene());
        }
    }

    private void navigateToSupport() {
        System.out.println("=== NAVIGATING TO SUPPORT ===");
        if (supportBtn != null) {
            SceneManager.navigateToSupport(supportBtn.getScene());
        }
    }

    private void navigateToSettings() {
        System.out.println("Already on Settings page");
        // No action needed - already on Settings page
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("=== HANDLING LOGOUT ===");
                if (logoutBtn != null) {
                    SceneManager.handleLogout(logoutBtn.getScene());
                }
            }
        });
    }
}