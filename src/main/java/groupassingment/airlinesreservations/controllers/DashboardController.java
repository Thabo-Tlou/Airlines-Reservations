package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    // Existing FXML elements (Main Content)
    @FXML private Label welcomeLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private ComboBox<String> originComboBox;
    @FXML private ComboBox<String> destinationComboBox;
    @FXML private ComboBox<String> dateComboBox;
    @FXML private Spinner<Integer> passengerSpinner;
    @FXML private Button bookFlightButton;
    @FXML private TextField searchField;

    // New FXML elements for enhanced UI (Stats)
    @FXML private Label totalBookingsLabel;
    @FXML private Label completedFlightsLabel;
    @FXML private Label upcomingFlightsLabel;
    @FXML private Label totalSpendingLabel;
    @FXML private Label priceLabel;
    @FXML private Label routeOriginLabel;
    @FXML private Label routeDestinationLabel;
    @FXML private Label baseFareLabel;
    @FXML private Label passengerCountLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label bookingDateLabel;
    @FXML private Label bookingPassengersLabel;
    @FXML private Label nextFlightLabel;
    @FXML private Label loyaltyPointsLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label bookingCountLabel;

    // FXML elements for Sidebar Navigation Buttons
    @FXML private Button btn_dashboard;
    @FXML private Button btn_reservation;
    @FXML private Button btn_manage_reservations;
    @FXML private Button btn_feedback;
    @FXML private Button btn_support;
    @FXML private Button btn_settings;
    @FXML private Button btn_logout;

    private String currentUserEmail;
    private String currentUserId;
    private String currentUserName;
    private String currentUserToken;
    private SupabaseService supabaseService;

    // Pricing data
    private Map<String, Double> routePrices = new HashMap<>();
    private double currentBaseFare = 0.0;
    private int currentPassengers = 1;

    public DashboardController() {
        this.supabaseService = new SupabaseService();
        initializeRoutePrices();
    }

    @FXML
    public void initialize() {
        setupEventHandlers();
        initializeDateOptions();
        initializePassengerSpinner();
        loadAirportsFromSupabase();
        initializePriceCalculations();

        // Set initial loading state
        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome to Bokamoso Airlines!");
            profileNameLabel.setText("Guest User");
            profileEmailLabel.setText("Please log in to access full features");
            bookFlightButton.setDisable(true);
            bookFlightButton.setText("Login to Book");

            // Initialize stats with default values
            updateStatistics(0, 0, 0, 0.0);
            resetProfileDetails();
            resetPriceDisplay();
        });
    }

    /**
     * Initialize session data when coming from other controllers
     */
    public void initializeSessionData(String authToken, String userId, String userEmail) {
        System.out.println("=== INITIALIZING DASHBOARD SESSION DATA ===");
        System.out.println("User Email: " + userEmail);
        System.out.println("User ID: " + userId);

        this.currentUserEmail = userEmail;
        this.currentUserId = userId;
        this.currentUserToken = authToken != null ? authToken.trim() : null;

        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome back!");
            profileNameLabel.setText("Loading...");
            profileEmailLabel.setText(userEmail);
            bookFlightButton.setDisable(false);
            bookFlightButton.setText("Book Flight");
        });

        // Load user-specific data
        loadUserProfileData();
        loadUserStatistics();
        loadDashboardCardsData();

        Platform.runLater(() -> {
            showAlert("Welcome Back!", "Successfully logged in as " + userEmail);
        });
    }

    /**
     * Initialize user data (legacy method for backward compatibility)
     */
    public void initializeUserData(String userEmail, String userId, String userToken) {
        System.out.println("=== INITIALIZING USER DATA (LEGACY METHOD) ===");
        System.out.println("User Email: " + userEmail);
        System.out.println("User ID: " + userId);

        this.currentUserEmail = userEmail;
        this.currentUserId = userId;
        this.currentUserToken = userToken != null ? userToken.trim() : null;

        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome back!");
            profileNameLabel.setText("Loading...");
            profileEmailLabel.setText(userEmail);
            bookFlightButton.setDisable(false);
            bookFlightButton.setText("Book Flight");
        });

        // Load user-specific data
        loadUserProfileData();
        loadUserStatistics();
        loadDashboardCardsData();

        Platform.runLater(() -> {
            showAlert("Welcome Back!", "Successfully logged in as " + userEmail + "\n\nYour dashboard is now being loaded with your personal data.");
        });
    }

    private void setupEventHandlers() {
        bookFlightButton.setOnAction(event -> handleBookFlight());

        originComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        destinationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        passengerSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        dateComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateBookingSummary());

        // Navigation button handlers
        if (btn_dashboard != null) btn_dashboard.setOnAction(event -> handleViewDashboard());
        if (btn_reservation != null) btn_reservation.setOnAction(event -> handleViewReservation());
        if (btn_manage_reservations != null) btn_manage_reservations.setOnAction(event -> handleManageReservations());
        if (btn_feedback != null) btn_feedback.setOnAction(event -> handleViewFeedback());
        if (btn_support != null) btn_support.setOnAction(event -> handleViewSupport());
        if (btn_settings != null) btn_settings.setOnAction(event -> handleViewSettings());
        if (btn_logout != null) btn_logout.setOnAction(event -> handleLogout());
    }

    /**
     * FIX: Add the missing method that's referenced in FXML
     */
    @FXML
    private void handleReservationView() {
        System.out.println("=== HANDLE RESERVATION VIEW (FXML METHOD) ===");
        handleManageReservations(); // Route to manage reservations
    }

    private void resetProfileDetails() {
        Platform.runLater(() -> {
            nextFlightLabel.setText("No upcoming flights");
            loyaltyPointsLabel.setText("Loyalty Points: 0");
            memberSinceLabel.setText("Member since: -");
            bookingCountLabel.setText("Total bookings: 0");
        });
    }

    private void initializeDateOptions() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            dateComboBox.getItems().add(today.plusDays(i).format(formatter));
        }
    }

    private void initializePassengerSpinner() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);
        passengerSpinner.setValueFactory(valueFactory);
    }

    private void initializeRoutePrices() {
        routePrices.put("JNB-CPT", 2500.0);
        routePrices.put("JNB-DUR", 1800.0);
        routePrices.put("JNB-GBE", 3200.0);
        routePrices.put("JNB-MSU", 2800.0);
        routePrices.put("CPT-JNB", 2500.0);
        routePrices.put("CPT-DUR", 2200.0);
        routePrices.put("DUR-JNB", 1800.0);
        routePrices.put("DUR-CPT", 2200.0);
        routePrices.put("GBE-JNB", 3200.0);
        routePrices.put("MSU-JNB", 2800.0);
    }

    private void initializePriceCalculations() {
        updatePriceCalculation();
        updateBookingSummary();
    }

    // ---------------- LOAD DATA ----------------

    private void loadAirportsFromSupabase() {
        System.out.println("=== STARTING AIRPORT LOADING PROCESS ===");

        supabaseService.testConnection()
                .thenAccept(connected -> {
                    if (connected) {
                        System.out.println("✓ Supabase connection successful");
                        fetchAirports();
                    } else {
                        System.err.println("✗ Failed to connect to Supabase");
                        Platform.runLater(() -> showAlert("Error", "Cannot connect to database. Please check your internet connection."));
                    }
                });
    }

    private void fetchAirports() {
        System.out.println("Fetching airports from Supabase...");

        supabaseService.getAirports()
                .thenAccept(response -> {
                    System.out.println("Airports API Response - Status: " + response.statusCode());

                    if (response.statusCode() == 200) {
                        try {
                            JSONArray airports = new JSONArray(response.body());
                            System.out.println("Successfully parsed " + airports.length() + " airports");

                            Platform.runLater(() -> {
                                originComboBox.getItems().clear();
                                destinationComboBox.getItems().clear();

                                if (airports.length() == 0) {
                                    showAlert("Info", "No airports available in database.");
                                    return;
                                }

                                for (int i = 0; i < airports.length(); i++) {
                                    JSONObject airport = airports.getJSONObject(i);
                                    String code = airport.optString("airport_code", "N/A");
                                    String name = airport.optString("airport_name", "Unknown Airport");
                                    String city = airport.optString("city", "");
                                    String country = airport.optString("country", "");

                                    String displayText = code + " - " + name + " (" + city + ", " + country + ")";
                                    originComboBox.getItems().add(displayText);
                                    destinationComboBox.getItems().add(displayText);
                                }
                                System.out.println("✓ Loaded " + airports.length() + " airports into comboboxes");

                                if (originComboBox.getItems().size() > 0) {
                                    originComboBox.setValue(originComboBox.getItems().get(0));
                                }
                                if (destinationComboBox.getItems().size() > 1) {
                                    destinationComboBox.setValue(destinationComboBox.getItems().get(1));
                                }

                                updatePriceCalculation();
                            });
                        } catch (Exception e) {
                            System.err.println("✗ Failed to parse airports JSON: " + e.getMessage());
                            Platform.runLater(() -> showAlert("Error", "Failed to parse airport data: " + e.getMessage()));
                        }
                    } else {
                        System.err.println("✗ Error fetching airports: " + response.statusCode() + " - " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("✗ Network error loading airports: " + ex.getMessage());
                    Platform.runLater(() -> showAlert("Error", "Network error: " + ex.getMessage()));
                    return null;
                });
    }

    // ---------------- DASHBOARD CARDS DATA ----------------

    /**
     * Load data for all dashboard cards and statistics
     */
    private void loadDashboardCardsData() {
        if (currentUserEmail == null || currentUserToken == null) {
            System.out.println("No user logged in, skipping dashboard cards load");
            return;
        }

        System.out.println("Loading dashboard cards data for: " + currentUserEmail);

        // Load user reservations for statistics
        loadUserReservationsForDashboard();

        // Load next upcoming flight
        loadNextUpcomingFlight();

        // Load loyalty points and member info
        loadLoyaltyAndMemberInfo();
    }

    private void loadUserReservationsForDashboard() {
        System.out.println("Loading user reservations for dashboard...");

        // Use getAllReservations and filter by current user
        supabaseService.getAllReservations(currentUserToken)
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 206) {
                        try {
                            JSONArray allReservations = new JSONArray(response.body());
                            JSONArray userReservations = new JSONArray();

                            // Filter reservations for current user
                            for (int i = 0; i < allReservations.length(); i++) {
                                JSONObject reservation = allReservations.getJSONObject(i);

                                // Check if reservation belongs to current user
                                if (reservation.has("customer_id")) {
                                    // If customer_id is an object, check the email
                                    if (reservation.get("customer_id") instanceof JSONObject) {
                                        JSONObject customer = reservation.getJSONObject("customer_id");
                                        String customerEmail = customer.optString("email", "");
                                        if (currentUserEmail.equals(customerEmail)) {
                                            userReservations.put(reservation);
                                        }
                                    } else {
                                        // If customer_id is just an ID, we need to check differently
                                        // For now, include all reservations and we'll filter later
                                        userReservations.put(reservation);
                                    }
                                }
                            }

                            System.out.println("Filtered " + userReservations.length() + " reservations for user");
                            calculateDashboardStatistics(userReservations);

                        } catch (Exception e) {
                            System.err.println("Error parsing reservations for dashboard: " + e.getMessage());
                            updateStatistics(0, 0, 0, 0.0);
                        }
                    } else {
                        System.err.println("Failed to load reservations for dashboard: " + response.statusCode());
                        System.err.println("Response body: " + response.body());
                        updateStatistics(0, 0, 0, 0.0);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error loading reservations for dashboard: " + ex.getMessage());
                    updateStatistics(0, 0, 0, 0.0);
                    return null;
                });
    }

    private void calculateDashboardStatistics(JSONArray reservations) {
        int totalBookings = reservations.length();
        int completedFlights = 0;
        int upcomingFlights = 0;
        double totalSpending = 0.0;

        try {
            LocalDate today = LocalDate.now();

            for (int i = 0; i < reservations.length(); i++) {
                JSONObject reservation = reservations.getJSONObject(i);

                String status = reservation.optString("reservation_status", "Pending");
                double totalFare = reservation.optDouble("total_fare", 0.0);

                totalSpending += totalFare;

                if ("Confirmed".equalsIgnoreCase(status)) {
                    upcomingFlights++;
                } else if ("Completed".equalsIgnoreCase(status)) {
                    completedFlights++;
                } else if ("Cancelled".equalsIgnoreCase(status)) {
                    // Don't count cancelled reservations in totals
                    totalBookings--;
                    totalSpending -= totalFare;
                }
            }

            int loyaltyPoints = (int) (totalSpending / 10);

            int finalTotalBookings = totalBookings;
            int finalCompletedFlights = completedFlights;
            int finalUpcomingFlights = upcomingFlights;
            double finalTotalSpending = totalSpending;
            Platform.runLater(() -> {
                updateStatistics(finalTotalBookings, finalCompletedFlights, finalUpcomingFlights, finalTotalSpending);
                loyaltyPointsLabel.setText("Loyalty Points: " + loyaltyPoints);
                bookingCountLabel.setText("Total bookings: " + finalTotalBookings);

                // Set member since (using current date for demo)
                memberSinceLabel.setText("Member since: " + LocalDate.now().minusMonths(3).format(DateTimeFormatter.ofPattern("MMM yyyy")));

                System.out.println("✓ Dashboard statistics updated: " + finalTotalBookings + " bookings, M" + finalTotalSpending + " spent");
            });

        } catch (Exception e) {
            System.err.println("Error calculating dashboard statistics: " + e.getMessage());
            Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
        }
    }

    private void loadNextUpcomingFlight() {
        if (currentUserEmail == null) return;

        // Use getAllReservations and find the next upcoming one
        supabaseService.getAllReservations(currentUserToken)
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 206) {
                        try {
                            JSONArray allReservations = new JSONArray(response.body());
                            JSONObject nextFlight = findNextUpcomingFlight(allReservations);
                            updateNextFlightDisplay(nextFlight);
                        } catch (Exception e) {
                            System.err.println("Error finding next flight: " + e.getMessage());
                        }
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error loading next flight: " + ex.getMessage());
                    return null;
                });
    }

    private JSONObject findNextUpcomingFlight(JSONArray reservations) {
        try {
            JSONObject nextFlight = null;

            for (int i = 0; i < reservations.length(); i++) {
                JSONObject reservation = reservations.getJSONObject(i);
                String status = reservation.optString("reservation_status", "");

                // Check if reservation belongs to current user and is confirmed
                if ("Confirmed".equalsIgnoreCase(status) && reservation.has("customer_id")) {
                    if (reservation.get("customer_id") instanceof JSONObject) {
                        JSONObject customer = reservation.getJSONObject("customer_id");
                        String customerEmail = customer.optString("email", "");
                        if (currentUserEmail.equals(customerEmail)) {
                            if (nextFlight == null) {
                                nextFlight = reservation;
                            }
                        }
                    } else {
                        // If we can't verify the customer, include it for now
                        if (nextFlight == null) {
                            nextFlight = reservation;
                        }
                    }
                }
            }
            return nextFlight;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateNextFlightDisplay(JSONObject nextFlight) {
        Platform.runLater(() -> {
            if (nextFlight != null) {
                try {
                    String reservationCode = nextFlight.optString("reservation_code", "N/A");
                    String seatClass = nextFlight.optString("seat_class", "Economic");

                    // Get flight details if available
                    if (nextFlight.has("flight_id") && !nextFlight.isNull("flight_id")) {
                        Long flightId = nextFlight.getLong("flight_id");
                        // Fetch flight details to get route information
                        fetchFlightDetailsForDisplay(flightId, reservationCode, seatClass);
                    } else {
                        nextFlightLabel.setText("Next Flight: " + reservationCode + " (" + seatClass + ")");
                    }
                } catch (Exception e) {
                    nextFlightLabel.setText("Upcoming flight booked");
                }
            } else {
                nextFlightLabel.setText("No upcoming flights");
            }
        });
    }

    private void fetchFlightDetailsForDisplay(Long flightId, String reservationCode, String seatClass) {
        supabaseService.getFlightDetails(flightId)
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JSONArray flights = new JSONArray(response.body());
                            if (flights.length() > 0) {
                                JSONObject flight = flights.getJSONObject(0);
                                String flightCode = flight.optString("flight_code", "N/A");
                                String departure = flight.optString("departure_city", "Unknown");
                                String destination = flight.optString("destination_city", "Unknown");
                                String date = flight.optString("departure_date", "");

                                Platform.runLater(() -> {
                                    String displayText = String.format("Next: %s → %s | %s | %s",
                                            departure, destination,
                                            date.isEmpty() ? "Soon" : formatDate(date),
                                            seatClass);
                                    nextFlightLabel.setText(displayText);
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() ->
                                    nextFlightLabel.setText("Next: " + reservationCode + " (" + seatClass + ")"));
                        }
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            nextFlightLabel.setText("Next: " + reservationCode + " (" + seatClass + ")"));
                    return null;
                });
    }

    private void loadLoyaltyAndMemberInfo() {
        // Calculate loyalty points based on total spending
        // This is already handled in calculateDashboardStatistics
        // Additional loyalty info can be loaded here if needed
    }

    private String formatDate(String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString);
            return date.format(DateTimeFormatter.ofPattern("MMM dd"));
        } catch (Exception e) {
            return dateString;
        }
    }

    // ---------------- PRICE CALCULATIONS ----------------

    private void updatePriceCalculation() {
        String origin = originComboBox.getValue();
        String destination = destinationComboBox.getValue();
        Integer passengers = passengerSpinner.getValue();

        if (origin == null || destination == null || passengers == null) {
            resetPriceDisplay();
            return;
        }

        try {
            String originCode = origin.split(" - ")[0];
            String destinationCode = destination.split(" - ")[0];

            Platform.runLater(() -> {
                routeOriginLabel.setText(originCode);
                routeDestinationLabel.setText(destinationCode);
            });

            String routeKey = originCode + "-" + destinationCode;
            currentBaseFare = routePrices.getOrDefault(routeKey, 1500.0);
            currentPassengers = passengers;

            double totalPrice = currentBaseFare * currentPassengers;

            Platform.runLater(() -> {
                baseFareLabel.setText(String.format("R%.2f", currentBaseFare));
                passengerCountLabel.setText("x " + currentPassengers);
                totalPriceLabel.setText(String.format("R%.2f", totalPrice));
                priceLabel.setText(String.format("R%.2f", totalPrice));

                bookingPassengersLabel.setText(currentPassengers + " passenger" + (currentPassengers > 1 ? "s" : ""));
            });

        } catch (Exception e) {
            System.err.println("Error calculating price: " + e.getMessage());
            resetPriceDisplay();
        }
    }

    private void updateBookingSummary() {
        String date = dateComboBox.getValue();
        Platform.runLater(() -> {
            if (date != null) {
                try {
                    LocalDate flightDate = LocalDate.parse(date);
                    DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    bookingDateLabel.setText(flightDate.format(displayFormatter));

                    if (currentUserEmail != null) {
                        nextFlightLabel.setText("Next Flight: " +
                                (routeOriginLabel.getText().equals("-") ? "Not booked" :
                                        routeOriginLabel.getText() + " → " + routeDestinationLabel.getText() + " | " +
                                                flightDate.format(displayFormatter)));
                    }
                } catch (Exception e) {
                    bookingDateLabel.setText("Invalid date");
                }
            } else {
                bookingDateLabel.setText("No date selected");
                if (currentUserEmail != null) {
                    nextFlightLabel.setText("No upcoming flights");
                }
            }
        });
    }

    private void resetPriceDisplay() {
        Platform.runLater(() -> {
            priceLabel.setText("R0.00");
            routeOriginLabel.setText("-");
            routeDestinationLabel.setText("-");
            baseFareLabel.setText("R0.00");
            passengerCountLabel.setText("x 0");
            totalPriceLabel.setText("R0.00");
            bookingPassengersLabel.setText("0 passengers");
            bookingDateLabel.setText("No date selected");
            if (currentUserEmail != null) {
                nextFlightLabel.setText("No upcoming flights");
            }
        });
    }

    // ---------------- USER STATISTICS ----------------

    private void loadUserStatistics() {
        if (currentUserEmail == null || currentUserToken == null) {
            System.out.println("No user or token logged in, skipping statistics load");
            return;
        }

        System.out.println("Loading user statistics for: " + currentUserEmail);

        // Use getAllReservations and filter for the current user
        supabaseService.getAllReservations(currentUserToken)
                .thenAccept(response -> {
                    System.out.println("User statistics response - Status: " + response.statusCode());

                    if (response.statusCode() == 200 || response.statusCode() == 206) {
                        try {
                            JSONArray allReservations = new JSONArray(response.body());
                            JSONArray userBookings = new JSONArray();

                            // Filter bookings for current user
                            for (int i = 0; i < allReservations.length(); i++) {
                                JSONObject reservation = allReservations.getJSONObject(i);

                                // Check if reservation belongs to current user
                                if (reservation.has("customer_id")) {
                                    if (reservation.get("customer_id") instanceof JSONObject) {
                                        JSONObject customer = reservation.getJSONObject("customer_id");
                                        String customerEmail = customer.optString("email", "");
                                        if (currentUserEmail.equals(customerEmail)) {
                                            userBookings.put(reservation);
                                        }
                                    } else {
                                        // Include all for now if we can't verify customer
                                        userBookings.put(reservation);
                                    }
                                }
                            }

                            System.out.println("Found " + userBookings.length() + " bookings for user");
                            calculateUserStatistics(userBookings);
                        } catch (Exception e) {
                            System.err.println("Error parsing user bookings: " + e.getMessage());
                            Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
                        }
                    } else {
                        System.err.println("Failed to load user statistics: " + response.statusCode());
                        System.err.println("Response body: " + response.body());
                        Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error loading user statistics: " + ex.getMessage());
                    Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
                    return null;
                });
    }

    private void calculateUserStatistics(JSONArray bookings) {
        int totalBookings = bookings.length();
        int completedFlights = 0;
        int upcomingFlights = 0;
        double totalSpending = 0.0;

        try {
            for (int i = 0; i < bookings.length(); i++) {
                JSONObject booking = bookings.getJSONObject(i);

                String status = booking.optString("reservation_status", "Pending");
                double totalFare = booking.optDouble("total_fare", 0.0);

                totalSpending += totalFare;

                if ("Confirmed".equalsIgnoreCase(status)) {
                    upcomingFlights++;
                } else if ("Completed".equalsIgnoreCase(status)) {
                    completedFlights++;
                } else if ("Cancelled".equalsIgnoreCase(status)) {
                    // Adjust totals for cancelled bookings
                    totalBookings--;
                    totalSpending -= totalFare;
                }
            }

            int finalTotalBookings = totalBookings;
            int finalCompletedFlights = completedFlights;
            int finalUpcomingFlights = upcomingFlights;
            double finalTotalSpending = totalSpending;
            Platform.runLater(() -> {
                updateStatistics(finalTotalBookings, finalCompletedFlights, finalUpcomingFlights, finalTotalSpending);
                System.out.println("✓ User statistics updated: " + finalTotalBookings + " bookings, R" + finalTotalSpending + " spent");
            });

        } catch (Exception e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
            Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
        }
    }

    private void updateStatistics(int totalBookings, int completedFlights, int upcomingFlights, double totalSpending) {
        Platform.runLater(() -> {
            totalBookingsLabel.setText(String.valueOf(totalBookings));
            completedFlightsLabel.setText(String.valueOf(completedFlights));
            upcomingFlightsLabel.setText(String.valueOf(upcomingFlights));
            totalSpendingLabel.setText(String.format("R%.2f", totalSpending));
        });
    }

    // ---------------- USER PROFILE DATA ----------------

    private void loadUserProfileData() {
        if (currentUserEmail == null || currentUserToken == null) {
            System.out.println("No user data available, skipping profile load");
            return;
        }

        System.out.println("Loading user profile data for: " + currentUserEmail);

        supabaseService.getPassengerByEmail(currentUserEmail, currentUserToken)
                .thenAccept(response -> {
                    System.out.println("User profile response - Status: " + response.statusCode());

                    if (response.statusCode() == 200 && !response.body().equals("[]")) {
                        try {
                            JSONArray jsonArray = new JSONArray(response.body());
                            JSONObject userData = jsonArray.getJSONObject(0);
                            updateUIWithUserData(userData);
                            return;
                        } catch (Exception e) {
                            System.err.println("Error parsing user data: " + e.getMessage());
                        }
                    }
                    createNewPassengerRecord();
                })
                .exceptionally(ex -> {
                    System.err.println("Error loading user data: " + ex.getMessage());
                    createNewPassengerRecord();
                    return null;
                });
    }

    private void createNewPassengerRecord() {
        if (currentUserId == null || currentUserToken == null) {
            System.err.println("Cannot create passenger record: Missing user ID or token.");
            useAuthUserData();
            return;
        }

        System.out.println("Creating new passenger record for: " + currentUserEmail);
        String[] nameParts = extractNamesFromEmail(currentUserEmail);

        supabaseService.insertPassenger(nameParts[0], nameParts[1], currentUserEmail, "Not provided", currentUserId, currentUserToken)
                .thenAccept(response -> {
                    System.out.println("Create passenger response - Status: " + response.statusCode());

                    if (response.statusCode() == 201) {
                        System.out.println("✓ New passenger record created successfully. Fetching data...");
                        fetchAndFinalizeProfileData();
                    } else {
                        System.err.println("Failed to create passenger record, using auth data. Status: " + response.statusCode());
                        useAuthUserData();
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error creating passenger: " + ex.getMessage());
                    useAuthUserData();
                    return null;
                });
    }

    private void fetchAndFinalizeProfileData() {
        if (currentUserToken == null) {
            System.err.println("Cannot finalize profile: Missing user token.");
            useAuthUserData();
            return;
        }

        System.out.println("Finalizing profile data load...");

        supabaseService.getPassengerByEmail(currentUserEmail, currentUserToken)
                .thenAccept(response -> {
                    if (response.statusCode() == 200 && !response.body().equals("[]")) {
                        try {
                            JSONArray jsonArray = new JSONArray(response.body());
                            JSONObject userData = jsonArray.getJSONObject(0);
                            updateUIWithUserData(userData);
                            System.out.println("✓ Profile finalized and UI updated.");
                            return;
                        } catch (Exception e) {
                            System.err.println("Error parsing user data after creation: " + e.getMessage());
                        }
                    }
                    useAuthUserData();
                })
                .exceptionally(ex -> {
                    System.err.println("Error fetching user data after creation: " + ex.getMessage());
                    useAuthUserData();
                    return null;
                });
    }

    private void updateUIWithUserData(JSONObject userData) {
        String firstName = userData.optString("first_name", "User");
        String lastName = userData.optString("last_name", "");
        String email = userData.optString("email", currentUserEmail);

        this.currentUserName = firstName + " " + lastName;

        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome " + firstName + "!");
            profileNameLabel.setText(firstName + " " + lastName);
            profileEmailLabel.setText(email);

            System.out.println("✓ Updated UI with user data: " + firstName + " " + lastName);
        });
    }

    private void useAuthUserData() {
        String nameFromEmail = extractNameFromEmail(currentUserEmail);
        this.currentUserName = nameFromEmail;

        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome " + nameFromEmail + "!");
            profileNameLabel.setText(nameFromEmail);
            profileEmailLabel.setText(currentUserEmail);
        });
        System.out.println("✓ Using auth user data: " + nameFromEmail);
    }

    private String[] extractNamesFromEmail(String email) {
        String firstName = "User";
        String lastName = "";
        try {
            String[] nameParts = email.split("@")[0].split("[._]");
            if (nameParts.length > 0) firstName = capitalize(nameParts[0]);
            if (nameParts.length > 1) lastName = capitalize(nameParts[1]);
        } catch (Exception ignored) {}
        return new String[]{firstName, lastName};
    }

    private String extractNameFromEmail(String email) {
        try {
            String[] parts = email.split("@")[0].split("[._]");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(capitalize(p));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "User";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ---------------- BOOKING ----------------

    @FXML
    private void handleBookFlight() {
        if (currentUserEmail == null) {
            showAlert("Login Required", "Please log in to book flights.");
            return;
        }

        try {
            String origin = originComboBox.getValue();
            String destination = destinationComboBox.getValue();
            String date = dateComboBox.getValue();
            Integer passengers = passengerSpinner.getValue();

            if (origin == null || destination == null || date == null) {
                showAlert("Error", "Please fill in all flight details");
                return;
            }

            String originCode = origin.split(" - ")[0];
            String destinationCode = destination.split(" - ")[0];

            if (originCode.equals(destinationCode)) {
                showAlert("Error", "Origin and destination cannot be the same");
                return;
            }

            String routeKey = originCode + "-" + destinationCode;
            double baseFare = routePrices.getOrDefault(routeKey, 1500.0);
            double totalPrice = baseFare * passengers;

            // Instead of using the deprecated flight_bookings table, navigate to reservation form
            Platform.runLater(() -> {
                showAlert("Redirecting", "Redirecting to reservation form to complete your booking...");
                handleViewReservation(); // Navigate to the proper reservation form
            });

        } catch (Exception e) {
            System.err.println("Unexpected booking error: " + e.getMessage());
            showAlert("Error", "Unexpected error: " + e.getMessage());
        }
    }

    // ---------------- NAVIGATION HANDLERS USING SCENE MANAGER ----------------

    @FXML
    private void handleViewDashboard() {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        // Refresh dashboard data
        if (currentUserEmail != null) {
            loadUserStatistics();
            loadDashboardCardsData();
        }
        // Since we're already on dashboard, just refresh the data
        SceneManager.navigateToDashboard(btn_dashboard.getScene());
    }

    @FXML
    private void handleViewReservation() {
        if (currentUserEmail == null) {
            showAlert("Login Required", "Please log in to make a new reservation.");
            return;
        }

        System.out.println("=== NAVIGATING TO RESERVATION FORM ===");
        SceneManager.navigateToReservationForm(btn_reservation.getScene());
    }

    @FXML
    private void handleManageReservations() {
        if (currentUserEmail == null) {
            showAlert("Login Required", "Please log in to manage reservations.");
            return;
        }

        System.out.println("=== NAVIGATING TO MANAGE RESERVATIONS ===");
        SceneManager.navigateToManageReservations(btn_manage_reservations.getScene());
    }

    @FXML
    private void handleViewFeedback() {
        System.out.println("=== NAVIGATING TO FEEDBACK ===");
        SceneManager.navigateToFeedback(btn_feedback.getScene());
    }

    @FXML
    private void handleViewSupport() {
        System.out.println("=== NAVIGATING TO SUPPORT ===");
        SceneManager.navigateToSupport(btn_support.getScene());
    }

    @FXML
    private void handleViewSettings() {
        System.out.println("=== NAVIGATING TO SETTINGS ===");
        SceneManager.navigateToSettings(btn_settings.getScene());
    }

    @FXML
    private void handleLogout() {
        System.out.println("=== HANDLING LOGOUT ===");
        SceneManager.handleLogout(btn_logout.getScene());
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

    public boolean isUserLoggedIn() {
        return currentUserEmail != null;
    }

    public String getCurrentUserName() {
        return currentUserName != null ? currentUserName : "Guest";
    }
}