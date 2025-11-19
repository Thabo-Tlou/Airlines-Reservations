package groupassingment.airlinesreservations.controllers;

import groupassingment.airlinesreservations.controllers.SceneManager;
import groupassingment.airlinesreservations.controllers.SessionManager;
import groupassingment.airlinesreservations.controllers.SupabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ReservationFormController {

    // Passenger Information Fields
    @FXML private ComboBox<String> combo_adults;
    @FXML private ComboBox<String> combo_children;
    @FXML private ComboBox<String> combo_infants;
    @FXML private TextField txt_entername;
    @FXML private TextField txt_idnumber;
    @FXML private TextField txt_enteraddress;
    @FXML private TextField txt_enteremail;
    @FXML private TextField txt_enterphone;
    @FXML private ComboBox<String> combo_categoryfare;

    // Flight Details Fields
    @FXML private ComboBox<String> combo_triptype;
    @FXML private TextField txt_enterflightcode;
    @FXML private DatePicker dp_traveldate;
    @FXML private DatePicker dp_returndate;
    @FXML private ComboBox<String> combo_travelfrom;
    @FXML private ComboBox<String> combo_travelto;
    @FXML private ComboBox<String> combo_seatclass;
    @FXML private ComboBox<String> combo_seatpref;
    @FXML private TextField txt_totalfare;

    // NEW: Available Flights ComboBox
    @FXML private ComboBox<String> combo_available_flights;

    // Payment Information Fields
    @FXML private ComboBox<String> combo_paymentmethod;
    @FXML private TextField txt_cardnumber;
    @FXML private TextField txt_expirydate;
    @FXML private TextField txt_cvv;
    @FXML private CheckBox chk_terms;
    @FXML private CheckBox chk_newsletter;

    // Action Buttons and Status
    @FXML private Button btn_submit;
    @FXML private Button btn_clear;
    @FXML private Button btn_print;
    @FXML private Label lbl_status;
    @FXML private ProgressBar progress_bar;

    // Navigation Buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnReservation;
    @FXML private Button btnManageReservations;
    @FXML private Button btnFeedback;
    @FXML private Button btnSupport;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    private SupabaseService supabaseService;
    private String userAuthToken;
    private String userId;
    private String userEmail;
    private Long customerId;

    // Store loaded data
    private ObservableList<String> airportsList = FXCollections.observableArrayList();
    private ObservableList<String> fareCategoriesList = FXCollections.observableArrayList();
    private ObservableList<String> availableFlightsList = FXCollections.observableArrayList();

    // Store flight data for quick lookup
    private JSONArray allFlightsData = new JSONArray();

    // Initialize method called by FXML loader
    public void initialize() {
        supabaseService = new SupabaseService();
        setupFormDefaults();
        setupEventHandlers();
        setupNavigationHandlers();

        // Load dynamic data from Supabase
        loadAirports();
        loadFareCategories();
        loadAvailableFlights();

        // Initially disable print button until reservation is confirmed
        if(btn_print != null) btn_print.setDisable(true);
        debugSessionInfo();
    }

    private void setupNavigationHandlers() {
        if (btnDashboard != null) btnDashboard.setOnAction(event -> navigateToDashboard());
        if (btnManageReservations != null) btnManageReservations.setOnAction(event -> navigateToManageReservations());
        if (btnFeedback != null) btnFeedback.setOnAction(event -> navigateToFeedback());
        if (btnSupport != null) btnSupport.setOnAction(event -> navigateToSupport());
        if (btnSettings != null) btnSettings.setOnAction(event -> navigateToSettings());
        if (btnLogout != null) btnLogout.setOnAction(event -> handleLogout());
    }

    private void debugSessionInfo() {
        System.out.println("=== DEBUG: INITIALIZATION ===");
        System.out.println("SessionManager Auth Token: " + (SessionManager.getAuthToken() != null ? "Present" : "NULL"));
        System.out.println("SessionManager Customer ID: " + SessionManager.getCustomerID());
        System.out.println("SessionManager User Email: " + SessionManager.getUserEmail());
        System.out.println("=== DEBUG END ===");
    }

    // Method called from DashboardController to pass session data
    public void initializeSessionData(String authToken, String userId, String userEmail) {
        this.userAuthToken = authToken;
        this.userId = userId;
        this.userEmail = userEmail;

        System.out.println("=== DEBUG: SESSION DATA RECEIVED ===");
        System.out.println("User Email: " + userEmail);

        // Pre-fill customer email and load customer data
        prefillCustomerData();
    }

    private void setupFormDefaults() {
        ObservableList<String> passengerCounts = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5", "6");
        combo_adults.setItems(passengerCounts);
        combo_children.setItems(passengerCounts);
        combo_infants.setItems(passengerCounts);
        combo_adults.setValue("1");
        combo_children.setValue("0");
        combo_infants.setValue("0");

        ObservableList<String> tripTypes = FXCollections.observableArrayList("One-way", "Round-trip");
        combo_triptype.setItems(tripTypes);
        combo_triptype.setValue("One-way");

        ObservableList<String> seatClasses = FXCollections.observableArrayList("Economic", "Business");
        combo_seatclass.setItems(seatClasses);
        combo_seatclass.setValue("Economic");

        ObservableList<String> seatPrefs = FXCollections.observableArrayList("Window", "Aisle", "Middle");
        combo_seatpref.setItems(seatPrefs);
        combo_seatpref.setValue("Window");

        ObservableList<String> paymentMethods = FXCollections.observableArrayList(
                "Credit Card", "Debit Card", "Bank Transfer"
        );
        combo_paymentmethod.setItems(paymentMethods);
        combo_paymentmethod.setValue("Credit Card");

        dp_traveldate.setValue(LocalDate.now());

        generateFlightCode();

        combo_travelfrom.setItems(airportsList);
        combo_travelto.setItems(airportsList);
        combo_categoryfare.setItems(fareCategoriesList);

        // NEW: Set up available flights combo box
        combo_available_flights.setItems(availableFlightsList);
    }

    private void setupEventHandlers() {
        // Add listeners for dynamic fare calculation
        combo_seatclass.valueProperty().addListener((obs, oldVal, newVal) -> calculateTotalFare());
        combo_categoryfare.valueProperty().addListener((obs, oldVal, newVal) -> calculateTotalFare());
        combo_adults.valueProperty().addListener((obs, oldVal, newVal) -> calculateTotalFare());
        combo_children.valueProperty().addListener((obs, oldVal, newVal) -> calculateTotalFare());
        combo_infants.valueProperty().addListener((obs, oldVal, newVal) -> calculateTotalFare());

        // Handle return date visibility based on trip type
        combo_triptype.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isRoundTrip = "Round-trip".equals(newVal);
            if (dp_returndate != null) {
                dp_returndate.setVisible(isRoundTrip);
                dp_returndate.setManaged(isRoundTrip);
            }
        });

        // NEW: Handle flight selection from available flights combo box
        combo_available_flights.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                handleFlightSelection(newVal);
            }
        });
    }

    // NEW: Load available flights from the flights table
    private void loadAvailableFlights() {
        updateStatus("Loading available flights...", true);

        supabaseService.getFlights()
                .thenAccept(flightsResponse -> {
                    if (flightsResponse.statusCode() == 200) {
                        try {
                            JSONArray flightsArray = new JSONArray(flightsResponse.body());
                            allFlightsData = flightsArray; // Store for later lookup

                            Platform.runLater(() -> {
                                availableFlightsList.clear();

                                for (int i = 0; i < flightsArray.length(); i++) {
                                    JSONObject flight = flightsArray.getJSONObject(i);

                                    String flightCode = flight.optString("flight_code", "Unknown");
                                    String departureCity = flight.optString("departure_city", "Unknown");
                                    String destinationCity = flight.optString("destination_city", "Unknown");
                                    String departureDate = flight.optString("departure_date", "");
                                    String departureTime = flight.optString("departure_time", "");
                                    String arrivalDate = flight.optString("arrival_date", "");
                                    String arrivalTime = flight.optString("arrival_time", "");
                                    int availableEconomicSeats = flight.optInt("available_economic_seats", 0);
                                    int availableBusinessSeats = flight.optInt("available_business_seats", 0);

                                    // Format: Flight Code | Route | Date | Time | Available Seats
                                    String displayText = String.format("%s | %s → %s | %s %s | Eco: %d Bus: %d",
                                            flightCode,
                                            departureCity,
                                            destinationCity,
                                            departureDate,
                                            departureTime,
                                            availableEconomicSeats,
                                            availableBusinessSeats);

                                    availableFlightsList.add(displayText);
                                }

                                combo_available_flights.setItems(availableFlightsList);
                                updateStatus("Available flights loaded", false);

                                if (availableFlightsList.isEmpty()) {
                                    showAlert("Info", "No available flights found in the system.");
                                }
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                updateStatus("Error loading flights", false);
                                showAlert("Error", "Failed to load available flights: " + e.getMessage());
                            });
                        }
                    } else {
                        Platform.runLater(() -> {
                            updateStatus("Failed to load flights", false);
                            showAlert("Error", "Failed to load available flights from server.");
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        updateStatus("Network error loading flights", false);
                        showAlert("Error", "Network error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    // NEW: Handle when user selects a flight from the available flights combo box
    private void handleFlightSelection(String selectedFlightDisplay) {
        try {
            // Extract flight code from the display text (format: "FLIGHT123 | CityA → CityB | ...")
            String flightCode = selectedFlightDisplay.split("\\|")[0].trim();

            // Find the flight data from our stored array
            for (int i = 0; i < allFlightsData.length(); i++) {
                JSONObject flight = allFlightsData.getJSONObject(i);
                String currentFlightCode = flight.optString("flight_code", "");

                if (flightCode.equals(currentFlightCode)) {
                    // Extract and set the flight details
                    String departureCity = flight.optString("departure_city", "");
                    String destinationCity = flight.optString("destination_city", "");
                    String departureDate = flight.optString("departure_date", "");
                    String arrivalDate = flight.optString("arrival_date", "");
                    String departureTime = flight.optString("departure_time", "");
                    String arrivalTime = flight.optString("arrival_time", "");

                    // Update the form fields
                    Platform.runLater(() -> {
                        // Set departure and destination cities
                        combo_travelfrom.setValue(departureCity);
                        combo_travelto.setValue(destinationCity);

                        // Set dates
                        if (!departureDate.isEmpty()) {
                            try {
                                dp_traveldate.setValue(LocalDate.parse(departureDate));
                            } catch (Exception e) {
                                System.err.println("Error parsing departure date: " + e.getMessage());
                            }
                        }

                        if (!arrivalDate.isEmpty() && dp_returndate != null) {
                            try {
                                dp_returndate.setValue(LocalDate.parse(arrivalDate));
                            } catch (Exception e) {
                                System.err.println("Error parsing arrival date: " + e.getMessage());
                            }
                        }

                        // Set flight code
                        txt_enterflightcode.setText(flightCode);

                        // Update status
                        updateStatus("Flight " + flightCode + " selected", false);
                    });

                    break;
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                showAlert("Error", "Failed to process selected flight: " + e.getMessage());
            });
        }
    }

    // ==========================================
    // MAIN SUBMISSION LOGIC (UPDATED)
    // ==========================================

    @FXML
    private void submit_form() {
        System.out.println("=== SUBMIT FORM TRIGGERED ===");

        if (!validateForm()) {
            return;
        }

        // NEW: Check if a flight is selected from available flights
        if (combo_available_flights.getValue() == null || combo_available_flights.getValue().isEmpty()) {
            showAlert("Validation Error", "Please select a flight from the available flights list.");
            return;
        }

        // Debug authentication and customer info
        debugAuthAndCustomerInfo();

        if (customerId == null) {
            System.out.println("Customer ID is null, creating customer first...");
            createCustomerAndThenReservation();
        } else {
            System.out.println("Customer ID exists: " + customerId + ", processing reservation...");
            processReservationWithSelectedFlight();
        }
    }

    // NEW: Debug authentication and customer info
    private void debugAuthAndCustomerInfo() {
        System.out.println("=== AUTH & CUSTOMER DEBUG ===");
        System.out.println("Customer ID: " + customerId);
        System.out.println("User Email: " + userEmail);
        System.out.println("Auth Token: " + (userAuthToken != null ? "Present (" + userAuthToken.length() + " chars)" : "NULL"));

        if (userAuthToken != null) {
            try {
                String[] parts = userAuthToken.split("\\.");
                if (parts.length == 3) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                    System.out.println("JWT Payload: " + payload);
                }
            } catch (Exception e) {
                System.err.println("Error decoding JWT: " + e.getMessage());
            }
        }
        System.out.println("=== END DEBUG ===");
    }

    // NEW: Process reservation using the selected flight from combo box
    private void processReservationWithSelectedFlight() {
        updateStatus("Processing reservation with selected flight...", true);

        String selectedFlightDisplay = combo_available_flights.getValue();
        String flightCode = selectedFlightDisplay.split("\\|")[0].trim();

        // Find the flight ID from our stored data
        Long flightId = null;
        String seatClass = combo_seatclass.getValue();
        String seatPreference = combo_seatpref.getValue();

        try {
            for (int i = 0; i < allFlightsData.length(); i++) {
                JSONObject flight = allFlightsData.getJSONObject(i);
                if (flightCode.equals(flight.optString("flight_code", ""))) {
                    flightId = flight.getLong("flight_id");
                    break;
                }
            }

            if (flightId != null) {
                System.out.println("Found flight ID: " + flightId + " for flight: " + flightCode);
                checkAvailableSeats(flightId, flightCode, seatClass, seatPreference);
            } else {
                Platform.runLater(() -> {
                    updateStatus("Flight not found in system", false);
                    showAlert("Error", "Selected flight not found in system. Please select another flight.");
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                updateStatus("Error processing flight selection", false);
                showAlert("Error", "Error processing selected flight: " + e.getMessage());
            });
        }
    }

    // ==========================================
    // ENHANCED SEAT AVAILABILITY CHECKING (UPDATED)
    // ==========================================

    private void checkAvailableSeats(Long flightId, String flightCode, String seatClass, String seatPreference) {
        updateStatus("Checking available " + seatClass + " class " + seatPreference + " seats...", true);

        System.out.println("=== SEAT CHECK DEBUG ===");
        System.out.println("Flight ID: " + flightId);
        System.out.println("Seat Class: " + seatClass);
        System.out.println("Seat Preference: " + seatPreference);
        System.out.println("Using Database Function: get_available_seats");

        supabaseService.getAvailableSeatsByFlight(flightId)
                .thenAccept(seatsResponse -> {
                    System.out.println("Database function response - Status: " + seatsResponse.statusCode());
                    System.out.println("Database function response - Body: " + seatsResponse.body());

                    if (seatsResponse.statusCode() == 200) {
                        try {
                            JSONArray seatsArray = new JSONArray(seatsResponse.body());
                            Long availableSeatId = null;
                            String seatNumber = "";
                            String matchedSeatPosition = "";

                            System.out.println("Database function returned " + seatsArray.length() + " actually available seats");

                            // Debug: Log all available seats
                            for (int i = 0; i < seatsArray.length(); i++) {
                                JSONObject seat = seatsArray.getJSONObject(i);
                                System.out.println("Available Seat: " +
                                        seat.optString("seat_number") + " - " +
                                        seat.optString("seat_class") + " - " +
                                        seat.optString("seat_position"));
                            }

                            // Strategy: Find best matching seat
                            for (int i = 0; i < seatsArray.length(); i++) {
                                JSONObject seat = seatsArray.getJSONObject(i);
                                String seatClassDb = seat.optString("seat_class", "");
                                String currentSeatPosition = seat.optString("seat_position", "");

                                if (seatClass.equalsIgnoreCase(seatClassDb)) {
                                    // Priority 1: Exact preference match
                                    if (seatPreference.equalsIgnoreCase(currentSeatPosition)) {
                                        availableSeatId = seat.getLong("seat_id");
                                        seatNumber = seat.optString("seat_number", "");
                                        matchedSeatPosition = currentSeatPosition;
                                        System.out.println("Found exact match: " + seatNumber);
                                        break; // Stop at first exact match
                                    }
                                    // Priority 2: First available in class (fallback)
                                    else if (availableSeatId == null) {
                                        availableSeatId = seat.getLong("seat_id");
                                        seatNumber = seat.optString("seat_number", "");
                                        matchedSeatPosition = currentSeatPosition;
                                        System.out.println("Found fallback seat: " + seatNumber);
                                    }
                                }
                            }

                            if (availableSeatId != null) {
                                System.out.println("CONFIRMED: Seat " + seatNumber +
                                        " (" + matchedSeatPosition + ") - ID: " + availableSeatId);
                                createConfirmedReservation(flightId, flightCode, availableSeatId, seatNumber);
                            } else {
                                System.out.println("NO SEATS: Adding to waiting list");
                                createWaitingListReservation(flightId, flightCode);
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing seat data: " + e.getMessage());
                            Platform.runLater(() -> {
                                updateStatus("Error checking seats", false);
                                showAlert("Error", "Failed to process seat availability: " + e.getMessage());
                            });
                        }
                    } else {
                        // Handle function call errors - try fallback method
                        Platform.runLater(() -> {
                            updateStatus("Failed to check seat availability", false);
                            System.err.println("Database function error: " + seatsResponse.body());

                            // Try fallback method
                            tryFallbackSeatCheck(flightId, flightCode, seatClass, seatPreference);
                        });
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception during seat check: " + ex.getMessage());
                    Platform.runLater(() -> {
                        updateStatus("Network error checking seats", false);
                        // Try fallback on network errors
                        tryFallbackSeatCheck(flightId, flightCode, seatClass, seatPreference);
                    });
                    return null;
                });
    }

    // Fallback method for error scenarios
    private void tryFallbackSeatCheck(Long flightId, String flightCode, String seatClass, String seatPreference) {
        System.out.println("Attempting fallback seat check...");

        supabaseService.getAvailableSeatsByFlightFallback(flightId)
                .thenAccept(fallbackResponse -> {
                    if (fallbackResponse.statusCode() == 200) {
                        try {
                            JSONArray seatsArray = new JSONArray(fallbackResponse.body());
                            Long availableSeatId = null;
                            String seatNumber = "";

                            System.out.println("Fallback found " + seatsArray.length() + " seats");

                            for (int i = 0; i < seatsArray.length(); i++) {
                                JSONObject seat = seatsArray.getJSONObject(i);
                                String seatClassDb = seat.optString("seat_class", "");
                                String seatPosition = seat.optString("seat_position", "");
                                boolean isAvailable = seat.optBoolean("is_available", false);

                                if (seatClass.equals(seatClassDb) && isAvailable) {
                                    if (seatPreference.equals(seatPosition) || availableSeatId == null) {
                                        availableSeatId = seat.getLong("seat_id");
                                        seatNumber = seat.optString("seat_number", "");
                                        if (seatPreference.equals(seatPosition)) {
                                            break;
                                        }
                                    }
                                }
                            }

                            if (availableSeatId != null) {
                                System.out.println("Fallback found available seat: " + seatNumber + " (ID: " + availableSeatId + ")");
                                createConfirmedReservation(flightId, flightCode, availableSeatId, seatNumber);
                            } else {
                                System.out.println("Fallback: No seats available - adding to waiting list");
                                createWaitingListReservation(flightId, flightCode);
                            }
                        } catch (Exception e) {
                            System.err.println("Error in fallback seat check: " + e.getMessage());
                            Platform.runLater(() -> {
                                updateStatus("Error in fallback seat check", false);
                                createWaitingListReservation(flightId, flightCode); // Default to waiting list
                            });
                        }
                    } else {
                        Platform.runLater(() -> {
                            updateStatus("Both seat checks failed", false);
                            showAlert("Error", "Unable to check seat availability. Please try again later.");
                        });
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception during fallback seat check: " + ex.getMessage());
                    Platform.runLater(() -> {
                        updateStatus("All seat checks failed", false);
                        createWaitingListReservation(flightId, flightCode); // Default to waiting list
                    });
                    return null;
                });
    }

    // ==========================================
    // RESERVATION CREATION METHODS
    // ==========================================

    private void createConfirmedReservation(Long flightId, String flightCode, Long seatId, String seatNumber) {
        updateStatus("Reserving seat " + seatNumber + "...", true);

        try {
            JSONObject reservationData = createReservationData(flightId, seatId);
            // Override the waiting list settings for confirmed reservations
            reservationData.put("reservation_status", "Confirmed");
            reservationData.put("is_confirmed", true);
            reservationData.put("confirmed_at", "now()");
            reservationData.remove("waiting_list_position"); // Remove waiting list position for confirmed

            System.out.println("=== CONFIRMED RESERVATION DEBUG ===");
            System.out.println("Reservation Data: " + reservationData.toString(2));

            supabaseService.addReservation(reservationData, userAuthToken)
                    .thenAccept(response -> {
                        System.out.println("=== CONFIRMED RESERVATION RESPONSE ===");
                        System.out.println("Status: " + response.statusCode());
                        System.out.println("Body: " + response.body());
                        System.out.println("Headers: " + response.headers().map());

                        Platform.runLater(() -> {
                            if (response.statusCode() == 201) {
                                markSeatAsBooked(seatId);
                                updateStatus("Reservation CONFIRMED! Seat: " + seatNumber, false);
                                showAlert("Reservation Confirmed",
                                        "🎉 Your flight has been confirmed!\n\n" +
                                                "Flight: " + flightCode + "\n" +
                                                "Seat: " + combo_seatclass.getValue() + " Class, " + seatNumber + "\n" +
                                                "Status: CONFIRMED");

                                generateTicket(flightCode, seatNumber, "Confirmed");
                                if(btn_print != null) btn_print.setDisable(false);
                            } else {
                                updateStatus("Reservation failed: " + response.statusCode(), false);
                                showAlert("Error", "Failed to create reservation: " + response.body());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("Exception during confirmed reservation: " + ex.getMessage());
                        Platform.runLater(() -> {
                            updateStatus("Network error creating reservation", false);
                            showAlert("Error", "Network error: " + ex.getMessage());
                        });
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Exception in createConfirmedReservation: " + e.getMessage());
            Platform.runLater(() -> {
                updateStatus("Error creating reservation", false);
                showAlert("Error", "System error: " + e.getMessage());
            });
        }
    }

    // UPDATED: Create waiting list reservation with proper position calculation
    private void createWaitingListReservation(Long flightId, String flightCode) {
        updateStatus("No seats available - Creating waiting list reservation...", true);

        try {
            // Debug customer information
            System.out.println("=== WAITING LIST RESERVATION DEBUG ===");
            System.out.println("Customer ID: " + customerId);
            System.out.println("User Email: " + userEmail);
            System.out.println("Flight ID: " + flightId);
            System.out.println("Auth Token: " + (userAuthToken != null ? "Present" : "NULL"));

            // Get the next available waiting position first
            getNextWaitingPosition(flightId, combo_seatclass.getValue())
                    .thenAccept(nextPosition -> {
                        try {
                            // First create a reservation with "Waiting" status
                            JSONObject reservationData = createReservationData(flightId, null);
                            reservationData.put("reservation_status", "Waiting");
                            reservationData.put("is_confirmed", false);
                            reservationData.put("waiting_list_position", nextPosition);

                            System.out.println("Reservation Data: " + reservationData.toString(2));

                            supabaseService.addReservation(reservationData, userAuthToken)
                                    .thenAccept(reservationResponse -> {
                                        System.out.println("=== RESERVATION CREATION RESPONSE ===");
                                        System.out.println("Status Code: " + reservationResponse.statusCode());
                                        System.out.println("Response Body: " + reservationResponse.body());
                                        System.out.println("Headers: " + reservationResponse.headers().map());

                                        if (reservationResponse.statusCode() == 201) {
                                            try {
                                                JSONArray createdReservation = new JSONArray(reservationResponse.body());
                                                Long reservationId = createdReservation.getJSONObject(0).getLong("reservation_id");

                                                System.out.println("SUCCESS: Created reservation ID: " + reservationId);

                                                // Now add to waiting_list with the reservation_id
                                                addToWaitingList(reservationId, flightId, flightCode, nextPosition);

                                            } catch (Exception e) {
                                                System.err.println("ERROR: Failed to parse reservation response: " + e.getMessage());
                                                e.printStackTrace();
                                                Platform.runLater(() -> {
                                                    updateStatus("Failed to parse reservation response", false);
                                                    showAlert("Error", "Failed to create waiting list entry: " + e.getMessage());
                                                });
                                            }
                                        } else {
                                            System.err.println("ERROR: Failed to create reservation. Status: " + reservationResponse.statusCode());
                                            System.err.println("Response Body: " + reservationResponse.body());
                                            Platform.runLater(() -> {
                                                updateStatus("Failed to create reservation: " + reservationResponse.statusCode(), false);
                                                showAlert("Error", "Failed to create reservation: " + reservationResponse.body());
                                            });
                                        }
                                    })
                                    .exceptionally(ex -> {
                                        System.err.println("EXCEPTION: During reservation creation: " + ex.getMessage());
                                        ex.printStackTrace();
                                        Platform.runLater(() -> {
                                            updateStatus("Network error creating reservation", false);
                                            showAlert("Error", "Network error: " + ex.getMessage());
                                        });
                                        return null;
                                    });
                        } catch (Exception e) {
                            System.err.println("EXCEPTION: In reservation creation: " + e.getMessage());
                            Platform.runLater(() -> {
                                updateStatus("Error creating reservation", false);
                                showAlert("Error", "System error: " + e.getMessage());
                            });
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("EXCEPTION: Failed to get waiting position: " + ex.getMessage());
                        Platform.runLater(() -> {
                            updateStatus("Error getting waiting position", false);
                            showAlert("Error", "Failed to calculate waiting position: " + ex.getMessage());
                        });
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("EXCEPTION: In createWaitingListReservation: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                updateStatus("Error creating waiting list reservation", false);
                showAlert("Error", "System error: " + e.getMessage());
            });
        }
    }

    // UPDATED: Add to waiting list with proper position
    private void addToWaitingList(Long reservationId, Long flightId, String flightCode, int position) {
        updateStatus("Adding to waiting list...", true);

        try {
            JSONObject waitingListData = new JSONObject();
            waitingListData.put("reservation_id", reservationId);
            waitingListData.put("flight_id", flightId);
            waitingListData.put("seat_class", combo_seatclass.getValue());
            waitingListData.put("seat_preference", combo_seatpref.getValue());
            waitingListData.put("position", position);

            System.out.println("=== WAITING LIST INSERT DEBUG ===");
            System.out.println("Waiting List Data: " + waitingListData.toString(2));

            supabaseService.addToWaitingList(waitingListData, userAuthToken)
                    .thenAccept(waitingResponse -> {
                        System.out.println("=== WAITING LIST RESPONSE ===");
                        System.out.println("Status: " + waitingResponse.statusCode());
                        System.out.println("Body: " + waitingResponse.body());

                        Platform.runLater(() -> {
                            if (waitingResponse.statusCode() == 201) {
                                updateStatus("Added to WAITING LIST - Position: " + position, false);
                                showAlert("Waiting List",
                                        "⏳ No seats available currently.\n\n" +
                                                "You have been added to the waiting list for flight " + flightCode + ".\n" +
                                                "Position: " + position + " in " + combo_seatclass.getValue() + " class\n" +
                                                "We will notify you if a seat becomes available.");
                            } else {
                                updateStatus("Failed to join waiting list: " + waitingResponse.statusCode(), false);
                                showAlert("Error", "Failed to join waiting list: " + waitingResponse.body());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("Exception during waiting list insertion: " + ex.getMessage());
                        Platform.runLater(() -> {
                            updateStatus("Error adding to waiting list", false);
                            showAlert("Error", "Network error: " + ex.getMessage());
                        });
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Exception in addToWaitingList: " + e.getMessage());
            Platform.runLater(() -> {
                updateStatus("Error adding to waiting list", false);
                showAlert("Error", "System error: " + e.getMessage());
            });
        }
    }

    // UPDATED: Get next waiting position - query the database for max position
    private CompletableFuture<Integer> getNextWaitingPosition(Long flightId, String seatClass) {
        System.out.println("Getting next waiting position for flight " + flightId + ", class " + seatClass);

        return supabaseService.getMaxWaitingPosition(flightId, seatClass, userAuthToken)
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            JSONArray result = new JSONArray(response.body());
                            if (result.length() > 0) {
                                int maxPosition = result.getJSONObject(0).optInt("max_position", 0);
                                int nextPosition = maxPosition + 1;
                                System.out.println("Current max position: " + maxPosition + ", next position: " + nextPosition);
                                return nextPosition;
                            }
                        }
                        System.out.println("No existing waiting positions, starting at position 1");
                        return 1;
                    } catch (Exception e) {
                        System.err.println("Error parsing waiting position: " + e.getMessage());
                        return 1; // Fallback
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error getting waiting position: " + ex.getMessage());
                    return 1; // Fallback
                });
    }

    // CORRECTED: Updated to match your actual table columns and remove generated columns
    private JSONObject createReservationData(Long flightId, Long seatId) {
        String fareText = txt_totalfare.getText().replace("R", "").trim();
        double totalFare = Double.parseDouble(fareText);

        JSONObject reservationData = new JSONObject();

        // Required fields
        reservationData.put("customer_id", customerId);
        reservationData.put("reservation_code", "RES" + System.currentTimeMillis() % 100000);
        reservationData.put("flight_id", flightId);
        reservationData.put("seat_class", combo_seatclass.getValue());
        reservationData.put("base_fare", totalFare * 0.7);
        reservationData.put("tax_amount", totalFare * 0.14);
        reservationData.put("fuel_surcharge_amount", 50.00);
        reservationData.put("airport_tax_amount", 25.00);
        reservationData.put("total_fare", totalFare);

        // Optional fields that exist in your table
        if (seatId != null) {
            reservationData.put("seat_id", seatId);
        }

        reservationData.put("trip_type", combo_triptype.getValue());
        reservationData.put("seat_preference", combo_seatpref.getValue());
        reservationData.put("category_discount", totalFare * 0.3);
        reservationData.put("seat_surcharge", 0.00);
        reservationData.put("amount_paid", 0.00);
        reservationData.put("payment_status", "Pending");
        reservationData.put("reservation_status", "Waiting"); // Will be set to "Confirmed" for confirmed reservations
        reservationData.put("reservation_date", "now()");
        reservationData.put("is_confirmed", false);
        // Note: waiting_list_position will be set separately after we calculate it
        return reservationData;
    }

    private void createCustomerAndThenReservation() {
        System.out.println("=== CREATE CUSTOMER AND START BOOKING ===");

        String displayCategory = combo_categoryfare.getValue();
        String actualCategory = extractCategoryName(displayCategory);

        updateStatus("Creating customer profile...", true);

        supabaseService.insertCustomer(
                        txt_entername.getText().trim(),
                        txt_idnumber.getText().trim(),
                        txt_enteraddress.getText().trim(),
                        txt_enteremail.getText().trim(),
                        txt_enterphone.getText().trim(),
                        actualCategory,
                        userAuthToken
                ).thenAccept(response -> {
                    System.out.println("=== CUSTOMER CREATION RESPONSE ===");
                    System.out.println("Status: " + response.statusCode());
                    System.out.println("Body: " + response.body());

                    if (response.statusCode() == 201) {
                        System.out.println("Customer created successfully, loading customer data...");
                        loadCustomerFromSupabase();
                        Platform.runLater(() -> {
                            System.out.println("Proceeding to search flight after customer creation...");
                            processReservationWithSelectedFlight();
                        });
                    } else {
                        Platform.runLater(() -> {
                            updateStatus("Failed to create customer profile", false);
                            showAlert("Error", "Failed to create customer profile: " + response.body());
                        });
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception during customer creation: " + ex.getMessage());
                    Platform.runLater(() -> {
                        updateStatus("Network error creating customer", false);
                        showAlert("Error", "Network error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    // ==========================================
    // DATA LOADING METHODS
    // ==========================================

    private void loadAirports() {
        updateStatus("Loading flight schedules...", true);
        supabaseService.getAirports().thenAccept(response -> {
            if (response.statusCode() == 200) {
                try {
                    JSONArray airports = new JSONArray(response.body());
                    Platform.runLater(() -> {
                        airportsList.clear();
                        for (int i = 0; i < airports.length(); i++) {
                            JSONObject airport = airports.getJSONObject(i);

                            String code = airport.optString("airport_code", "???");
                            String city = airport.optString("city", "Unknown");
                            String name = airport.optString("airport_name", "Airport");

                            String depDate = airport.optString("departure_date", "");
                            String retDate = airport.optString("return_date", "");

                            StringBuilder sb = new StringBuilder();
                            sb.append(city).append(" (").append(code).append(")");

                            if (!depDate.isEmpty()) {
                                sb.append(" [Dep: ").append(depDate);
                                if (!retDate.isEmpty()) {
                                    sb.append(" | Ret: ").append(retDate);
                                }
                                sb.append("]");
                            } else {
                                sb.append(" - ").append(name);
                            }

                            airportsList.add(sb.toString());
                        }
                        combo_travelfrom.setItems(airportsList);
                        combo_travelto.setItems(airportsList);
                        updateStatus("Schedules Loaded", false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> updateStatus("Error parsing airports", false));
                }
            }
        });
    }

    private void loadFareCategories() {
        supabaseService.getAllFareCategories().thenAccept(response -> {
            if (response.statusCode() == 200) {
                try {
                    JSONArray categories = new JSONArray(response.body());
                    Platform.runLater(() -> {
                        fareCategoriesList.clear();
                        for (int i = 0; i < categories.length(); i++) {
                            JSONObject category = categories.getJSONObject(i);
                            String displayText = String.format("%s (%.0f%% discount)",
                                    category.optString("category_name", ""),
                                    (1 - category.optDouble("fare_multiplier", 1.0)) * 100);
                            fareCategoriesList.add(displayText);
                        }
                        combo_categoryfare.setItems(fareCategoriesList);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void prefillCustomerData() {
        if (userEmail != null) {
            txt_enteremail.setText(userEmail);
            loadCustomerFromSupabase();
        }
    }

    private void loadCustomerFromSupabase() {
        if (userAuthToken == null || userEmail == null) return;

        supabaseService.getCustomerByEmail(userEmail, userAuthToken).thenAccept(response -> {
                    System.out.println("=== CUSTOMER LOAD RESPONSE ===");
                    System.out.println("Status: " + response.statusCode());
                    System.out.println("Body: " + response.body());

                    if (response.statusCode() == 200) {
                        try {
                            JSONArray customers = new JSONArray(response.body());
                            if (customers.length() > 0) {
                                JSONObject customer = customers.getJSONObject(0);
                                Platform.runLater(() -> {
                                    txt_entername.setText(customer.optString("full_name", ""));
                                    txt_idnumber.setText(customer.optString("id_number", ""));
                                    txt_enteraddress.setText(customer.optString("address", ""));
                                    txt_enterphone.setText(customer.optString("phone", ""));

                                    String customerCategory = customer.optString("category_fare", "Regular");
                                    for (String displayText : fareCategoriesList) {
                                        if (displayText.contains(customerCategory)) {
                                            combo_categoryfare.setValue(displayText);
                                            break;
                                        }
                                    }
                                    customerId = customer.optLong("customer_id");
                                    SessionManager.setCustomerData(customerId, customerCategory);

                                    System.out.println("Loaded customer ID: " + customerId);
                                });
                            } else {
                                System.out.println("No customer found for email: " + userEmail);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Failed to load customer data: " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception loading customer: " + ex.getMessage());
                    return null;
                });
    }

    // ==========================================
    // TICKET PRINTING
    // ==========================================

    @FXML
    private void handlePrintTicket() {
        if (txt_entername.getText().isEmpty()) {
            showAlert("Error", "Please complete a reservation before printing.");
            return;
        }

        System.out.println("Print ticket requested. Starting PrinterJob...");

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(btn_print.getScene().getWindow())) {
            VBox ticketNode = createTicketNode();

            boolean success = job.printPage(ticketNode);
            if (success) {
                job.endJob();
                showAlert("Success", "Ticket sent to printer!");
            } else {
                showAlert("Error", "Printing failed.");
            }
        } else {
            System.out.println("Printing cancelled by user.");
        }
    }

    private VBox createTicketNode() {
        VBox ticket = new VBox(10);
        ticket.setPadding(new Insets(25));
        ticket.setStyle("-fx-background-color: white; -fx-border-color: #333; -fx-border-width: 1px;");
        ticket.setPrefWidth(450);

        Text title = new Text("BOARDING PASS (CONFIRMED)");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 18));

        Text flight = new Text("FLIGHT: " + txt_enterflightcode.getText());
        flight.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));

        Text route = new Text(extractAirportCode(combo_travelfrom.getValue()) + " ➝ " + extractAirportCode(combo_travelto.getValue()));
        route.setFont(Font.font("Verdana", FontWeight.BOLD, 16));

        ticket.getChildren().addAll(
                title,
                new Separator(),
                route,
                new Text("Date: " + dp_traveldate.getValue().toString()),
                new Separator(),
                new Text("Passenger Name: " + txt_entername.getText().toUpperCase()),
                new Text("ID/Passport: " + txt_idnumber.getText()),
                new Separator(),
                new Text("Class: " + combo_seatclass.getValue()),
                new Text("Seat Preference: " + combo_seatpref.getValue() + " (Assigned at check-in)"),
                new Text("Total Paid: " + txt_totalfare.getText()),
                new Separator(),
                new Text("Gate: TBD | Boarding Time: TBD")
        );

        ticket.setAlignment(Pos.TOP_LEFT);
        return ticket;
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    private void markSeatAsBooked(Long seatId) {
        System.out.println("Seat " + seatId + " marked as booked (Locally)");
    }

    private void generateTicket(String flightCode, String seatNumber, String status) {
        System.out.println("=== TICKET GENERATED FOR " + txt_entername.getText() + " ===");
        System.out.println("Flight: " + flightCode + " | Seat: " + seatNumber + " | Status: " + status);
    }

    private boolean validateForm() {
        if (txt_entername.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter your full name.");
            return false;
        }
        if (combo_travelfrom.getValue() == null || combo_travelto.getValue() == null) {
            showAlert("Validation Error", "Please select departure and destination airports.");
            return false;
        }
        if (combo_travelfrom.getValue().equals(combo_travelto.getValue())) {
            showAlert("Validation Error", "Departure and destination airports cannot be the same.");
            return false;
        }
        if (!chk_terms.isSelected()) {
            showAlert("Validation Error", "Please agree to the terms and conditions.");
            return false;
        }
        return true;
    }

    private void calculateTotalFare() {
        try {
            double baseFare = "Business".equals(combo_seatclass.getValue()) ? 5000.00 : 2000.00;
            int adults = Integer.parseInt(combo_adults.getValue());
            int children = Integer.parseInt(combo_children.getValue());
            int infants = Integer.parseInt(combo_infants.getValue());

            double multiplier = 1.0;
            String selectedCategory = combo_categoryfare.getValue();
            if (selectedCategory != null) {
                if (selectedCategory.contains("Student")) multiplier = 0.85;
                else if (selectedCategory.contains("Senior")) multiplier = 0.80;
                else if (selectedCategory.contains("Child")) multiplier = 0.60;
                else if (selectedCategory.contains("Infant")) multiplier = 0.10;
            }

            double total = (adults * baseFare * multiplier) +
                    (children * baseFare * multiplier * 0.6) +
                    (infants * baseFare * multiplier * 0.1);

            total += total * 0.14; // Tax
            total += 75.00; // Fees

            txt_totalfare.setText(String.format("R %.2f", total));
        } catch (Exception e) {
            // Ignore parse errors during initialization
        }
    }

    private String extractAirportCode(String displayText) {
        if (displayText == null) return "";
        int start = displayText.indexOf('(') + 1;
        int end = displayText.indexOf(')');
        if (start > 0 && end > start) return displayText.substring(start, end);
        return displayText;
    }

    private String extractCategoryName(String displayText) {
        if (displayText == null) return "Regular";
        if (displayText.contains("Student")) return "Student";
        if (displayText.contains("Senior")) return "Senior";
        if (displayText.contains("Child")) return "Child";
        if (displayText.contains("Infant")) return "Infant";
        return "Regular";
    }

    private void generateFlightCode() {
        String code = "BA" + System.currentTimeMillis() % 10000;
        txt_enterflightcode.setText(code);
    }

    @FXML
    private void clear_form() {
        txt_entername.clear();
        txt_idnumber.clear();
        txt_enteraddress.clear();
        txt_enterphone.clear();
        combo_adults.setValue("1");
        combo_seatclass.setValue("Economic");
        combo_available_flights.setValue(null);
        calculateTotalFare();
        updateStatus("Form cleared", false);
    }

    private void updateStatus(String message, boolean inProgress) {
        Platform.runLater(() -> {
            if (lbl_status != null) lbl_status.setText(message);
            if (progress_bar != null) progress_bar.setVisible(inProgress);
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ==========================================
    // UPDATED NAVIGATION METHODS USING SCENEMANAGER
    // ==========================================

    private void navigateToDashboard() {
        SceneManager.navigateToDashboard(btnDashboard.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToManageReservations() {
        SceneManager.navigateToManageReservations(btnManageReservations.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToFeedback() {
        SceneManager.navigateToFeedback(btnFeedback.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToSupport() {
        SceneManager.navigateToSupport(btnSupport.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToSettings() {
        SceneManager.navigateToSettings(btnSettings.getScene(), userAuthToken, userId, userEmail);
    }

    private void handleLogout() {
        SceneManager.handleLogout(btnLogout.getScene());
    }
}