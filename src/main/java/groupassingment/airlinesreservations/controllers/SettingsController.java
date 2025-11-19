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

    // Navigation - Since your FXML uses HBox with buttons inside, we need to handle this differently
    @FXML private HBox dashboardBtn;
    @FXML private HBox reservationBtn;
    @FXML private HBox feedbackBtn;
    @FXML private HBox manageBtn;
    @FXML private HBox supportBtn;
    @FXML private HBox settingsBtn;
    @FXML private HBox logoutBtn;

    private Preferences userPreferences;
    private boolean fieldsInitialized = false;

    @FXML
    public void initialize() {
        userPreferences = Preferences.userNodeForPackage(SettingsController.class);

        // Initialize with a small delay to ensure FXML fields are injected
        javafx.application.Platform.runLater(() -> {
            initializeFields();
        });
    }

    private void initializeFields() {
        try {
            setupComboBoxes();
            loadUserPreferences();
            setupNavigation();
            fieldsInitialized = true;
        } catch (Exception e) {
            System.err.println("Error initializing fields: " + e.getMessage());
            showAlert("Initialization Error", "Failed to initialize settings form.", Alert.AlertType.ERROR);
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
            emailField.setText(userPreferences.get("email", ""));
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
        setupNavigationHandler(dashboardBtn, "Dashboard");
        setupNavigationHandler(reservationBtn, "Reservation");
        setupNavigationHandler(feedbackBtn, "Feedback");
        setupNavigationHandler(manageBtn, "Manage Reservations");
        setupNavigationHandler(supportBtn, "Support");
        setupNavigationHandler(settingsBtn, "Settings");
        setupNavigationHandler(logoutBtn, "Logout");
    }

    private void setupNavigationHandler(HBox navItem, String pageName) {
        if (navItem != null) {
            navItem.setOnMouseClicked(event -> {
                handleNavigation(pageName);
            });
        }
    }

    private void handleNavigation(String pageName) {
        switch (pageName) {
            case "Dashboard":
                navigateToDashboard();
                break;
            case "Reservation":
                navigateToReservation();
                break;
            case "Feedback":
                navigateToFeedback();
                break;
            case "Manage Reservations":
                navigateToManage();
                break;
            case "Support":
                navigateToSupport();
                break;
            case "Settings":
                // Already on settings page
                break;
            case "Logout":
                logout();
                break;
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

    private void navigateToDashboard() {
        System.out.println("Navigating to Dashboard...");
        // Add your navigation logic here
    }

    private void navigateToReservation() {
        System.out.println("Navigating to Reservation...");
        // Add your navigation logic here
    }

    private void navigateToFeedback() {
        System.out.println("Navigating to Feedback...");
        // Add your navigation logic here
    }

    private void navigateToManage() {
        System.out.println("Navigating to Manage Reservations...");
        // Add your navigation logic here
    }

    private void navigateToSupport() {
        System.out.println("Navigating to Support...");
        // Add your navigation logic here
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("Logging out...");
                // logout logic here
            }
        });
    }
}