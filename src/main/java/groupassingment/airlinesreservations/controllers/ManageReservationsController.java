package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.CompletableFuture;

public class ManageReservationsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Label statusMessageLabel;
    @FXML private Label paginationInfo;
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private ProgressBar progressBar;

    @FXML private Button btnDashboard;
    @FXML private Button btnReservation;
    @FXML private Button btnManageReservations;
    @FXML private Button btnFeedback;
    @FXML private Button btnSupport;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;
    @FXML private Button btnRefresh;
    @FXML private Button btnExport;
    @FXML private Button searchButton;

    // Table columns
    @FXML private TableColumn<Reservation, String> colReservationCode;
    @FXML private TableColumn<Reservation, String> colCustomerName;
    @FXML private TableColumn<Reservation, String> colFlight;
    @FXML private TableColumn<Reservation, String> colDeparture;
    @FXML private TableColumn<Reservation, String> colDestination;
    @FXML private TableColumn<Reservation, String> colSeatClass;
    @FXML private TableColumn<Reservation, Double> colTotalFare;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, String> colPaymentStatus;
    @FXML private TableColumn<Reservation, String> colActions;

    private SupabaseService supabaseService;
    private String userAuthToken;
    private String userId;
    private String userEmail;
    private ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    // Reservation data model class
    public static class Reservation {
        private final Long reservationId;
        private final String reservationCode;
        private final String customerName;
        private final String flightCode;
        private final String departureCity;
        private final String destinationCity;
        private final String seatClass;
        private final Double totalFare;
        private final String reservationStatus;
        private final String paymentStatus;

        public Reservation(Long reservationId, String reservationCode, String customerName,
                           String flightCode, String departureCity, String destinationCity,
                           String seatClass, Double totalFare, String reservationStatus,
                           String paymentStatus) {
            this.reservationId = reservationId;
            this.reservationCode = reservationCode;
            this.customerName = customerName;
            this.flightCode = flightCode;
            this.departureCity = departureCity;
            this.destinationCity = destinationCity;
            this.seatClass = seatClass;
            this.totalFare = totalFare;
            this.reservationStatus = reservationStatus;
            this.paymentStatus = paymentStatus;
        }

        // Getters
        public Long getReservationId() { return reservationId; }
        public String getReservationCode() { return reservationCode; }
        public String getCustomerName() { return customerName; }
        public String getFlightCode() { return flightCode; }
        public String getDepartureCity() { return departureCity; }
        public String getDestinationCity() { return destinationCity; }
        public String getSeatClass() { return seatClass; }
        public Double getTotalFare() { return totalFare; }
        public String getReservationStatus() { return reservationStatus; }
        public String getPaymentStatus() { return paymentStatus; }
    }

    @FXML
    public void initialize() {
        supabaseService = new SupabaseService();
        setupFilters();
        setupTable();
        setupEventHandlers();
    }

    public void initializeSessionData(String authToken, String userId, String userEmail) {
        this.userAuthToken = authToken;
        this.userId = userId;
        this.userEmail = userEmail;

        System.out.println("ManageReservations: Session initialized for " + userEmail);
        loadReservations();
    }

    private void setupFilters() {
        // Status filter options
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "All Status", "Pending", "Confirmed", "Cancelled", "Waiting"
        );
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Status");

        // Page size options
        ObservableList<String> pageSizes = FXCollections.observableArrayList("10", "25", "50", "100");
        pageSizeCombo.setItems(pageSizes);
        pageSizeCombo.setValue("10");

        // Add listener for filter changes
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadReservations());
        pageSizeCombo.valueProperty().addListener((obs, oldVal, newVal) -> loadReservations());
    }

    private void setupTable() {
        // Configure table columns
        colReservationCode.setCellValueFactory(new PropertyValueFactory<>("reservationCode"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colFlight.setCellValueFactory(new PropertyValueFactory<>("flightCode"));
        colDeparture.setCellValueFactory(new PropertyValueFactory<>("departureCity"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destinationCity"));
        colSeatClass.setCellValueFactory(new PropertyValueFactory<>("seatClass"));
        colTotalFare.setCellValueFactory(new PropertyValueFactory<>("totalFare"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        // Format total fare column
        colTotalFare.setCellFactory(column -> new TableCell<Reservation, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("R %.2f", item));
                }
            }
        });

        // Status column with styled cells
        colStatus.setCellFactory(column -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "confirmed":
                            setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 4 8;");
                            break;
                        case "pending":
                            setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 4 8;");
                            break;
                        case "cancelled":
                            setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 4 8;");
                            break;
                        case "waiting":
                            setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 4 8;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Setup action buttons column
        setupActionButtons();

        reservationsTable.setItems(reservations);
    }

    private void setupActionButtons() {
        colActions.setCellFactory(new Callback<TableColumn<Reservation, String>, TableCell<Reservation, String>>() {
            @Override
            public TableCell<Reservation, String> call(TableColumn<Reservation, String> param) {
                return new TableCell<Reservation, String>() {
                    private final HBox buttonContainer = new HBox(5);
                    private final Button viewBtn = new Button("👁");
                    private final Button editBtn = new Button("✏");
                    private final Button cancelBtn = new Button("❌");
                    private final Button printBtn = new Button("🖨");

                    {
                        buttonContainer.setAlignment(Pos.CENTER);

                        // Style buttons
                        viewBtn.getStyleClass().add("table-action-button");
                        viewBtn.getStyleClass().add("view-button");
                        viewBtn.setTooltip(new Tooltip("View Details"));

                        editBtn.getStyleClass().add("table-action-button");
                        editBtn.getStyleClass().add("edit-button");
                        editBtn.setTooltip(new Tooltip("Edit Reservation"));

                        cancelBtn.getStyleClass().add("table-action-button");
                        cancelBtn.getStyleClass().add("cancel-button");
                        cancelBtn.setTooltip(new Tooltip("Cancel Reservation"));

                        printBtn.getStyleClass().add("table-action-button");
                        printBtn.getStyleClass().add("print-button");
                        printBtn.setTooltip(new Tooltip("Print Ticket"));

                        // Add buttons to container
                        buttonContainer.getChildren().addAll(viewBtn, editBtn, cancelBtn, printBtn);

                        // Set button actions
                        viewBtn.setOnAction(event -> {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            viewReservation(reservation);
                        });

                        editBtn.setOnAction(event -> {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            editReservation(reservation);
                        });

                        cancelBtn.setOnAction(event -> {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            cancelReservation(reservation);
                        });

                        printBtn.setOnAction(event -> {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            printTicket(reservation);
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            cancelBtn.setDisable("Cancelled".equals(reservation.getReservationStatus()));
                            editBtn.setDisable("Cancelled".equals(reservation.getReservationStatus()));
                            setGraphic(buttonContainer);
                        }
                    }
                };
            }
        });
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(event -> handleSearch());
        btnRefresh.setOnAction(event -> handleRefresh());
        btnExport.setOnAction(event -> handleExport());

        // Navigation handlers
        btnDashboard.setOnAction(event -> navigateToDashboard());
        btnReservation.setOnAction(event -> navigateToReservation());
        btnManageReservations.setOnAction(event -> navigateToManageReservations());
        btnFeedback.setOnAction(event -> navigateToFeedback());
        btnSupport.setOnAction(event -> navigateToSupport());
        btnSettings.setOnAction(event -> navigateToSettings());
        btnLogout.setOnAction(event -> handleLogout());
    }

    private void loadReservations() {
        showLoading(true);
        updateStatus("Loading reservations...", "info");

        supabaseService.getCustomerByEmail(userEmail, userAuthToken)
                .thenCompose(customerResponse -> {
                    if (customerResponse.statusCode() == 200) {
                        try {
                            JSONArray customers = new JSONArray(customerResponse.body());
                            if (customers.length() > 0) {
                                JSONObject customer = customers.getJSONObject(0);
                                Long customerId = customer.getLong("customer_id");
                                String customerName = customer.optString("full_name", "Unknown");

                                // Load reservations for this customer
                                return supabaseService.getReservationsByCustomerId(customerId, userAuthToken)
                                        .thenCompose(reservationsResponse -> {
                                            if (reservationsResponse != null && reservationsResponse.statusCode() == 200) {
                                                try {
                                                    JSONArray reservationsArray = new JSONArray(reservationsResponse.body());
                                                    CompletableFuture<Void> allFlightDetails = CompletableFuture.completedFuture(null);

                                                    // Fetch flight details for each reservation
                                                    for (int i = 0; i < reservationsArray.length(); i++) {
                                                        JSONObject reservationJson = reservationsArray.getJSONObject(i);
                                                        if (reservationJson.has("flight_id") && !reservationJson.isNull("flight_id")) {
                                                            int flightId = reservationJson.getInt("flight_id");
                                                            final int index = i;
                                                            allFlightDetails = allFlightDetails.thenCompose(v ->
                                                                    supabaseService.getFlightById(flightId, userAuthToken)
                                                                            .thenAccept(flightResponse -> {
                                                                                if (flightResponse != null && flightResponse.statusCode() == 200) {
                                                                                    try {
                                                                                        JSONArray flights = new JSONArray(flightResponse.body());
                                                                                        if (flights.length() > 0) {
                                                                                            JSONObject flight = flights.getJSONObject(0);
                                                                                            // Add flight data to reservation JSON
                                                                                            reservationJson.put("flight_code", flight.optString("flight_code", "FL" + flightId));
                                                                                            reservationJson.put("departure_city", flight.optString("departure_city", "N/A"));
                                                                                            reservationJson.put("destination_city", flight.optString("destination_city", "N/A"));
                                                                                        }
                                                                                    } catch (Exception e) {
                                                                                        System.err.println("Error parsing flight data: " + e.getMessage());
                                                                                    }
                                                                                }
                                                                            })
                                                            );
                                                        }
                                                        // Add customer name to each reservation
                                                        reservationJson.put("customer_name", customerName);
                                                    }
                                                    return allFlightDetails.thenApply(v -> reservationsResponse);
                                                } catch (Exception e) {
                                                    System.err.println("Error processing reservations: " + e.getMessage());
                                                }
                                            }
                                            return CompletableFuture.completedFuture(reservationsResponse);
                                        });
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing customer data: " + e.getMessage());
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenAccept(reservationsResponse -> {
                    Platform.runLater(() -> {
                        showLoading(false);

                        if (reservationsResponse != null && reservationsResponse.statusCode() == 200) {
                            try {
                                JSONArray reservationsArray = new JSONArray(reservationsResponse.body());
                                reservations.clear();

                                for (int i = 0; i < reservationsArray.length(); i++) {
                                    JSONObject reservationJson = reservationsArray.getJSONObject(i);

                                    // Apply filters
                                    String statusFilterValue = statusFilter.getValue();
                                    if (!"All Status".equals(statusFilterValue)) {
                                        String reservationStatus = reservationJson.optString("reservation_status", "");
                                        if (!statusFilterValue.equals(reservationStatus)) {
                                            continue;
                                        }
                                    }

                                    // Apply search filter
                                    String searchTerm = searchField.getText().toLowerCase();
                                    if (!searchTerm.isEmpty()) {
                                        String reservationCode = reservationJson.optString("reservation_code", "").toLowerCase();
                                        String customerName = reservationJson.optString("customer_name", "").toLowerCase();
                                        if (!reservationCode.contains(searchTerm) && !customerName.contains(searchTerm)) {
                                            continue;
                                        }
                                    }

                                    // Extract data
                                    String extractedCustomerName = reservationJson.optString("customer_name", "Unknown");
                                    String flightCode = reservationJson.optString("flight_code", "N/A");
                                    String departureCity = reservationJson.optString("departure_city", "N/A");
                                    String destinationCity = reservationJson.optString("destination_city", "N/A");

                                    System.out.println("Reservation " + i + ": " +
                                            "Customer: " + extractedCustomerName +
                                            ", Flight: " + flightCode +
                                            ", Departure: " + departureCity +
                                            ", Destination: " + destinationCity);

                                    Reservation reservation = new Reservation(
                                            reservationJson.getLong("reservation_id"),
                                            reservationJson.optString("reservation_code", "N/A"),
                                            extractedCustomerName,
                                            flightCode,
                                            departureCity,
                                            destinationCity,
                                            reservationJson.optString("seat_class", "Economic"),
                                            reservationJson.optDouble("total_fare", 0.0),
                                            reservationJson.optString("reservation_status", "Pending"),
                                            reservationJson.optString("payment_status", "Pending")
                                    );

                                    reservations.add(reservation);
                                }

                                updatePaginationInfo();
                                updateStatus("Loaded " + reservations.size() + " reservations", "success");

                            } catch (Exception e) {
                                System.err.println("Error parsing reservations: " + e.getMessage());
                                updateStatus("Error loading reservations", "error");
                            }
                        } else {
                            updateStatus("Failed to load reservations", "error");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        updateStatus("Error: " + ex.getMessage(), "error");
                        System.err.println("Error loading reservations: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleSearch() {
        loadReservations();
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        statusFilter.setValue("All Status");
        loadReservations();
    }

    @FXML
    private void handleExport() {
        StringBuilder exportData = new StringBuilder();
        exportData.append("Reservation Code,Customer Name,Flight,Departure,Destination,Class,Total Fare,Status,Payment Status\n");

        for (Reservation reservation : reservations) {
            exportData.append(String.format("%s,%s,%s,%s,%s,%s,R %.2f,%s,%s\n",
                    reservation.getReservationCode(),
                    reservation.getCustomerName(),
                    reservation.getFlightCode(),
                    reservation.getDepartureCity(),
                    reservation.getDestinationCity(),
                    reservation.getSeatClass(),
                    reservation.getTotalFare(),
                    reservation.getReservationStatus(),
                    reservation.getPaymentStatus()
            ));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Reservations Export");
        alert.setContentText("Data ready for export:\n\n" + exportData.toString().substring(0, Math.min(500, exportData.length())) + "\n\n(Full data available in console)");
        alert.showAndWait();

        System.out.println("EXPORT DATA:\n" + exportData.toString());
        updateStatus("Data exported to console", "info");
    }

    private void viewReservation(Reservation reservation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservation Details");
        alert.setHeaderText("Reservation: " + reservation.getReservationCode());
        alert.setContentText(String.format(
                "Customer: %s\nFlight: %s\nRoute: %s → %s\nClass: %s\nTotal Fare: R %.2f\nStatus: %s\nPayment: %s",
                reservation.getCustomerName(),
                reservation.getFlightCode(),
                reservation.getDepartureCity(),
                reservation.getDestinationCity(),
                reservation.getSeatClass(),
                reservation.getTotalFare(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus()
        ));
        alert.showAndWait();
    }

    private void editReservation(Reservation reservation) {
        TextInputDialog dialog = new TextInputDialog(reservation.getSeatClass());
        dialog.setTitle("Edit Reservation");
        dialog.setHeaderText("Edit Seat Class for " + reservation.getReservationCode());
        dialog.setContentText("Enter new seat class:");

        dialog.showAndWait().ifPresent(newSeatClass -> {
            updateStatus("Updating reservation...", "info");
            updateStatus("Reservation updated successfully", "success");
        });
    }

    private void cancelReservation(Reservation reservation) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Reservation");
        confirmAlert.setHeaderText("Cancel Reservation: " + reservation.getReservationCode());
        confirmAlert.setContentText("Are you sure you want to cancel this reservation? This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showLoading(true);
                updateStatus("Cancelling reservation...", "info");

                JSONObject updateData = new JSONObject();
                updateData.put("reservation_status", "Cancelled");
                updateData.put("payment_status", "Refunded");

                supabaseService.updateReservation(reservation.getReservationId().intValue(), updateData, userAuthToken)
                        .thenAccept(updateResponse -> {
                            Platform.runLater(() -> {
                                showLoading(false);
                                if (updateResponse.statusCode() == 200) {
                                    updateStatus("Reservation cancelled successfully", "success");
                                    loadReservations();
                                } else {
                                    updateStatus("Failed to cancel reservation", "error");
                                }
                            });
                        })
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                showLoading(false);
                                updateStatus("Error cancelling reservation: " + ex.getMessage(), "error");
                            });
                            return null;
                        });
            }
        });
    }

    private void printTicket(Reservation reservation) {
        updateStatus("Generating ticket for " + reservation.getReservationCode() + "...", "info");
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            updateStatus("Ticket printed successfully for " + reservation.getReservationCode(), "success");
                        });
                    }
                },
                2000
        );
    }

    // ==================== NAVIGATION METHODS ====================

    private void navigateToDashboard() {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        SceneManager.navigateToDashboard(btnDashboard.getScene());
    }

    private void navigateToReservation() {
        System.out.println("=== NAVIGATING TO RESERVATION FORM ===");
        SceneManager.navigateToReservationForm(btnReservation.getScene());
    }

    private void navigateToManageReservations() {
        System.out.println("=== REFRESHING MANAGE RESERVATIONS ===");
        loadReservations();
    }

    private void navigateToFeedback() {
        System.out.println("=== NAVIGATING TO FEEDBACK ===");
        SceneManager.navigateToFeedback(btnFeedback.getScene());
    }

    private void navigateToSupport() {
        System.out.println("=== NAVIGATING TO SUPPORT ===");
        SceneManager.navigateToSupport(btnSupport.getScene());
    }

    private void navigateToSettings() {
        System.out.println("=== NAVIGATING TO SETTINGS ===");
        SceneManager.navigateToSettings(btnSettings.getScene());
    }

    private void handleLogout() {
        System.out.println("=== HANDLING LOGOUT ===");
        SceneManager.handleLogout(btnLogout.getScene());
    }

    private void showLoading(boolean show) {
        progressBar.setVisible(show);
        btnRefresh.setDisable(show);
        btnExport.setDisable(show);
        searchButton.setDisable(show);
    }

    private void updateStatus(String message, String type) {
        statusMessageLabel.setText(message);
        statusMessageLabel.getStyleClass().removeAll("success", "error", "warning", "info");
        statusMessageLabel.getStyleClass().add(type);
    }

    private void updatePaginationInfo() {
        int total = reservations.size();
        paginationInfo.setText(String.format("Showing %d reservations", total));
    }
}