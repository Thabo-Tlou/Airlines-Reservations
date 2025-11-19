package groupassingment.airlinesreservations.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

public class ManageReservationsController {

    // --- FXML Injections (Main Content) ---
    @FXML private Button btnAddFlight;
    @FXML private Button btnUpdateFlight;
    @FXML private Button btnDeleteFlight;
    @FXML private TextField txtSearch;

    @FXML private TableView<ReservationItem> tblFlights;
    @FXML private TableColumn<ReservationItem, String> colFlightName;    // Maps to Reservation Code
    @FXML private TableColumn<ReservationItem, String> colFlightCode;    // Maps to Flight Date
    @FXML private TableColumn<ReservationItem, String> colRoute;         // Maps to Origin -> Destination
    @FXML private TableColumn<ReservationItem, String> colDepartureTime; // Maps to Seat Class
    @FXML private TableColumn<ReservationItem, String> colArrivalTime;   // Maps to Total Price
    @FXML private TableColumn<ReservationItem, String> colAvailableSeats;// Maps to Status (e.g., Confirmed)

    // --- Data and State ---
    private ObservableList<ReservationItem> masterReservationData = FXCollections.observableArrayList();
    private FilteredList<ReservationItem> filteredReservationData;
    private final SupabaseService supabaseService = new SupabaseService();

    private String userAuthToken;
    private String userEmail;
    private int currentCustomerId = -1;

    /**
     * Data Model to hold reservation details for the TableView.
     */
    public static class ReservationItem {
        private final SimpleStringProperty reservationCode;
        private final SimpleStringProperty flightDate;
        private final SimpleStringProperty route; // This is the property
        private final SimpleStringProperty seatClass;
        private final SimpleStringProperty price;
        private final SimpleStringProperty status;

        public ReservationItem(String resCode, String date, String route, String seat, String price, String status) {
            this.reservationCode = new SimpleStringProperty(resCode);
            this.flightDate = new SimpleStringProperty(date);
            this.route = new SimpleStringProperty(route);
            this.seatClass = new SimpleStringProperty(seat);
            this.price = new SimpleStringProperty(price);
            this.status = new SimpleStringProperty(status);
        }

        // --- Required Getters for Search Logic ---
        public String getReservationCode() { return reservationCode.get(); }

        // 💡 FIX: This method was missing and caused the error 💡
        public String getRoute() {
            return route.get();
        }

        // --- Mapped Getters for FXML Table Columns ---
        public String getColFlightName() { return getReservationCode(); }
        public String getColFlightCode() { return flightDate.get(); }
        public String getColRoute() { return getRoute(); }
        public String getColDepartureTime() { return seatClass.get(); }
        public String getColArrivalTime() { return price.get(); }
        public String getColAvailableSeats() { return status.get(); }
    }

