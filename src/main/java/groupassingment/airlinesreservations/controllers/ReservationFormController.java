package groupassingment.airlinesreservations.controllers;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReservationFormController {

    private SupabaseService supabaseService;

    private String lastBookingDetails;
    private double lastCalculatedPrice;

    private String userAuthToken;
    private String userId;
    private String userEmail;

    // FXML elements remain the same
    @FXML private Label lbl_customer;
    @FXML private TextField txt_entername;
    @FXML private TextField txt_idnumber;
    @FXML private TextField txt_enteraddress;
    @FXML private TextField txt_enteremail;
    @FXML private TextField txt_enterphone;
    @FXML private ComboBox<String> combo_categoryfare;

    @FXML private Label lbl_flight;
    @FXML private TextField txt_enterflightcode;
    @FXML private DatePicker dp_traveldate;
    @FXML private ComboBox<String> combo_travelfrom;
    @FXML private ComboBox<String> combo_travelto;
    @FXML private ComboBox<String> combo_seatclass;
    @FXML private ComboBox<String> combo_seatpref;

    @FXML private Button btn_dashboard;
    @FXML private Button btn_reservation;
    @FXML private Button btn_managerservations;
    @FXML private Button btn_feedback;
    @FXML private Button btn_support;
    @FXML private Button btn_settings;
    @FXML private Button btn_logout;

    @FXML private Button btn_submit;
    @FXML private Button btn_submit1;
    @FXML private Label lbl_status;


    @FXML
    public void initialize() {
        supabaseService = new SupabaseService();
        populateStaticComboBoxes();
        populateAirportComboBoxes();
        generateUniqueFlightCode();
        btn_submit1.setDisable(true);
        lbl_status.setText("");
    }

    private void generateUniqueFlightCode() {
        Random random = new Random();
        int min = 100000;
        int max = 999999;
        String code = String.valueOf(random.nextInt(max - min + 1) + min);
        txt_enterflightcode.setText(code);
        txt_enterflightcode.setDisable(true);
    }

    /**
     * FIX: Trim the token here for defensive programming.
     */
    public void initializeSessionData(String userAuthToken, String userId, String userEmail) {
        this.userAuthToken = userAuthToken != null ? userAuthToken.trim() : null;
        this.userId = userId;
        this.userEmail = userEmail;

        if (this.userEmail != null && !this.userEmail.isEmpty()) {
            txt_enteremail.setText(this.userEmail);
        }
    }


    private void populateStaticComboBoxes() {
        combo_categoryfare.setItems(FXCollections.observableArrayList(
                "Standard",
                "Student",
                "Senior Citizen",
                "Cancer patient"
        ));

        combo_seatclass.setItems(FXCollections.observableArrayList(
                "Economy",
                "Business",
                "First Class"
        ));

        combo_seatpref.setItems(FXCollections.observableArrayList(
                "Seat by the window",
                "Seat by the isle",
                "Seat by the door"
        ));
    }

    private void populateAirportComboBoxes() {
        supabaseService.getAirports().thenAccept(response -> {
            if (response.statusCode() == 200) {
                JSONArray airports = new JSONArray(response.body());

                List<String> airportList = StreamSupport.stream(airports.spliterator(), false)
                        .map(obj -> (JSONObject) obj)
                        .map(airport -> String.format("%s - %s, %s",
                                airport.getString("airport_code"),
                                airport.getString("city"),
                                airport.getString("country")))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    combo_travelfrom.setItems(FXCollections.observableArrayList(airportList));
                    combo_travelto.setItems(FXCollections.observableArrayList(airportList));
                });
            } else {
                Platform.runLater(() -> {
                    showErrorAlert("Failed to Load Airports", "Could not fetch airport data from the database. Status code: " + response.statusCode());
                });
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showErrorAlert("Network Error", "An error occurred while trying to connect: " + ex.getMessage());
            });
            return null;
        });
    }

    /**
     * CRITICAL FIX: The arguments passed to insertCustomer are in the correct order.
     */
    @FXML
    private void handleBookFlight() {
        btn_submit.setDisable(true);
        lbl_status.setText("Checking input...");

        if (userAuthToken == null || userAuthToken.isEmpty() || userId == null || userId.isEmpty()) {
            showErrorAlert("Session Error (Possible 401 Cause)", "User session data (Auth Token/User ID) is not valid. Ensure 'initializeSessionData' was called correctly.");
            btn_submit.setDisable(false);
            return;
        }

        String validationError = getValidationErrors();

        if (validationError != null) {
            lbl_status.setText("Validation failed.");
            showErrorAlert("Input Error", validationError);
            btn_submit.setDisable(false);
            return;
        }

        // 3. Create Customer - CORRECTED CALL
        supabaseService.insertCustomer(
                txt_entername.getText(),
                txt_idnumber.getText(),
                txt_enteraddress.getText(),
                txt_enteremail.getText(),
                txt_enterphone.getText(),
                combo_categoryfare.getValue(),
                this.userId, // CORRECT: User ID is now the 7th argument for the payload
                this.userAuthToken // CORRECT: User Token is now the 8th argument for Authorization
        ).thenAccept(customerResponse -> {
            if (customerResponse.statusCode() == 201) {
                JSONArray newCustomerArray = new JSONArray(customerResponse.body());
                if (newCustomerArray.length() == 0) {
                    throw new RuntimeException("Failed to parse new customer data. Response: " + customerResponse.body());
                }
                int newCustomerId = newCustomerArray.getJSONObject(0).getInt("customer_id");

                // 4. Create Reservation
                createReservation(newCustomerId, userAuthToken);

            } else {
                Platform.runLater(() -> {
                    showErrorAlert("Customer Insertion Failed (400?)",
                            "Failed to save customer data.\nStatus: " + customerResponse.statusCode() +
                                    "\nResponse (Check Console): " + customerResponse.body());
                    btn_submit.setDisable(false);
                });
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showErrorAlert("Booking Error", "An unexpected error occurred during customer insertion: " + ex.getMessage());
                btn_submit.setDisable(false);
            });
            return null;
        });
    }

    private String getValidationErrors() {
        if (txt_entername.getText().trim().isEmpty()) return "Customer Name is required.";
        if (txt_idnumber.getText().trim().isEmpty()) return "ID Number is required.";
        if (!txt_idnumber.getText().trim().matches("^\\d{10}$")) return "ID Number must be exactly 10 digits.";
        if (txt_enteraddress.getText().trim().isEmpty()) return "Address is required.";
        if (txt_enteremail.getText().trim().isEmpty()) return "Email is required.";
        if (!txt_enteremail.getText().contains("@")) return "Email is invalid. Must contain '@'.";
        if (txt_enterphone.getText().trim().isEmpty()) return "Phone number is required.";
        if (combo_categoryfare.getValue() == null) return "Fare Category must be selected.";

        if (txt_enterflightcode.getText().trim().isEmpty()) return "Flight Code is required.";
        if (!txt_enterflightcode.getText().trim().matches("^\\d{6}$")) return "Flight Code must be exactly 6 digits.";
        if (dp_traveldate.getValue() == null) return "Travel Date must be selected.";
        if (combo_travelfrom.getValue() == null) return "Departure Airport (Travel From) must be selected.";
        if (combo_travelto.getValue() == null) return "Destination Airport (Travel To) must be selected.";
        if (combo_seatclass.getValue() == null) return "Seat Class must be selected.";
        if (combo_seatpref.getValue() == null) return "Seat Preference must be selected.";

        return null;
    }


    private void createReservation(int customerId, String userAuthToken) {
        String flightCode = txt_enterflightcode.getText();
        String travelDate = dp_traveldate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String travelFrom = combo_travelfrom.getValue().split(" - ")[0];
        String travelTo = combo_travelto.getValue().split(" - ")[0];
        String seatClass = combo_seatclass.getValue();
        String seatPref = combo_seatpref.getValue();

        JSONObject reservationPayload = new JSONObject();
        reservationPayload.put("customer_id", customerId);
        reservationPayload.put("flight_code", flightCode);
        reservationPayload.put("travel_date", travelDate);

        // 🔑 FIX: Corrected keys to match schema
        reservationPayload.put("travel_from", travelFrom);
        reservationPayload.put("travel_to", travelTo);

        reservationPayload.put("seat_class", seatClass);

        // 🔑 FIX: Corrected key to match schema
        reservationPayload.put("seat_pref", seatPref);

        // Removed: reservationPayload.put("user_email", this.userEmail); // Not needed in reservations table

        supabaseService.insertFlightBooking(reservationPayload, userAuthToken)
                .thenAccept(reservationResponse -> {
                    if (reservationResponse.statusCode() == 201) {
                        lastCalculatedPrice = calculatePrice();
                        lastBookingDetails = buildTicketString();

                        Platform.runLater(() -> {
                            lbl_status.setText("Booking successful! Code: " + flightCode);
                            btn_submit1.setDisable(false);
                            btn_submit.setDisable(false);
                            generateUniqueFlightCode();
                            showSuccessAlert("Booking Confirmed", "Your flight has been successfully booked (Code: " + flightCode + "). You can now print your ticket.");
                        });
                    } else {
                        Platform.runLater(() -> {
                            showErrorAlert("Reservation Error",
                                    "Failed to save reservation data.\nStatus: " + reservationResponse.statusCode() +
                                            "\nResponse: " + reservationResponse.body());
                            btn_submit.setDisable(false);
                        });
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorAlert("Reservation Error", "An unexpected error occurred during reservation insertion: " + ex.getMessage());
                        btn_submit.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handlePrintTicket() {
        if (lastBookingDetails == null || lastBookingDetails.isEmpty()) {
            if (lbl_status != null) {
                lbl_status.setText("No ticket data available. Please book a flight first.");
            }
            showErrorAlert("No Ticket Data", "Please book a flight first.");
            return;
        }

        Alert ticketAlert = new Alert(Alert.AlertType.INFORMATION);
        ticketAlert.setTitle("Your Flight Ticket");
        ticketAlert.setHeaderText("Booking Confirmation");
        ticketAlert.setContentText(lastBookingDetails);
        ticketAlert.showAndWait();

        if (lbl_status != null) {
            lbl_status.setText("Ticket displayed successfully.");
        }
    }

    private double calculatePrice() {
        double basePrice = 350.0;
        String fare = Optional.ofNullable(combo_categoryfare.getValue()).orElse("Standard");

        switch (fare) {
            case "Student": return basePrice * 0.80;
            case "Senior Citizen": return basePrice * 0.75;
            case "Cancer patient": return basePrice * 0.50;
            case "Standard":
            default: return basePrice;
        }
    }

    private String buildTicketString() {
        return String.format(
                "--- CUSTOMER ---\n" +
                        "Name: %s\n" +
                        "Email: %s\n" +
                        "Phone: %s\n" +
                        "ID Number: %s\n" +
                        "Fare Category: %s\n" +
                        "\n" +
                        "--- FLIGHT ---\n" +
                        "Flight Code: %s\n" +
                        "Date: %s\n" +
                        "From: %s\n" +
                        "To: %s\n" +
                        "\n" +
                        "--- SEAT ---\n" +
                        "Class: %s\n" +
                        "Preference: %s\n" +
                        "\n" +
                        "--- PRICE ---\n" +
                        "Total Paid: $%.2f",
                txt_entername.getText(),
                txt_enteremail.getText(),
                txt_enterphone.getText(),
                txt_idnumber.getText(),
                combo_categoryfare.getValue(),
                txt_enterflightcode.getText(),
                dp_traveldate.getValue().toString(),
                combo_travelfrom.getValue(),
                combo_travelto.getValue(),
                combo_seatclass.getValue(),
                combo_seatpref.getValue(),
                lastCalculatedPrice
        );
    }

    private void showErrorAlert(String title, String content) {
        lbl_status.setText(title);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void submit_form(ActionEvent actionEvent) {
        handleBookFlight();
    }
}