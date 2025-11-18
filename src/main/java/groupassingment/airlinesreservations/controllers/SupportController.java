package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class SupportController {

    // Contact Information Section
    @FXML private HBox dashboardBtn;
    @FXML private HBox reservationBtn;
    @FXML private HBox feedbackBtn;
    @FXML private HBox manageBtn;
    @FXML private HBox supportBtn;
    @FXML private HBox settingsBtn;
    @FXML private HBox logoutBtn;

    // Message Form Section
    @FXML private ComboBox<String> subjectCombo;
    @FXML private TextArea messageArea;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private Button chooseFileButton;
    @FXML private Label fileLabel;

    private File selectedFile;

    @FXML
    public void initialize() {
        setupNavigation();
        setupContactForm();
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

    private void setupContactForm() {
        // Populate the ComboBox items
        if (subjectCombo != null) {
            subjectCombo.getItems().addAll(
                    "Booking Assistance",
                    "Flight Change",
                    "Refund Request",
                    "Baggage Issue",
                    "Special Assistance",
                    "Other"
            );
        }

        // Set up button actions
        if (sendButton != null) {
            sendButton.setOnAction(e -> handleSendMessage());
        }
        if (clearButton != null) {
            clearButton.setOnAction(e -> handleClearForm());
        }
        if (chooseFileButton != null) {
            chooseFileButton.setOnAction(e -> handleChooseFile());
        }

        // Initialize file label
        if (fileLabel != null) {
            fileLabel.setText("No file chosen");
        }
    }

    @FXML
    private void handleSendMessage() {
        if (!validateForm()) {
            return;
        }

        // Simulate sending message (in real app, this would call your backend service)
        String subject = subjectCombo.getValue();
        String message = messageArea.getText().trim();
        String attachmentInfo = selectedFile != null ? " with attachment: " + selectedFile.getName() : "";

        System.out.println("Sending support message:");
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
        System.out.println("Attachment: " + (selectedFile != null ? selectedFile.getName() : "None"));

        // Show success message
        showAlert("Message Sent",
                "Thank you for contacting Bokamoso Airlines Support. " +
                        "We have received your message and will respond within 24 hours." +
                        (selectedFile != null ? "\n\nAttachment: " + selectedFile.getName() : ""),
                Alert.AlertType.INFORMATION);

        // Clear the form after successful submission
        handleClearForm();
    }

    @FXML
    private void handleClearForm() {
        if (subjectCombo != null) {
            subjectCombo.setValue(null);
        }
        if (messageArea != null) {
            messageArea.clear();
        }
        if (fileLabel != null) {
            fileLabel.setText("No file chosen");
        }
        selectedFile = null;
    }

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachment");

        // Set file filters
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Supported files (*.pdf, *.jpg, *.jpeg, *.png)",
                "*.pdf", "*.jpg", "*.jpeg", "*.png"
        );
        fileChooser.getExtensionFilters().add(extFilter);

        // Show open file dialog
        Stage stage = (Stage) chooseFileButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Check file size (10MB limit)
            long fileSizeInMB = file.length() / (1024 * 1024);
            if (fileSizeInMB > 10) {
                showAlert("File Too Large",
                        "The selected file exceeds the 10MB size limit. Please choose a smaller file.",
                        Alert.AlertType.ERROR);
                return;
            }

            selectedFile = file;
            if (fileLabel != null) {
                fileLabel.setText(file.getName());
            }
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // Validate subject
        if (subjectCombo == null || subjectCombo.getValue() == null) {
            errors.append("• Please select a subject for your inquiry\n");
        }

        // Validate message
        if (messageArea == null || messageArea.getText().trim().isEmpty()) {
            errors.append("• Please enter your message\n");
        } else if (messageArea.getText().trim().length() < 10) {
            errors.append("• Please provide more details in your message (minimum 10 characters)\n");
        }

        // Validate file if selected
        if (selectedFile != null) {
            String fileName = selectedFile.getName().toLowerCase();
            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".jpg") &&
                    !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
                errors.append("• Unsupported file type. Please select PDF, JPG, or PNG files only\n");
            }
        }

        if (errors.length() > 0) {
            showAlert("Validation Error", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Navigation Methods
    private void navigateToDashboard() {
        System.out.println("Navigating to Dashboard...");
        // Implementation: Load Dashboard.fxml
    }

    private void navigateToReservation() {
        System.out.println("Navigating to Reservation...");
        // Implementation: Load Reservation.fxml
    }

    private void navigateToFeedback() {
        System.out.println("Navigating to Feedback...");
        // Implementation: Load Feedback.fxml
    }

    private void navigateToManage() {
        System.out.println("Navigating to Manage Reservations...");
        // Implementation: Load ManageReservations.fxml
    }

    private void navigateToSupport() {
        System.out.println("Already on Support page");
        // No action needed - already on support page
    }

    private void navigateToSettings() {
        System.out.println("Navigating to Settings...");
        // Implementation: Load Settings.fxml
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("Logging out...");
                // Implementation: Clear session and return to login page
                // Example:
                // Stage stage = (Stage) logoutBtn.getScene().getWindow();
                // stage.setScene(loginScene);
            }
        });
    }

    // Additional utility methods
    public void simulatePhoneCall() {
        showAlert("Phone Support",
                "Calling: +1 (555) 123-4567\n\n" +
                        "Our phone support is available 24/7 to assist you with any urgent matters.",
                Alert.AlertType.INFORMATION);
    }

    public void openLiveChat() {
        showAlert("Live Chat",
                "Live chat support is available from 6:00 AM to 10:00 PM.\n\n" +
                        "Our agents are ready to help you with real-time assistance.",
                Alert.AlertType.INFORMATION);
    }

    public void showEmailTemplate() {
        if (subjectCombo != null && messageArea != null) {
            String subject = subjectCombo.getValue() != null ? subjectCombo.getValue() : "General Inquiry";
            String body = messageArea.getText();

            String emailTemplate =
                    "To: support@bokamoso.com\n" +
                            "Subject: " + subject + "\n\n" +
                            "Dear Bokamoso Airlines Support,\n\n" +
                            (body.isEmpty() ? "[Please enter your message here]" : body) + "\n\n" +
                            "Thank you,\n" +
                            "[Your Name]";

            showAlert("Email Template", emailTemplate, Alert.AlertType.INFORMATION);
        }
    }

    // Method to handle FAQ expansion (if needed for additional functionality)
    public void handleFaqExpansion(TitledPane faqPane) {
        // This method can be used if you want to add specific behavior when FAQ items are expanded
        System.out.println("FAQ expanded: " + faqPane.getText());
    }
}