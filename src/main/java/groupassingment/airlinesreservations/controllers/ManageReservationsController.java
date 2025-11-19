package groupassingment.airlinesreservations.controllers;

import groupassingment.airlinesreservations.controllers.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

// Imports for Supabase JSON/HTTP handling
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.http.HttpResponse;

import java.net.URL;
import java.util.*;

public class ManageReservationsController implements Initializable {

    // --- FXML Injections (Sidebar) ---
    @FXML private Button btnDashboard;
    @FXML private Button btnReservation;
    @FXML private Button btnFeedback;
    @FXML private Button btnManageReservations;
    @FXML private Button btnSupport;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    // --- FXML Injections (Main Content) ---
    @FXML private Button btnAddFlight;
    @FXML private Button btnUpdateFlight;
    @FXML private Button btnDeleteFlight;
    @FXML private TextField txtSearch;

    @FXML private TableView<Flight> tblFlights;
    @FXML private TableColumn<Flight, String> colFlightName;
    @FXML private TableColumn<Flight, String> colFlightCode;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colDepartureTime;
    @FXML private TableColumn<Flight, String> colArrivalTime;
    @FXML private TableColumn<Flight, Integer> colAvailableSeats;

    // --- Data and State ---
    private ObservableList<Flight> masterFlightData = FXCollections.observableArrayList();
    private FilteredList<Flight> filteredFlightData;
    private final SupabaseService supabaseService = new SupabaseService();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // ** FIX 1: Mock Session Initialization REMOVED. **
        // Data loading is now delayed until initializeSessionData is called with the real token.

        // 1. Setup Table Columns
        setupTableColumns();

        // Initialize the filtered list (will be populated asynchronously later)
        filteredFlightData = new FilteredList<>(masterFlightData, p -> true);
        tblFlights.setItems(filteredFlightData);

        // 2. Setup Search Listener
        setupSearchListener();

        // 3. Setup CRUD Handlers
        btnAddFlight.setOnAction(event -> handleAddReservation());
        btnUpdateFlight.setOnAction(event -> handleUpdateReservation());
        btnDeleteFlight.setOnAction(event -> handleDeleteReservation());

        // 4. Setup Sidebar Navigation Handlers (Mock)
        setupNavigationHandlers();

