package groupassingment.airlinesreservations.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;

public class FeedbackController {

    @FXML
    private RadioButton btn_radioButtonOS1;
    @FXML
    private RadioButton btn_radioButtonOS2;
    @FXML
    private RadioButton btn_radioButtonOS3;
    @FXML
    private RadioButton btn_radioButtonOS4;
    @FXML
    private RadioButton btn_radioButtonOS5;
    @FXML
    private RadioButton btn_radioButtonTS1;
    @FXML
    private RadioButton btn_radioButtonTS2;
    @FXML
    private RadioButton btn_radioButtonTS3;
    @FXML
    private RadioButton btn_radioButtonTS4;
    @FXML
    private RadioButton btn_radioButtonTS5;
    @FXML
    private Button btn_send;
    @FXML
    private HBox hb_HBoxDashboard;
    @FXML
    private Label lbl_address;
    @FXML
    private Label lbl_email;
    @FXML
    private Label lbl_fullName;
    @FXML
    private Label lbl_number;
    @FXML
    private Label lbl_onlineSatisfaction;
    @FXML
    private Label lbl_passanger;
    @FXML
    private Label lbl_phone;
    @FXML
    private Label lbl_phoneNum;
    @FXML
    private Label lbl_ticket;
    @FXML
    private Label lbl_tourSatisfaction;
    @FXML
    private Label lbl_travelFeedback;
    @FXML
    private TextField txtField_emailAddress;
    @FXML
    private TextField txtField_fullName;
    @FXML
    private TextField txtField_phoneNumber;
    @FXML
    private TextField txtField_ticketNumber;
    @FXML
    private TextArea txt_textArea;

    // Session data fields
    private String authToken;
    private String userId;
    private String userEmail;

    // ToggleGroups to ensure only one selection per group
    private ToggleGroup tourSatisfactionGroup;
    private ToggleGroup onlineServiceGroup;

    @FXML
    public void initialize() {
        // Initialize ToggleGroup for Tour Satisfaction
        tourSatisfactionGroup = new ToggleGroup();
        btn_radioButtonTS1.setToggleGroup(tourSatisfactionGroup);
        btn_radioButtonTS2.setToggleGroup(tourSatisfactionGroup);
        btn_radioButtonTS3.setToggleGroup(tourSatisfactionGroup);
        btn_radioButtonTS4.setToggleGroup(tourSatisfactionGroup);
        btn_radioButtonTS5.setToggleGroup(tourSatisfactionGroup);

        // Initialize ToggleGroup for Online Service
        onlineServiceGroup = new ToggleGroup();
        btn_radioButtonOS1.setToggleGroup(onlineServiceGroup);
        btn_radioButtonOS2.setToggleGroup(onlineServiceGroup);
        btn_radioButtonOS3.setToggleGroup(onlineServiceGroup);
        btn_radioButtonOS4.setToggleGroup(onlineServiceGroup);
        btn_radioButtonOS5.setToggleGroup(onlineServiceGroup);
    }

    /**
     * Initialize session data when navigating to this controller
     */
    public void initializeSessionData(String authToken, String userId, String userEmail) {
        this.authToken = authToken;
        this.userId = userId;
        this.userEmail = userEmail;

        System.out.println("=== FEEDBACK CONTROLLER SESSION DATA ===");
        System.out.println("User Email: " + this.userEmail);
        System.out.println("User ID: " + this.userId);
        System.out.println("Auth Token: " + (this.authToken != null ? "PRESENT" : "NULL"));
        System.out.println("=== END FEEDBACK SESSION DATA ===");

        // Pre-fill user data if available
        if (this.userEmail != null && !this.userEmail.isEmpty()) {
            txtField_emailAddress.setText(this.userEmail);
        }
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleDashboardNavigation(ActionEvent event) {
        System.out.println("Navigating to Dashboard from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToDashboard(currentScene);
    }

    @FXML
    private void handleReservationFormNavigation(ActionEvent event) {
        System.out.println("Navigating to Reservation Form from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToReservationForm(currentScene);
    }

    @FXML
    private void handleManageReservationsNavigation(ActionEvent event) {
        System.out.println("Navigating to Manage Reservations from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToManageReservations(currentScene);
    }

    @FXML
    private void handleFeedbackNavigation(ActionEvent event) {
        // Already on feedback page, do nothing or show message
        System.out.println("Already on Feedback page");
    }

    @FXML
    private void handleSupportNavigation(ActionEvent event) {
        System.out.println("Navigating to Support from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToSupport(currentScene);
    }

    @FXML
    private void handleSettingsNavigation(ActionEvent event) {
        System.out.println("Navigating to Settings from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToSettings(currentScene);
    }

    @FXML
    private void handlePrintTicketNavigation(ActionEvent event) {
        System.out.println("Navigating to Print Ticket from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.navigateToPrintTicket(currentScene);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Logging out from Feedback");
        Scene currentScene = btn_send.getScene();
        SceneManager.handleLogout(currentScene);
    }

    // ==================== FEEDBACK-SPECIFIC METHODS ====================

    @FXML
    void handleSendButton(ActionEvent event) {
        if (!validateAllFields()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Feedback successfully sent!");
        alert.showAndWait();

        clearForm();
    }

    private boolean validateAllFields() {
        if (txtField_fullName.getText() == null || txtField_fullName.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Enter your full name");
            txtField_fullName.requestFocus();
            return false;
        }

        if (txtField_emailAddress.getText() == null || txtField_emailAddress.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Enter your email address");
            txtField_emailAddress.requestFocus();
            return false;
        } else if (!isValidEmail(txtField_emailAddress.getText().trim())) {
            showAlert("Validation Error", "Enter a valid email address");
            txtField_emailAddress.requestFocus();
            return false;
        }

        if (txtField_phoneNumber.getText() == null || txtField_phoneNumber.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Enter your phone number");
            txtField_phoneNumber.requestFocus();
            return false;
        }

        if (txtField_ticketNumber.getText() == null || txtField_ticketNumber.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Enter your ticket number");
            txtField_ticketNumber.requestFocus();
            return false;
        }

        if (tourSatisfactionGroup.getSelectedToggle() == null) {
            showAlert("Validation Error", "Select a Tour Satisfaction rating");
            return false;
        }

        if (onlineServiceGroup.getSelectedToggle() == null) {
            showAlert("Validation Error", "Select an Online Service rating");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        // Basic email validation regex
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        txtField_fullName.clear();
        txtField_emailAddress.clear();
        txtField_phoneNumber.clear();
        txtField_ticketNumber.clear();
        txt_textArea.clear();
        clearRadioButtonSelections();

        // Restore user email if available
        if (this.userEmail != null && !this.userEmail.isEmpty()) {
            txtField_emailAddress.setText(this.userEmail);
        }
    }

    private void clearRadioButtonSelections() {
        tourSatisfactionGroup.selectToggle(null);
        onlineServiceGroup.selectToggle(null);
    }

    // ==================== GETTERS FOR SESSION DATA ====================

    public String getAuthToken() {
        return authToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }
}