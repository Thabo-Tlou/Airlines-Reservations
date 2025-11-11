package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private ComboBox<String> originComboBox;
    @FXML private ComboBox<String> destinationComboBox;
    @FXML private ComboBox<String> dateComboBox;
    @FXML private Spinner<Integer> passengerSpinner;
    @FXML private Button bookFlightButton;
    @FXML private TextField searchField;

    private String currentUserEmail;
    private String currentUserId;
    private SupabaseService supabaseService;

    public DashboardController() {
        this.supabaseService = new SupabaseService();
    }

    @FXML
    public void initialize() {
        setupEventHandlers();
        showDefaultState();
        initializeDateOptions();
        loadAirportsFromSupabase();
    }

    private void setupEventHandlers() {
        bookFlightButton.setOnAction(event -> handleBookFlight());
    }

    private void showDefaultState() {
        welcomeLabel.setText("Welcome!");
        profileNameLabel.setText("Guest User");
        profileEmailLabel.setText("Please log in");
    }

    private void initializeDateOptions() {
        dateComboBox.getItems().addAll("2025-11-20", "2025-11-21", "2025-11-22", "2025-11-23", "2025-11-24");
    }

    // ---------------- LOAD DATA ----------------

    private void loadAirportsFromSupabase() {
        supabaseService.getAirports()
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JSONArray airports = new JSONArray(response.body());
                            Platform.runLater(() -> {
                                originComboBox.getItems().clear();
                                destinationComboBox.getItems().clear();
                                for (int i = 0; i < airports.length(); i++) {
                                    JSONObject airport = airports.getJSONObject(i);
                                    String code = airport.optString("code");
                                    String name = airport.optString("name");
                                    originComboBox.getItems().add(code + " - " + name);
                                    destinationComboBox.getItems().add(code + " - " + name);
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Failed to parse airports: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Error fetching airports: " + response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Network error loading airports: " + ex.getMessage());
                    return null;
                });
    }

    // ---------------- USER SETUP ----------------

    public void initializeUserData(String userEmail, String userId) {
        this.currentUserEmail = userEmail;
        this.currentUserId = userId;
        loadUserProfileData();
    }

    private void loadUserProfileData() {
        supabaseService.getPassengerByEmail(currentUserEmail)
                .thenAccept(response -> {
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
        String[] nameParts = extractNamesFromEmail(currentUserEmail);
        supabaseService.insertPassenger(nameParts[0], nameParts[1], currentUserEmail, "Not provided")
                .thenAccept(response -> {
                    if (response.statusCode() == 201) {
                        loadUserProfileData();
                    } else {
                        useAuthUserData();
                    }
                })
                .exceptionally(ex -> {
                    useAuthUserData();
                    return null;
                });
    }

    private void updateUIWithUserData(JSONObject userData) {
        String firstName = userData.optString("first_name", "User");
        String lastName = userData.optString("last_name", "");
        String email = userData.optString("email", currentUserEmail);
        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome Mr " + firstName + "!");
            profileNameLabel.setText(firstName + " " + lastName);
            profileEmailLabel.setText(email);
        });
    }

    private void useAuthUserData() {
        String nameFromEmail = extractNameFromEmail(currentUserEmail);
        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome " + nameFromEmail + "!");
            profileNameLabel.setText(nameFromEmail);
            profileEmailLabel.setText(currentUserEmail);
        });
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
        try {
            String origin = originComboBox.getValue();
            String destination = destinationComboBox.getValue();
            String date = dateComboBox.getValue();
            Integer passengers = passengerSpinner.getValue();

            if (origin == null || destination == null || date == null) {
                showAlert("Error", "Please fill in all flight details");
                return;
            }
            if (origin.equals(destination)) {
                showAlert("Error", "Origin and destination cannot be the same");
                return;
            }

            JSONObject bookingData = new JSONObject();
            bookingData.put("user_email", currentUserEmail);
            bookingData.put("user_id", currentUserId);
            bookingData.put("origin", origin);
            bookingData.put("destination", destination);
            bookingData.put("flight_date", date);
            bookingData.put("passengers", passengers);
            bookingData.put("status", "confirmed");

            supabaseService.insertFlightBooking(bookingData)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 201) {
                            showAlert("Success", "Flight booked successfully!");
                            clearBookingForm();
                        } else {
                            showAlert("Error", "Failed to save booking.");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert("Error", "Network error."));
                        return null;
                    });
        } catch (Exception e) {
            showAlert("Error", "Unexpected error: " + e.getMessage());
        }
    }

    private void clearBookingForm() {
        Platform.runLater(() -> {
            originComboBox.setValue(null);
            destinationComboBox.setValue(null);
            dateComboBox.setValue(null);
            if (passengerSpinner.getValueFactory() != null)
                passengerSpinner.getValueFactory().setValue(1);
        });
    }

    @FXML
    private void handleLogout() {
        this.currentUserEmail = null;
        this.currentUserId = null;
        showDefaultState();
        clearBookingForm();
        showAlert("Success", "Logged out successfully!");
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