        System.out.println("ManageReservationsController initialized, awaiting session data from Dashboard.");
    }

    /**
     * ** FIX 2: Implementation to receive token and trigger data load. **
     * Called by the DashboardController immediately after FXML loading.
     */
    public void initializeSessionData(String currentUserToken, String currentUserId, String currentUserEmail) {
        // Ensure the SessionManager holds the correct, real data
        SessionManager.setSessionData(currentUserToken, currentUserId, currentUserEmail);

        System.out.println("Session data received. Triggering reservation load...");
        loadReservationsFromSupabase();
    }

    /**
     * Defines how each column in the TableView should display its data.
     */
    private void setupTableColumns() {
        colFlightName.setCellValueFactory(new PropertyValueFactory<>("flightName")); // Maps to Customer Name
        colFlightCode.setCellValueFactory(new PropertyValueFactory<>("flightCode")); // Maps to Reservation Code
        colRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colDepartureTime.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        colArrivalTime.setCellValueFactory(new PropertyValueFactory<>("arrivalTime")); // Maps to Status
        colAvailableSeats.setCellValueFactory(new PropertyValueFactory<>("availableSeats")); // Maps to Seats Booked

        // Custom renderer for available seats (kept)
        colAvailableSeats.setCellFactory(column -> new TableCell<Flight, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item < 20) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (item < 50) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
    }

    /**
     * *** REAL DATA FETCHING ***
     * Loads reservation data from Supabase and maps it to the Flight model.
     */
    private void loadReservationsFromSupabase() {
        String authToken = SessionManager.getAuthToken();

        if (authToken == null || authToken.isEmpty()) {
            showAlert("Authentication Error", "Session token is missing or invalid. Please log in.");
            return;
        }

        System.out.println("Attempting to load reservation data from Supabase...");

        // Ensure fetchManagedReservations uses the authToken
        supabaseService.fetchManagedReservations(0, 1000, null, authToken)
                .thenAccept(this::processSupabaseResponse)
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Network Error", "Could not connect to Supabase: " + e.getMessage()));
                    return null;
                });
    }

    /**
     * Parses the nested JSON response from Supabase and maps it to the Flight model.
     */
    private void processSupabaseResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() != 200) {
                showAlert("Load Error", "Failed to load reservations. Status: " + response.statusCode() + ". Check Supabase RLS.");
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(response.body());
                List<Flight> mappedFlights = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    // --- EXTRACTING DATA FROM NESTED OBJECTS (REAL JOIN) ---
                    int reservationId = obj.optInt("id");
                    String reservationCode = obj.optString("reservation_code", "N/A");
                    int seatsBooked = obj.optInt("seats_booked", 1);
                    String status = obj.optString("status", "Pending");

                    // Joined Customer Data (from customers table)
                    String customerName = "N/A";
                    if (obj.has("customers") && !obj.isNull("customers")) {
                        customerName = obj.getJSONObject("customers").optString("name", "N/A");
                    }

                    // Joined Flight Data (from flights table)
                    String flightRoute = "N/A";
                    String departureTime = "N/A";
                    if (obj.has("flights") && !obj.isNull("flights")) {
                        JSONObject flightObj = obj.getJSONObject("flights");
                        flightRoute = flightObj.optString("route", "N/A");
                        departureTime = flightObj.optString("departure_time", "N/A");
                    }

                    // --- MAPPING TO FLIGHT OBJECT (to match FXML) ---
                    Flight flight = new Flight(
                            String.valueOf(reservationId),       // id (Internal: Reservation ID)
                            customerName,                       // flightName (Maps to Customer Name)
                            reservationCode,                    // flightCode (Maps to Reservation Code)
                            flightRoute,                        // route (Maps to Flight Route)
                            departureTime,                      // departureTime (Maps to Departure Time)
                            status,                             // arrivalTime (Maps to Status)
                            100,                                // totalSeats (Placeholder/Default)
                            seatsBooked                         // reservedSeats (Used for Seats Booked)
                    );
                    mappedFlights.add(flight);
                }

                masterFlightData.setAll(mappedFlights);
                System.out.println("Successfully loaded " + masterFlightData.size() + " reservations.");

            } catch (Exception e) {
                showAlert("Data Error", "Failed to parse Supabase data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Sets up the listener for the search text field to filter the data.
     */
    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredFlightData.setPredicate(flight -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Filter by Customer Name (flightName) or Reservation Code (flightCode)
                if (flight.getFlightName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (flight.getFlightCode().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
    }

    // --- CRUD Operations ---

    private void handleAddReservation() {
        System.out.println("Add New Reservation button clicked.");

        JSONObject newReservation = new JSONObject();
        // Placeholder data - replace with actual form input
        newReservation.put("customer_id", 1);
        newReservation.put("flight_id", 1);
        newReservation.put("reservation_code", "NEW" + UUID.randomUUID().toString().substring(0, 4));
        newReservation.put("seats_booked", 1);
        newReservation.put("status", "Awaiting Payment");

        String authToken = SessionManager.getAuthToken();

        supabaseService.addReservation(newReservation, authToken)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 201) {
                            showInfoAlert("Success", "Reservation added successfully.");
                            loadReservationsFromSupabase(); // Refresh data
                        } else {
                            showAlert("Error", "Failed to add reservation. Status: " + response.statusCode() + " | Response: " + response.body());
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Network Error", "Failed to add reservation: " + e.getMessage()));
                    return null;
                });
    }

    private void handleUpdateReservation() {
        Flight selectedFlight = tblFlights.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a reservation to update.");
            return;
        }

        // Example: Change status
        String newStatus = "CONFIRMED";

        JSONObject updateData = new JSONObject();
        updateData.put("status", newStatus);

        String authToken = SessionManager.getAuthToken();
        int reservationId = Integer.parseInt(selectedFlight.getId());

        supabaseService.updateReservation(reservationId, updateData, authToken)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 204) {
                            selectedFlight.setArrivalTime(newStatus); // Update local model (using arrivalTime for Status)
                            tblFlights.refresh();
                            showInfoAlert("Success", "Reservation " + selectedFlight.getFlightCode() + " status updated to " + newStatus + ".");
                        } else {
                            showAlert("Error", "Failed to update reservation. Status: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Network Error", "Failed to update reservation: " + e.getMessage()));
                    return null;
                });
    }

    private void handleDeleteReservation() {
        Flight selectedFlight = tblFlights.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a reservation to delete.");
            return;
        }

        ButtonType confirmButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete reservation " + selectedFlight.getFlightCode() + "?",
                confirmButton, cancelButton);
        confirm.setTitle("Confirm Deletion");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == confirmButton) {

            String authToken = SessionManager.getAuthToken();
            int reservationId = Integer.parseInt(selectedFlight.getId());

            supabaseService.deleteReservation(reservationId, authToken)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 204) {
                                masterFlightData.remove(selectedFlight);
                                showInfoAlert("Success", "Reservation " + selectedFlight.getFlightCode() + " deleted successfully.");
                            } else {
                                showAlert("Error", "Failed to delete reservation. Status: " + response.statusCode());
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Network Error", "Failed to delete reservation: " + e.getMessage()));
                        return null;
                    });
        }
    }

    // --- Utility Methods ---

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupNavigationHandlers() {
        btnLogout.setOnAction(e -> {
            SessionManager.clearSessionData();
            showInfoAlert("Logout", "You have been successfully logged out and session data cleared.");
            // Implement scene change back to Login
        });

        btnDashboard.setOnAction(e -> showInfoAlert("Navigation", "Navigating to Dashboard..."));
        btnReservation.setOnAction(e -> showInfoAlert("Navigation", "Navigating to Reservation page..."));
        // ... (other navigation handlers)
    }


    // --- Flight Data Model (UNCHANGED TO MATCH FXML) ---
    public static class Flight {
        private final SimpleStringProperty id;
        private final SimpleStringProperty flightName;
        private final SimpleStringProperty flightCode;
        private final SimpleStringProperty route;
        private final SimpleStringProperty departureTime;
        private final SimpleStringProperty arrivalTime;
        private int totalSeats;
        private int reservedSeats;
        private final SimpleIntegerProperty availableSeats;

        public Flight(String id, String flightName, String flightCode, String route, String departureTime, String arrivalTime, int totalSeats, int reservedSeats) {
            this.id = new SimpleStringProperty(id);
            this.flightName = new SimpleStringProperty(flightName);
            this.flightCode = new SimpleStringProperty(flightCode);
            this.route = new SimpleStringProperty(route);
            this.departureTime = new SimpleStringProperty(departureTime);
            this.arrivalTime = new SimpleStringProperty(arrivalTime);
            this.totalSeats = totalSeats;
            this.reservedSeats = reservedSeats;
            this.availableSeats = new SimpleIntegerProperty(calculateAvailableSeats());
        }

        private int calculateAvailableSeats() {
            return Math.max(0, totalSeats - reservedSeats);
        }

        // Getters for TableView properties
        public String getId() { return id.get(); }
        public SimpleStringProperty idProperty() { return id; }

        public String getFlightName() { return flightName.get(); }
        public SimpleStringProperty flightNameProperty() { return flightName; }

        public String getFlightCode() { return flightCode.get(); }
        public SimpleStringProperty flightCodeProperty() { return flightCode; }

        public String getRoute() { return route.get(); }
        public SimpleStringProperty routeProperty() { return route; }

        public String getDepartureTime() { return departureTime.get(); }
        public SimpleStringProperty departureTimeProperty() { return departureTime; }

        public String getArrivalTime() { return arrivalTime.get(); }
        public SimpleStringProperty arrivalTimeProperty() { return arrivalTime; }

        public int getAvailableSeats() { return availableSeats.get(); }
        public SimpleIntegerProperty availableSeatsProperty() { return availableSeats; }

        // Standard getters/setters for internal data
        public int getTotalSeats() { return totalSeats; }
        public void setTotalSeats(int totalSeats) {
            this.totalSeats = totalSeats;
            this.availableSeats.set(calculateAvailableSeats());
        }

        public int getReservedSeats() { return reservedSeats; }
        public void setReservedSeats(int reservedSeats) {
            this.reservedSeats = reservedSeats;
            this.availableSeats.set(calculateAvailableSeats());
        }

        // Setters used for UPDATES
        public void setFlightName(String flightName) { this.flightName.set(flightName); }
        public void setRoute(String route) { this.route.set(route); }
        public void setArrivalTime(String arrivalTime) { this.arrivalTime.set(arrivalTime); }
    }
}