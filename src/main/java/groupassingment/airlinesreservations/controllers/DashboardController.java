package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.IOException;
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

    // FXML elements for Sidebar Navigation Buttons (NEWLY ADDED)
    @FXML private Button btn_dashboard;
    @FXML private Button btn_reservation;
    @FXML private Button btn_manage_reservations; // Using the ID from your latest instruction
    @FXML private Button btn_feedback;
    @FXML private Button btn_support;
    @FXML private Button btn_settings;


    private String currentUserEmail;
    private String currentUserId;
    private String currentUserName;
    private String currentUserToken; // 💡 STORED TOKEN
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
            welcomeLabel.setText("Loading...");
            profileNameLabel.setText("Loading...");
            profileEmailLabel.setText("Loading user data...");
            bookFlightButton.setDisable(true);
            bookFlightButton.setText("Please wait...");
        });
    }

    private void setupEventHandlers() {
        bookFlightButton.setOnAction(event -> handleBookFlight());

        originComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        destinationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        passengerSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceCalculation());
        dateComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateBookingSummary());

        // Navigation button handlers (NEWLY ADDED)
        if (btn_dashboard != null) btn_dashboard.setOnAction(event -> handleViewDashboard());
        if (btn_reservation != null) btn_reservation.setOnAction(event -> handleViewReservation());
        if (btn_manage_reservations != null) btn_manage_reservations.setOnAction(event -> handleViewManageReservations());
        if (btn_feedback != null) btn_feedback.setOnAction(event -> handleViewFeedback());
        if (btn_support != null) btn_support.setOnAction(event -> handleViewSupport());
        if (btn_settings != null) btn_settings.setOnAction(event -> handleViewSettings());
    }

    private void showDefaultState() {
        if (currentUserEmail == null) {
            // Guest state
            Platform.runLater(() -> {
                welcomeLabel.setText("Welcome Guest!");
                profileNameLabel.setText("Guest User");
                profileEmailLabel.setText("Please log in to book flights");
                bookFlightButton.setDisable(true);
                bookFlightButton.setText("Login to Book");

                updateStatistics(0, 0, 0, 0.0);
                resetProfileDetails();
                resetPriceDisplay();
            });
        } else {
            // User is logged in - show loading state while data loads
            Platform.runLater(() -> {
                welcomeLabel.setText("Welcome back!");
                profileNameLabel.setText("Loading...");
                profileEmailLabel.setText(currentUserEmail);
                bookFlightButton.setDisable(false);
                bookFlightButton.setText("Book Flight");
            });

            // Load user-specific data
            loadUserProfileData();
            loadUserStatistics();
        }
    }

    private void resetProfileDetails() {
        Platform.runLater(() -> {
            nextFlightLabel.setText("No upcoming flights");
            loyaltyPointsLabel.setText("Loyalty Points: 0");
            memberSinceLabel.setText("Member since: -");
            bookingCountLabel.setText("Total bookings: 0");

            if (currentUserEmail == null) {
                profileEmailLabel.setText("Please log in to book flights");
            }
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
                        if (currentUserEmail != null) {
                            loadUserStatistics();
                        }
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
                baseFareLabel.setText(String.format("M%.2f", currentBaseFare));
                passengerCountLabel.setText("x " + currentPassengers);
                totalPriceLabel.setText(String.format("M%.2f", totalPrice));
                priceLabel.setText(String.format("M%.2f", totalPrice));

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
            priceLabel.setText("M0.00");
            routeOriginLabel.setText("-");
            routeDestinationLabel.setText("-");
            baseFareLabel.setText("M0.00");
            passengerCountLabel.setText("x 0");
            totalPriceLabel.setText("M0.00");
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

        // 🔑 FIX: Corrected call to pass the currentUserToken for RLS compliance
        supabaseService.getUserBookings(currentUserEmail, currentUserToken)
                .thenAccept(response -> {
                    System.out.println("User statistics response - Status: " + response.statusCode());

                    if (response.statusCode() == 200) {
                        try {
                            JSONArray bookings = new JSONArray(response.body());
                            System.out.println("Found " + bookings.length() + " bookings for user");
                            calculateUserStatistics(bookings);
                        } catch (Exception e) {
                            System.err.println("Error parsing user bookings: " + e.getMessage());
                            Platform.runLater(() -> updateStatistics(0, 0, 0, 0.0));
                        }
                    } else {
                        System.err.println("Failed to load user statistics: " + response.statusCode());
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
            LocalDate today = LocalDate.now();

            for (int i = 0; i < bookings.length(); i++) {
                JSONObject booking = bookings.getJSONObject(i);
                String status = booking.optString("status", "");
                String flightDateStr = booking.optString("flight_date", "");
                double price = booking.optDouble("price", 0.0);

                totalSpending += price;

                if ("completed".equalsIgnoreCase(status)) {
                    completedFlights++;
                } else if ("confirmed".equalsIgnoreCase(status) && !flightDateStr.isEmpty()) {
                    try {
                        LocalDate flightDate = LocalDate.parse(flightDateStr);
                        if (flightDate.isAfter(today) || flightDate.isEqual(today)) {
                            upcomingFlights++;
                        }
                    } catch (Exception e) {
                        // Skip date parsing errors
                    }
                }
            }

            int loyaltyPoints = (int) (totalSpending / 100);

            int finalCompletedFlights = completedFlights;
            int finalUpcomingFlights = upcomingFlights;
            double finalTotalSpending = totalSpending;

            Platform.runLater(() -> {
                updateStatistics(totalBookings, finalCompletedFlights, finalUpcomingFlights, finalTotalSpending);
                loyaltyPointsLabel.setText("Loyalty Points: " + loyaltyPoints);
                bookingCountLabel.setText("Total bookings: " + totalBookings);

                memberSinceLabel.setText("Member since: " + LocalDate.now().minusMonths(6).format(DateTimeFormatter.ofPattern("MMM yyyy")));

                System.out.println("✓ User statistics updated: " + totalBookings + " bookings, M" + finalTotalSpending + " spent");
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
            totalSpendingLabel.setText(String.format("M%.2f", totalSpending));
        });
    }

    // ---------------- USER SETUP ----------------

    /**
     * FIX: Updated to accept and store the user token for authenticated database calls.
     * CRITICAL FIX APPLIED: The userToken is now trimmed to remove whitespace that breaks the JWT format.
     */
    public void initializeUserData(String userEmail, String userId, String userToken) {
        System.out.println("=== INITIALIZING USER DATA ===");
        System.out.println("User Email: " + userEmail);
        System.out.println("User ID: " + userId);

        this.currentUserEmail = userEmail;
        this.currentUserId = userId;
        // 🔑 CRITICAL FIX: Trim the token immediately upon receiving it
        this.currentUserToken = userToken != null ? userToken.trim() : null;

        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome back!");
            profileNameLabel.setText("Loading...");
            profileEmailLabel.setText(userEmail);
            bookFlightButton.setDisable(false);
            bookFlightButton.setText("Book Flight");
        });

        loadUserProfileData();
        loadUserStatistics();

        Platform.runLater(() -> {
            showAlert("Welcome Back!", "Successfully logged in as " + userEmail + "\n\nYour dashboard is now being loaded with your personal data.");
        });
    }

    /**
     * FIX APPLIED: Now passes the currentUserToken to the SupabaseService.
     */
    private void loadUserProfileData() {
        if (currentUserEmail == null || currentUserToken == null) {
            System.out.println("No user data available, skipping profile load");
            return;
        }

        System.out.println("Loading user profile data for: " + currentUserEmail);

        // 🟢 Pass currentUserToken for authenticated profile retrieval
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

    /**
     * ✅ FIX APPLIED: Now passes the currentUserId and currentUserToken to the SupabaseService
     * to match the 6-argument signature.
     */
    private void createNewPassengerRecord() {
        if (currentUserId == null || currentUserToken == null) {
            System.err.println("Cannot create passenger record: Missing user ID or token.");
            useAuthUserData();
            return;
        }

        System.out.println("Creating new passenger record for: " + currentUserEmail);
        String[] nameParts = extractNamesFromEmail(currentUserEmail);

        // 🟢 FIX: Pass the userId (6th argument)
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

    /**
     * FIX APPLIED: Now passes the currentUserToken to the SupabaseService.
     */
    private void fetchAndFinalizeProfileData() {
        if (currentUserToken == null) {
            System.err.println("Cannot finalize profile: Missing user token.");
            useAuthUserData();
            return;
        }

        System.out.println("Finalizing profile data load...");

        // 🟢 Pass currentUserToken for authenticated final profile retrieval
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
            welcomeLabel.setText("Welcome Mr " + firstName + "!");
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

    // ---------------- BOOKING (FROM MAIN DASHBOARD) ----------------

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

            JSONObject bookingData = new JSONObject();
            bookingData.put("user_email", currentUserEmail);
            bookingData.put("user_id", currentUserId);
            bookingData.put("origin", originCode);
            bookingData.put("destination", destinationCode);
            bookingData.put("flight_date", date);
            bookingData.put("passengers", passengers);
            bookingData.put("status", "confirmed");
            bookingData.put("price", totalPrice);
            bookingData.put("flight_code", generateFlightCode(originCode, destinationCode));

            System.out.println("Sending booking data: " + bookingData.toString());

            // FIX: Pass the authenticated token to the service call
            supabaseService.insertFlightBooking(bookingData, currentUserToken)
                    .thenAccept(response -> Platform.runLater(() -> {
                        System.out.println("Booking response - Status: " + response.statusCode());

                        if (response.statusCode() == 201) {
                            showAlert("Success",
                                    "Flight booked successfully!\n\n" +
                                            "Route: " + originCode + " → " + destinationCode + "\n" +
                                            "Date: " + date + "\n" +
                                            "Passengers: " + passengers + "\n" +
                                            "Total: M" + String.format("%.2f", totalPrice) + "\n\n" +
                                            "Thank you, " + currentUserName + "!");
                            clearBookingForm();
                            loadUserStatistics();
                        } else {
                            showAlert("Error", "Failed to save booking. Status: " + response.statusCode() + ". Check Supabase logs/RLS.");
                        }
                    }))
                    .exceptionally(ex -> {
                        System.err.println("Booking error: " + ex.getMessage());
                        Platform.runLater(() -> showAlert("Error", "Network error: " + ex.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Unexpected booking error: " + e.getMessage());
            showAlert("Error", "Unexpected error: " + e.getMessage());
        }
    }

    private String generateFlightCode(String origin, String destination) {
        return "BA" + origin + destination + dateComboBox.getValue().replace("-", "").substring(2);
    }

    private void clearBookingForm() {
        Platform.runLater(() -> {
            originComboBox.setValue(null);
            destinationComboBox.setValue(null);
            dateComboBox.setValue(null);
            if (passengerSpinner.getValueFactory() != null)
                passengerSpinner.getValueFactory().setValue(1);
            resetPriceDisplay();
        });
    }

    // ---------------- NAVIGATION HANDLERS ----------------

    /**
     * Helper to load a new FXML file and replace the current scene.
     */
    private void loadAndSwitchScene(String fxmlPath, String title, Object controllerInstance) {
        try {
            // Use one of the buttons to get the current stage
            Stage stage = (Stage) btn_dashboard.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // If a controller instance is passed, set it. Otherwise, rely on FXML to create one.
            if (controllerInstance != null) {
                loader.setController(controllerInstance);
            }

            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Bokamoso Airlines - " + title);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Unable to load " + title + " view: " + e.getMessage());
        } catch (NullPointerException e) {
            // Handle case where button might be null during testing, or scene is not attached.
            e.printStackTrace();
            showAlert("Navigation Error", "Error accessing the main window. Ensure the FXML is correctly loaded.");
        }
    }

    /**
     * Navigates back to the main Dashboard view.
     */
    @FXML
    private void handleViewDashboard() {
        // Since this controller IS the Dashboard, reloading it essentially means
        // replacing the current scene with the dashboard FXML again.
        // We assume Dashboard.fxml is located in the same package root as the controllers.
        loadAndSwitchScene("/groupassingment/airlinesreservations/Dashboard.fxml", "Dashboard", null);
        // Note: For a real app, you would swap a pane, not the entire scene,
        // but for this structure, scene replacement is the most reliable method.
    }

    /**
     * Navigates to the New Reservation form and passes session data.
     */
    @FXML
    private void handleViewReservation() {
        if (currentUserEmail == null) {
            showAlert("Login Required", "Please log in to make a new reservation.");
            return;
        }

        try {
            Stage stage = (Stage) btn_reservation.getScene().getWindow();

            // Load the FXML using the user-specified file name: Reservation-Form.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Reservation-Form.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the session data
            ReservationFormController reservationController = loader.getController();

            // NOTE: The token is already trimmed in initializeUserData() of this DashboardController,
            // but we pass the clean token on to the next controller.
            reservationController.initializeSessionData(currentUserToken, currentUserId, currentUserEmail);

            // Switch the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Bokamoso Airlines - New Reservation");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Unable to load New Reservation view. Check that Reservation-Form.fxml exists: " + e.getMessage());
        }
    }

    /**
     * Placeholder for Manage Reservations view.
     */
    @FXML
    private void handleViewManageReservations() {
        loadAndSwitchScene("/groupassingment/airlinesreservations/ManageReservations.fxml", "Manage Reservations", null);
    }

    /**
     * Placeholder for Feedback view.
     */
    @FXML
    private void handleViewFeedback() {
        showAlert("Feature Coming Soon", "The Feedback section is where you can share your experience with Bokamoso Airlines.");
        loadAndSwitchScene("/groupassingment/airlinesreservations/Feedback.fxml", "Feedback", null);
    }

    /**
     * Placeholder for Support view.
     */
    @FXML
    private void handleViewSupport() {
        showAlert("Feature Coming Soon", "Access our support resources and contact us for assistance here.");
        loadAndSwitchScene("/groupassingment/airlinesreservations/Support.fxml", "Support", null);
    }

    /**
     * Placeholder for Settings view.
     */
    @FXML
    private void handleViewSettings() {
        showAlert("Feature Coming Soon", "Settings will allow you to update your profile and preferences.");
        loadAndSwitchScene("/groupassingment/airlinesreservations/Settings.fxml", "Settings", null);
    }

    @FXML
    private void handleReservationView() {
        // This old placeholder is replaced by handleViewManageReservations for consistency
        handleViewManageReservations();
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logging out user: " + currentUserEmail);
        String userName = currentUserName != null ? currentUserName : "Guest";
        this.currentUserEmail = null;
        this.currentUserId = null;
        this.currentUserName = null;
        this.currentUserToken = null;

        showDefaultState();
        clearBookingForm();

        // Redirect back to the login screen
        try {
            Stage stage = (Stage) (btn_dashboard != null ? btn_dashboard : bookFlightButton).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/groupassingment/airlinesreservations/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Bokamoso Airlines - Login");
            stage.centerOnScreen();

            showAlert("Goodbye!", "Logged out successfully!\n\nThank you for using Bokamoso Airlines, " + userName + "!");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Logout Error", "Unable to load login screen.");
        }
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