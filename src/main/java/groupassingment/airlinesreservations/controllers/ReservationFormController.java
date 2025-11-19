package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
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

    private SupabaseService supabaseService;
    private String userAuthToken;
    private String userId;
    private String userEmail;
    private Long customerId;

    // Store loaded data
    private ObservableList<String> airportsList = FXCollections.observableArrayList();
    private ObservableList<String> fareCategoriesList = FXCollections.observableArrayList();

    // Initialize method called by FXML loader
    public void initialize() {
        supabaseService = new SupabaseService();
        setupFormDefaults();
        setupEventHandlers();

        // Load dynamic data from Supabase
        loadAirports();
        loadFareCategories();

        // Debug: Test connection and session
        debugSessionInfo();
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
        System.out.println("Auth Token: " + (authToken != null ? "Present (" + authToken.substring(0, Math.min(20, authToken.length())) + "...)" : "NULL"));
        System.out.println("User ID: " + userId);
        System.out.println("User Email: " + userEmail);
        System.out.println("=== DEBUG END ===");

        // Pre-fill customer email and load customer data
        prefillCustomerData();
    }

    private void setupFormDefaults() {
        // Setup passenger count comboboxes
        ObservableList<String> passengerCounts = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5", "6");
        combo_adults.setItems(passengerCounts);
        combo_children.setItems(passengerCounts);
        combo_infants.setItems(passengerCounts);
        combo_adults.setValue("1");
        combo_children.setValue("0");
        combo_infants.setValue("0");

        // Setup trip types
        ObservableList<String> tripTypes = FXCollections.observableArrayList("One-way", "Round-trip");
        combo_triptype.setItems(tripTypes);
        combo_triptype.setValue("One-way");

        // Setup seat classes
        ObservableList<String> seatClasses = FXCollections.observableArrayList("Economic", "Business");
        combo_seatclass.setItems(seatClasses);
        combo_seatclass.setValue("Economic");

        // Setup seat preferences
        ObservableList<String> seatPrefs = FXCollections.observableArrayList("Window", "Aisle", "Middle");
        combo_seatpref.setItems(seatPrefs);
        combo_seatpref.setValue("Window");

        // Setup payment methods
        ObservableList<String> paymentMethods = FXCollections.observableArrayList(
                "Credit Card", "Debit Card", "Bank Transfer"
        );
        combo_paymentmethod.setItems(paymentMethods);
        combo_paymentmethod.setValue("Credit Card");

        // Set today's date as default
        dp_traveldate.setValue(LocalDate.now());

        // Auto-generate flight code
        generateFlightCode();

        // Initialize empty comboboxes (will be populated from Supabase)
        combo_travelfrom.setItems(airportsList);
        combo_travelto.setItems(airportsList);
        combo_categoryfare.setItems(fareCategoriesList);
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
    }

    private void loadAirports() {
        updateStatus("Loading airports...", true);

        supabaseService.getAirports()
                .thenAccept(response -> {
                    System.out.println("=== AIRPORTS API RESPONSE ===");
                    System.out.println("Status: " + response.statusCode());
                    System.out.println("Body: " + response.body());

                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        if (responseBody != null && !responseBody.trim().isEmpty()) {
                            try {
                                JSONArray airports = new JSONArray(responseBody);
                                Platform.runLater(() -> {
                                    airportsList.clear();
                                    for (int i = 0; i < airports.length(); i++) {
                                        JSONObject airport = airports.getJSONObject(i);
                                        String airportCode = airport.optString("airport_code", "");
                                        String airportName = airport.optString("airport_name", "");
                                        String city = airport.optString("city", "");
                                        String country = airport.optString("country", "");

                                        String displayText = String.format("%s (%s) - %s, %s",
                                                airportName, airportCode, city, country);
                                        airportsList.add(displayText);
                                    }

                                    // Set the combobox items
                                    combo_travelfrom.setItems(airportsList);
                                    combo_travelto.setItems(airportsList);

                                    if (!airportsList.isEmpty()) {
                                        combo_travelfrom.setValue(airportsList.get(0));
                                        if (airportsList.size() > 1) {
                                            combo_travelto.setValue(airportsList.get(1));
                                        }
                                    }

                                    updateStatus("Airports loaded successfully", false);
                                    System.out.println("Success: Loaded " + airportsList.size() + " airports");
                                });
                            } catch (Exception e) {
                                System.err.println("Error parsing airports data: " + e.getMessage());
                                Platform.runLater(() -> {
                                    updateStatus("Error parsing airports data", false);
                                });
                            }
                        }
                    } else {
                        System.err.println("Failed to load airports: " + response.body());
                        Platform.runLater(() -> {
                            updateStatus("Failed to load airports", false);
                        });
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception loading airports: " + ex.getMessage());
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        updateStatus("Error loading airports", false);
                    });
                    return null;
                });
    }

    private void loadFareCategories() {
        System.out.println("=== LOADING FARE CATEGORIES ===");

        supabaseService.getAllFareCategories()
                .thenAccept(response -> {
                    System.out.println("Fare Categories Response - Status: " + response.statusCode());
                    System.out.println("Fare Categories Response - Body: " + response.body());

                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        if (responseBody != null && !responseBody.trim().isEmpty()) {
                            try {
                                JSONArray categories = new JSONArray(responseBody);
                                Platform.runLater(() -> {
                                    fareCategoriesList.clear();
                                    for (int i = 0; i < categories.length(); i++) {
                                        JSONObject category = categories.getJSONObject(i);
                                        String categoryName = category.optString("category_name", "");
                                        double multiplier = category.optDouble("fare_multiplier", 1.0);
                                        String description = category.optString("description", "");

                                        String displayText = String.format("%s (%.0f%% discount)",
                                                categoryName, (1 - multiplier) * 100);
                                        fareCategoriesList.add(displayText);
                                    }

                                    combo_categoryfare.setItems(fareCategoriesList);
                                    if (!fareCategoriesList.isEmpty()) {
                                        combo_categoryfare.setValue(fareCategoriesList.get(0));
                                    }

                                    System.out.println("Success: Loaded " + fareCategoriesList.size() + " fare categories");
                                });
                            } catch (Exception e) {
                                System.err.println("Error parsing fare categories: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.err.println("Failed to load fare categories: " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception loading fare categories: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private void prefillCustomerData() {
        if (userEmail != null) {
            txt_enteremail.setText(userEmail);

            // Load customer data from Supabase
            loadCustomerFromSupabase();
        }
    }

    private void loadCustomerFromSupabase() {
        if (userAuthToken == null || userEmail == null) {
            System.err.println("Cannot load customer: Auth token or email is null");
            return;
        }

        System.out.println("=== LOADING CUSTOMER DATA ===");
        System.out.println("Loading customer for email: " + userEmail);

        supabaseService.getCustomerByEmail(userEmail, userAuthToken)
                .thenAccept(response -> {
                    System.out.println("Customer Data Response - Status: " + response.statusCode());
                    System.out.println("Customer Data Response - Body: " + response.body());

                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        if (responseBody != null && !responseBody.trim().isEmpty()) {
                            try {
                                JSONArray customers = new JSONArray(responseBody);
                                if (customers.length() > 0) {
                                    JSONObject customer = customers.getJSONObject(0);
                                    Platform.runLater(() -> {
                                        // Pre-fill form with existing customer data
                                        txt_entername.setText(customer.optString("full_name", ""));
                                        txt_idnumber.setText(customer.optString("id_number", ""));
                                        txt_enteraddress.setText(customer.optString("address", ""));
                                        txt_enterphone.setText(customer.optString("phone", ""));

                                        String customerCategory = customer.optString("category_fare", "Regular");
                                        // Find and set the corresponding display text
                                        for (String displayText : fareCategoriesList) {
                                            if (displayText.contains(customerCategory)) {
                                                combo_categoryfare.setValue(displayText);
                                                break;
                                            }
                                        }

                                        // Store customer ID for reservation
                                        customerId = customer.optLong("customer_id");
                                        SessionManager.setCustomerData(customerId, customerCategory);

                                        System.out.println("Success: Customer data loaded - ID: " + customerId + ", Category: " + customerCategory);
                                    });
                                } else {
                                    System.out.println("No existing customer found for email: " + userEmail);
                                }
                            } catch (Exception e) {
                                System.err.println("Error parsing customer data: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.err.println("Failed to load customer data: " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception loading customer data: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private void generateFlightCode() {
        String code = "BA" + System.currentTimeMillis() % 10000;
        txt_enterflightcode.setText(code);
    }

    private void calculateTotalFare() {
        try {
            // Base fare calculation
            double baseFare = "Business".equals(combo_seatclass.getValue()) ? 5000.00 : 2000.00;

            // Passenger count
            int adults = Integer.parseInt(combo_adults.getValue());
            int children = Integer.parseInt(combo_children.getValue());
            int infants = Integer.parseInt(combo_infants.getValue());

            // Get fare multiplier from selected category
            double multiplier = 1.0;
            String selectedCategory = combo_categoryfare.getValue();
            if (selectedCategory != null) {
                // Extract multiplier from display text (e.g., "Student (15% discount)" -> 0.85)
                if (selectedCategory.contains("Student")) multiplier = 0.85;
                else if (selectedCategory.contains("Senior")) multiplier = 0.80;
                else if (selectedCategory.contains("Child")) multiplier = 0.60;
                else if (selectedCategory.contains("Infant")) multiplier = 0.10;
            }

            double total = (adults * baseFare * multiplier) +
                    (children * baseFare * multiplier * 0.6) +
                    (infants * baseFare * multiplier * 0.1);

            // Add taxes and fees (14% tax + fixed fees)
            total += total * 0.14; // Tax
            total += 75.00; // Airport tax + fuel surcharge

            txt_totalfare.setText(String.format("R %.2f", total));
        } catch (Exception e) {
            System.err.println("Error calculating fare: " + e.getMessage());
        }
    }

    @FXML
    private void submit_form() {
        System.out.println("=== SUBMIT FORM TRIGGERED ===");

        if (!validateForm()) {
            return;
        }

        if (customerId == null) {
            System.out.println("Customer ID is null, creating customer first...");
            createCustomerAndThenReservation();
        } else {
            System.out.println("Customer ID exists: " + customerId + ", creating reservation...");
            createReservation();
        }
    }

    private boolean validateForm() {
        System.out.println("=== VALIDATING FORM ===");

        if (txt_entername.getText().trim().isEmpty()) {
            System.err.println("Validation failed: Full name is empty");
            showAlert("Validation Error", "Please enter your full name.");
            return false;
        }
        if (txt_idnumber.getText().trim().isEmpty()) {
            System.err.println("Validation failed: ID number is empty");
            showAlert("Validation Error", "Please enter your ID number.");
            return false;
        }
        if (combo_travelfrom.getValue() == null || combo_travelto.getValue() == null) {
            System.err.println("Validation failed: Airports not selected");
            showAlert("Validation Error", "Please select departure and destination airports.");
            return false;
        }
        if (combo_travelfrom.getValue().equals(combo_travelto.getValue())) {
            System.err.println("Validation failed: Same departure and destination");
            showAlert("Validation Error", "Departure and destination airports cannot be the same.");
            return false;
        }
        if (!chk_terms.isSelected()) {
            System.err.println("Validation failed: Terms not accepted");
            showAlert("Validation Error", "Please agree to the terms and conditions.");
            return false;
        }

        System.out.println("Form validation passed");
        return true;
    }

    private void createCustomerAndThenReservation() {
        System.out.println("=== CREATE CUSTOMER AND RESERVATION ===");

        // Extract actual category name from display text
        String displayCategory = combo_categoryfare.getValue();
        String actualCategory = "Regular"; // default
        if (displayCategory != null) {
            if (displayCategory.contains("Student")) actualCategory = "Student";
            else if (displayCategory.contains("Senior")) actualCategory = "Senior";
            else if (displayCategory.contains("Child")) actualCategory = "Child";
            else if (displayCategory.contains("Infant")) actualCategory = "Infant";
        }

        System.out.println("Creating customer with:");
        System.out.println("  Name: " + txt_entername.getText().trim());
        System.out.println("  ID: " + txt_idnumber.getText().trim());
        System.out.println("  Email: " + txt_enteremail.getText().trim());
        System.out.println("  Category: " + actualCategory);

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
                    System.out.println("Proceeding to create reservation after customer creation...");
                    createReservation();
                });
            } else {
                Platform.runLater(() -> {
                    System.err.println("Failed to create customer profile");
                    updateStatus("Failed to create customer profile", false);
                    showAlert("Error", "Failed to create customer profile: " + response.body());
                });
            }
        }).exceptionally(ex -> {
            System.err.println("=== EXCEPTION DURING CUSTOMER CREATION ===");
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            Platform.runLater(() -> {
                updateStatus("Error creating customer", false);
                showAlert("Error", "Failed to create customer: " + ex.getMessage());
            });
            return null;
        });
    }

    private void createReservation() {
        updateStatus("Creating reservation...", true);

        System.out.println("=== DEBUG: CREATE RESERVATION START ===");
        System.out.println("Customer ID: " + customerId);
        System.out.println("Auth Token: " + (userAuthToken != null ? "Present" : "NULL"));
        System.out.println("User Email: " + userEmail);

        // Validate critical data
        if (customerId == null) {
            System.err.println("ERROR: Customer ID is null!");
            updateStatus("Error: Customer profile not found", false);
            showAlert("Error", "Customer profile not found. Please complete your profile first.");
            return;
        }

        if (userAuthToken == null) {
            System.err.println("ERROR: Auth token is null!");
            updateStatus("Error: Not authenticated", false);
            showAlert("Error", "Please log in again.");
            return;
        }

        try {
            // Extract data from form
            String fromAirport = extractAirportCode(combo_travelfrom.getValue());
            String toAirport = extractAirportCode(combo_travelto.getValue());
            String displayCategory = combo_categoryfare.getValue();
            String actualCategory = extractCategoryName(displayCategory);
            String fareText = txt_totalfare.getText().replace("R", "").trim();
            double totalFare = Double.parseDouble(fareText);

            System.out.println("Form Data:");
            System.out.println("  From: " + fromAirport + " (Original: " + combo_travelfrom.getValue() + ")");
            System.out.println("  To: " + toAirport + " (Original: " + combo_travelto.getValue() + ")");
            System.out.println("  Category: " + actualCategory + " (Display: " + displayCategory + ")");
            System.out.println("  Total Fare: " + totalFare);

            // Create reservation data with ALL required fields
            JSONObject reservationData = new JSONObject();

            // Required fields from your table schema
            reservationData.put("customer_id", customerId);
            reservationData.put("reservation_code", "RES" + System.currentTimeMillis() % 100000);
            reservationData.put("flight_id", 1); // You need to implement flight selection
            reservationData.put("seat_class", combo_seatclass.getValue());
            reservationData.put("trip_type", combo_triptype.getValue());
            reservationData.put("seat_preference", combo_seatpref.getValue());

            // Pricing fields (all required based on your schema)
            reservationData.put("base_fare", totalFare * 0.7);
            reservationData.put("category_discount", totalFare * 0.3);
            reservationData.put("seat_surcharge", 0.00);
            reservationData.put("tax_amount", totalFare * 0.14);
            reservationData.put("fuel_surcharge_amount", 50.00);
            reservationData.put("airport_tax_amount", 25.00);
            reservationData.put("total_fare", totalFare);

            // Status fields
            reservationData.put("reservation_status", "Pending");
            reservationData.put("payment_status", "Pending");
            reservationData.put("is_confirmed", false);
            reservationData.put("amount_paid", 0.00);

            // Debug: Print the complete request data
            System.out.println("=== REQUEST DATA ===");
            System.out.println(reservationData.toString(2));
            System.out.println("=== END REQUEST DATA ===");

            // Make the API call
            supabaseService.addReservation(reservationData, userAuthToken)
                    .thenAccept(response -> {
                        System.out.println("=== API RESPONSE ===");
                        System.out.println("Status Code: " + response.statusCode());
                        System.out.println("Response Body: " + response.body());
                        System.out.println("=== END API RESPONSE ===");

                        Platform.runLater(() -> {
                            if (response.statusCode() == 201) {
                                System.out.println("SUCCESS: Reservation created!");
                                updateStatus("Reservation created successfully!", false);
                                showAlert("Success", "Your flight has been reserved successfully!");

                                // Try to extract reservation ID from response
                                try {
                                    String responseBody = response.body();
                                    if (responseBody != null && !responseBody.trim().isEmpty()) {
                                        JSONArray responseArray = new JSONArray(responseBody);
                                        if (responseArray.length() > 0) {
                                            JSONObject createdReservation = responseArray.getJSONObject(0);
                                            Long reservationId = createdReservation.getLong("reservation_id");
                                            System.out.println("Created Reservation ID: " + reservationId);
                                            createPaymentRecord(reservationId);
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("Warning: Could not extract reservation ID from response: " + e.getMessage());
                                }
                            } else {
                                String errorBody = response.body();
                                System.err.println("ERROR: Reservation creation failed!");
                                System.err.println("Status: " + response.statusCode());
                                System.err.println("Full Error: " + errorBody);

                                // Parse the error for better understanding
                                try {
                                    JSONObject errorJson = new JSONObject(errorBody);
                                    String errorCode = errorJson.optString("code", "Unknown");
                                    String errorMessage = errorJson.optString("message", "Unknown error");
                                    String errorDetails = errorJson.optString("details", "No details");
                                    String errorHint = errorJson.optString("hint", "No hint");

                                    System.err.println("Parsed Error:");
                                    System.err.println("  Code: " + errorCode);
                                    System.err.println("  Message: " + errorMessage);
                                    System.err.println("  Details: " + errorDetails);
                                    System.err.println("  Hint: " + errorHint);

                                    updateStatus("Failed: " + errorCode, false);
                                    showAlert("Reservation Failed",
                                            "Error: " + errorCode + "\n" +
                                                    "Message: " + errorMessage + "\n" +
                                                    (errorHint != null && !errorHint.equals("No hint") ? "Hint: " + errorHint + "\n" : "") +
                                                    "Check console for full details.");

                                } catch (Exception parseError) {
                                    System.err.println("Could not parse error JSON: " + parseError.getMessage());
                                    updateStatus("Failed with status: " + response.statusCode(), false);
                                    showAlert("Reservation Failed",
                                            "Status: " + response.statusCode() + "\n" +
                                                    "Error: " + errorBody);
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("=== EXCEPTION DURING API CALL ===");
                        System.err.println("Exception: " + ex.getMessage());
                        ex.printStackTrace();
                        System.err.println("=== END EXCEPTION ===");

                        Platform.runLater(() -> {
                            updateStatus("Network error", false);
                            showAlert("Network Error",
                                    "Failed to connect to server:\n" +
                                            ex.getMessage() + "\n" +
                                            "Check console for details.");
                        });
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("=== EXCEPTION PREPARING DATA ===");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END EXCEPTION ===");

            Platform.runLater(() -> {
                updateStatus("Form error", false);
                showAlert("Form Error",
                        "Please check all fields:\n" +
                                e.getMessage() + "\n" +
                                "Check console for details.");
            });
        }
    }

    private void createPaymentRecord(Long reservationId) {
        System.out.println("=== CREATING PAYMENT FOR RESERVATION " + reservationId + " ===");

        try {
            JSONObject paymentData = new JSONObject();
            paymentData.put("reservation_id", reservationId);
            paymentData.put("payment_method", combo_paymentmethod.getValue());
            paymentData.put("amount", Double.parseDouble(txt_totalfare.getText().replace("R", "").trim()));
            paymentData.put("payment_status", "Completed");
            paymentData.put("payment_reference", "PAY" + System.currentTimeMillis() % 100000);

            System.out.println("Payment Data: " + paymentData.toString(2));

            // Uncomment when you're ready to implement payments
            // supabaseService.insertPayment(paymentData, userAuthToken)
            //     .thenAccept(response -> {
            //         System.out.println("Payment creation response: " + response.statusCode());
            //     });

        } catch (Exception e) {
            System.err.println("Error creating payment record: " + e.getMessage());
        }
    }

    private String extractAirportCode(String displayText) {
        if (displayText == null) return "";
        // Extract code from format: "Airport Name (CODE) - City, Country"
        int start = displayText.indexOf('(') + 1;
        int end = displayText.indexOf(')');
        if (start > 0 && end > start) {
            return displayText.substring(start, end);
        }
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

    @FXML
    private void clear_form() {
        System.out.println("=== CLEARING FORM ===");

        txt_entername.clear();
        txt_idnumber.clear();
        txt_enteraddress.clear();
        txt_enterphone.clear();

        combo_adults.setValue("1");
        combo_children.setValue("0");
        combo_infants.setValue("0");
        combo_triptype.setValue("One-way");
        combo_seatclass.setValue("Economic");
        combo_seatpref.setValue("Window");

        if (!airportsList.isEmpty()) {
            combo_travelfrom.setValue(airportsList.get(0));
            if (airportsList.size() > 1) {
                combo_travelto.setValue(airportsList.get(1));
            }
        }

        if (!fareCategoriesList.isEmpty()) {
            combo_categoryfare.setValue(fareCategoriesList.get(0));
        }

        generateFlightCode();
        calculateTotalFare();

        updateStatus("Form cleared", false);
        System.out.println("Form cleared successfully");
    }

    @FXML
    private void handlePrintTicket() {
        System.out.println("Print ticket requested");
        showAlert("Print Feature", "Ticket printing feature will be implemented soon!");
    }

    private void updateStatus(String message, boolean inProgress) {
        Platform.runLater(() -> {
            if (lbl_status != null) {
                lbl_status.setText(message);
            }
            if (progress_bar != null) {
                progress_bar.setVisible(inProgress);
            }
            System.out.println("Status Update: " + message + " [InProgress: " + inProgress + "]");
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
}