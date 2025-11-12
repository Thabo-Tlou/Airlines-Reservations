package groupassingment.airlinesreservations.controllers;

import groupassingment.airlinesreservations.models.DatabaseHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.time.LocalDate;

public class ReservationFormController {

    // Customer Fields
    @FXML private TextField txt_entername; // Must be UNIQUE NOT NULL
    @FXML private TextField txt_idnumber; // Must be UNIQUE NOT NULL (10 digits)
    @FXML private TextField txt_enteraddress; // NOT NULL
    @FXML private TextField txt_enteremail; // UNIQUE NOT NULL
    @FXML private TextField txt_enterphone; // NOT NULL
    @FXML private TextField txt_enterprofession; // Concess - NOT NULL

    // Flight Fields
    @FXML private TextField txt_enterflightcode; // NOT NULL (6 digits)
    @FXML private DatePicker dp_traveldate; // Changed from TextField to DatePicker
    @FXML private TextField txt_departure; // Travel from - NOT NULL
    @FXML private TextField txt_destination; // Travel to - NOT NULL
    @FXML private TextField txt_seatclass; // NOT NULL
    @FXML private TextField txt_seatpreference; // NOT NULL

    @FXML private Button btn_submit;


    /**
     * Validates all form fields based on the new SQL constraints.
     * @return true if all fields are valid, false otherwise.
     */
    private boolean validateFields() {
        String name = txt_entername.getText();
        String idNum = txt_idnumber.getText();
        String address = txt_enteraddress.getText();
        String email = txt_enteremail.getText();
        String phone = txt_enterphone.getText();
        String concess = txt_enterprofession.getText();

        String flightCode = txt_enterflightcode.getText();
        LocalDate travelDate = dp_traveldate.getValue();
        String departure = txt_departure.getText();
        String destination = txt_destination.getText();
        String seatClass = txt_seatclass.getText();
        String seatPref = txt_seatpreference.getText();

        // Regex Patterns
        Pattern ID_PATTERN = Pattern.compile("^\\d{10}$");
        Pattern FLIGHT_CODE_PATTERN = Pattern.compile("^\\d{6}$");
        // Basic email pattern (can be more complex)
        Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        // 1. Check for basic null/empty fields
        if (name.isEmpty() || idNum.isEmpty() || address.isEmpty() || email.isEmpty() || phone.isEmpty() || concess.isEmpty() ||
                flightCode.isEmpty() || travelDate == null || departure.isEmpty() || destination.isEmpty() || seatClass.isEmpty() || seatPref.isEmpty()) {

            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required and cannot be empty.");
            return false;
        }

        // 2. ID Number validation (exactly 10 digits)
        if (!ID_PATTERN.matcher(idNum).matches()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "ID Number must contain exactly 10 digits.");
            return false;
        }

        // 3. Email validation
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid email address.");
            return false;
        }

        // 4. Flight Code validation (exactly 6 digits)
        if (!FLIGHT_CODE_PATTERN.matcher(flightCode).matches()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Flight Code must contain exactly 6 digits.");
            return false;
        }

        return true;
    }

    /**
     * Helper method to show JavaFX alert boxes.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the submit button action, performs validation, and saves data.
     */
    @FXML
    void submit_form(ActionEvent event) {
        if (validateFields()) {
            try {
                // This method will attempt to save both customer and reservation data
                saveReservationData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Flight reservation submitted successfully!");
                // Clear form fields here
            } catch (SQLException e) {
                // Catches database errors (like UNIQUE constraint violation for ID, Name, or Email)
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save reservation: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Connects to the database and saves the customer and reservation data.
     */
    private void saveReservationData() throws SQLException {
        // Step 1: Insert Customer Data
        String insertCustomerSQL = "INSERT INTO public.customers (name, id_num, address, email, phone, concess) VALUES (?, ?, ?, ?, ?, ?) RETURNING customer_id";

        // Step 2: Insert Reservation Data (uses the customer_id returned from Step 1)
        String insertReservationSQL = "INSERT INTO public.reservations (customer_id, flight_code, travel_date, travel_from, travel_to, seat_class, seat_pref) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // **NOTE:** You must implement a DatabaseHandler class to manage connections.
        try (Connection conn = DatabaseHandler.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            int customerId = -1;

            // --- 1. Save Customer ---
            try (PreparedStatement pstmt = conn.prepareStatement(insertCustomerSQL)) {
                pstmt.setString(1, txt_entername.getText());
                pstmt.setString(2, txt_idnumber.getText());
                pstmt.setString(3, txt_enteraddress.getText());
                pstmt.setString(4, txt_enteremail.getText());
                pstmt.setString(5, txt_enterphone.getText());
                pstmt.setString(6, txt_enterprofession.getText());

                // Execute and get the generated customer_id
                if (pstmt.execute()) {
                    try (var rs = pstmt.getResultSet()) {
                        if (rs.next()) {
                            customerId = rs.getInt(1); // Retrieve the generated ID
                        }
                    }
                }
            }

            // --- 2. Save Reservation ---
            if (customerId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertReservationSQL)) {
                    pstmt.setInt(1, customerId);
                    pstmt.setString(2, txt_enterflightcode.getText());
                    // Convert LocalDate to SQL Date
                    pstmt.setString(3, dp_traveldate.getValue().format(DateTimeFormatter.ISO_DATE));
                    pstmt.setString(4, txt_departure.getText());
                    pstmt.setString(5, txt_destination.getText());
                    pstmt.setString(6, txt_seatclass.getText());
                    pstmt.setString(7, txt_seatpreference.getText());

                    pstmt.executeUpdate();
                }
            }

            conn.commit(); // Commit transaction if both inserts succeeded
        } catch (SQLException e) {
            // Rollback transaction if any part fails
            // This rollback logic is usually handled better inside the DatabaseHandler,
            // but is shown here for context.
            throw e;
        }
    }
}