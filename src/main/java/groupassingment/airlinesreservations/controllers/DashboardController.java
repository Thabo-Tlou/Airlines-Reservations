package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;

    @FXML private ComboBox<String> originComboBox;
    @FXML private ComboBox<String> destinationComboBox;
    @FXML private ComboBox<String> dateComboBox;
    @FXML private Spinner<Integer> passengerSpinner;

    @FXML private Button bookFlightButton;
    @FXML private Button reservationButton;
    @FXML private Button logoutButton;

    @FXML private Button dashboardButton;
    @FXML private Button feedbackButton;
    @FXML private Button manageReservationsButton;
    @FXML private Button supportButton;
    @FXML private Button settingsButton;

    @FXML
    public void initialize() {
        // Initialize ComboBoxes with sample data
        originComboBox.getItems().addAll("Maseru", "Johannesburg", "Cape Town");
        destinationComboBox.getItems().addAll("Johannesburg", "Cape Town", "Maseru");
        dateComboBox.getItems().addAll("Nov 15, 2025", "Nov 16, 2025", "Nov 17, 2025");

        passengerSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));

        welcomeLabel.setText("Welcome Mr Thabo!");
        profileNameLabel.setText("Thabo Moloi");
        profileEmailLabel.setText("thabo.moloi@email.com");
    }

    @FXML
    private void handleBookFlight() {
        String origin = originComboBox.getValue();
        String destination = destinationComboBox.getValue();
        String date = dateComboBox.getValue();
        int passengers = passengerSpinner.getValue();

        if (origin == null || destination == null || date == null) {
            System.out.println("Please fill in all fields before booking.");
            return;
        }

        System.out.println("Booking flight from " + origin + " to " + destination + " on " + date + " for " + passengers + " passenger(s).");

        // TODO: Implement actual booking logic
    }

    @FXML
    private void handleReservationView() {
        System.out.println("Navigating to reservation view...");
        // TODO: Implement navigation logic
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logging out...");
        // TODO: Implement logout logic
    }

    @FXML
    private void handleDashboard() {
        System.out.println("Navigating to dashboard...");
    }

    @FXML
    private void handleFeedback() {
        System.out.println("Navigating to feedback view...");
    }

    @FXML
    private void handleManageReservations() {
        System.out.println("Navigating to manage reservations view...");
    }

    @FXML
    private void handleSupport() {
        System.out.println("Navigating to support view...");
    }

    @FXML
    private void handleSettings() {
        System.out.println("Navigating to settings view...");
    }
}