    @FXML
    public void initialize() {
        // 1. Initialize Table Columns
        colFlightName.setCellValueFactory(new PropertyValueFactory<>("colFlightName"));
        colFlightCode.setCellValueFactory(new PropertyValueFactory<>("colFlightCode"));
        colRoute.setCellValueFactory(new PropertyValueFactory<>("colRoute"));
        colDepartureTime.setCellValueFactory(new PropertyValueFactory<>("colDepartureTime"));
        colArrivalTime.setCellValueFactory(new PropertyValueFactory<>("colArrivalTime"));
        colAvailableSeats.setCellValueFactory(new PropertyValueFactory<>("colAvailableSeats"));

        // Update column headers for clarity
        colFlightName.setText("Reservation Code");
        colFlightCode.setText("Flight Date");
        colRoute.setText("Route");
        colDepartureTime.setText("Seat Class");
        colArrivalTime.setText("Total Price");
        colAvailableSeats.setText("Status");

        // 2. Setup Search Listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredReservationData != null) {
                filteredReservationData.setPredicate(reservation -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();

                    // These two methods are now resolved due to the fix above
                    return reservation.getReservationCode().toLowerCase().contains(lowerCaseFilter) ||
                            reservation.getRoute().toLowerCase().contains(lowerCaseFilter);
                });
            }
        });

        // 3. Setup UI state
        btnAddFlight.setDisable(true);
        btnUpdateFlight.setDisable(false);
        btnDeleteFlight.setDisable(false);

        tblFlights.setPlaceholder(new Label("Loading your reservations..."));
    }

    /**
     * Called by the DashboardController to pass session data.
     */
    public void initializeSessionData(String userAuthToken, String userId, String userEmail) {
        this.userAuthToken = userAuthToken;
        this.userEmail = userEmail;
        fetchCustomerIdAndReservations();
    }

    private void fetchCustomerIdAndReservations() {
        if (userEmail == null || userAuthToken == null) {
            Platform.runLater(() -> tblFlights.setPlaceholder(new Label("Error: User session data is missing.")));
            return;
        }

        // 1. Get Customer ID using the email
        supabaseService.getCustomerByEmail(userEmail, userAuthToken)
                .thenAccept(customerResponse -> {
                    if (customerResponse.statusCode() == 200) {
                        try {
                            JSONArray customers = new JSONArray(customerResponse.body());
                            if (customers.length() > 0) {
                                this.currentCustomerId = customers.getJSONObject(0).getInt("customer_id");
                                fetchUserReservations();
                            } else {
                                Platform.runLater(() -> tblFlights.setPlaceholder(new Label("Customer profile not found. You may need to complete a full booking first.")));
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to parse customer ID: " + e.getMessage()));
                        }
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "API Error (Step 1)",
                                "Error fetching customer profile: Status " + customerResponse.statusCode() + "."));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to service to find customer profile."));
                    return null;
                });
    }

    private void fetchUserReservations() {
        if (currentCustomerId == -1) return;

        // 2. Fetch Reservations using the Customer ID
        supabaseService.getReservationsByCustomerId((long) currentCustomerId, userAuthToken)
                .thenAccept(reservationResponse -> {
                    Platform.runLater(() -> {
                        if (reservationResponse.statusCode() == 200) {
                            try {
                                JSONArray reservations = new JSONArray(reservationResponse.body());
                                populateTable(reservations);
                            } catch (Exception e) {
                                showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to parse reservation data: " + e.getMessage());
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "API Error (Step 2)",
                                    "Error loading reservations: Status " + reservationResponse.statusCode() + ".");
                            tblFlights.setPlaceholder(new Label("Failed to load your reservations."));
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Network Error", "Network error fetching reservations."));
                    return null;
                });
    }

    private void populateTable(JSONArray reservations) {
        masterReservationData.clear();
        if (reservations.length() == 0) {
            tblFlights.setPlaceholder(new Label("You have no active reservations."));
            return;
        }

        for (int i = 0; i < reservations.length(); i++) {
            JSONObject res = reservations.getJSONObject(i);

            String route = res.optString("travel_from", "N/A") + " → " + res.optString("travel_to", "N/A");
            String price = String.format("M%.2f", res.optDouble("total_price", 0.0));

            masterReservationData.add(new ReservationItem(
                    res.optString("reservation_code", "N/A"),
                    res.optString("flight_date", "N/A"),
                    route,
                    res.optString("seat_class", "N/A"),
                    price,
                    res.optString("status", "N/A")
            ));
        }

        filteredReservationData = new FilteredList<>(masterReservationData, p -> true);
        tblFlights.setItems(filteredReservationData);
    }

    // --- Management Actions ---

    @FXML
    private void handleUpdateFlight() {
        ReservationItem selectedItem = tblFlights.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to update.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Update Reservation", "Update functionality for reservation " + selectedItem.getReservationCode() + " needs to be implemented.");
    }

    @FXML
    private void handleDeleteFlight() {
        ReservationItem selectedItem = tblFlights.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete reservation " + selectedItem.getReservationCode() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            showAlert(Alert.AlertType.INFORMATION, "Delete Status", "Deletion of reservation " + selectedItem.getReservationCode() + " needs to be implemented.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}