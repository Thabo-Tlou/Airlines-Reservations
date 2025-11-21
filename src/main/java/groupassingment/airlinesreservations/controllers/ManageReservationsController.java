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

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

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

    private int currentPage = 0;
    private int pageSize = 10;
    private String currentSearchTerm = "";

    // Cache for customer and flight data
    private Map<Long, String> customerNameCache = new HashMap<>();
    private Map<Long, JSONObject> flightCache = new HashMap<>();

    // Enhanced Reservation data model class
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
        private final String departureDate;
        private final String departureTime;
        private final Long flightId;
        private final Long customerId;

        public Reservation(Long reservationId, String reservationCode, String customerName,
                           String flightCode, String departureCity, String destinationCity,
                           String seatClass, Double totalFare, String reservationStatus,
                           String paymentStatus, String departureDate, String departureTime,
                           Long flightId, Long customerId) {
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
            this.departureDate = departureDate;
            this.departureTime = departureTime;
            this.flightId = flightId;
            this.customerId = customerId;
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
        public String getDepartureDate() { return departureDate; }
        public String getDepartureTime() { return departureTime; }
        public Long getFlightId() { return flightId; }
        public Long getCustomerId() { return customerId; }
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
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            loadReservations();
        });
        pageSizeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            pageSize = Integer.parseInt(newVal);
            currentPage = 0;
            loadReservations();
        });
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
                            // Disable actions for cancelled reservations
                            boolean isCancelled = "Cancelled".equals(reservation.getReservationStatus());
                            cancelBtn.setDisable(isCancelled);
                            editBtn.setDisable(isCancelled);
                            printBtn.setDisable(isCancelled);
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

        currentSearchTerm = searchField.getText().trim();

        // First, get all reservations
        CompletableFuture<HttpResponse<String>> reservationsFuture;

        if (currentSearchTerm.isEmpty()) {
            // Use getAllReservations if no search term
            reservationsFuture = supabaseService.getAllReservations(userAuthToken);
        } else {
            // Use fetchManagedReservations for search
            reservationsFuture = supabaseService.fetchManagedReservations(currentPage, pageSize, currentSearchTerm, userAuthToken);
        }

        reservationsFuture.thenCompose(response -> {
                    // Handle 206 status as success (Partial Content is still valid)
                    if (response.statusCode() == 200 || response.statusCode() == 206) {
                        try {
                            JSONArray reservationsArray = new JSONArray(response.body());
                            System.out.println("DEBUG: Found " + reservationsArray.length() + " reservations");

                            // Collect unique customer and flight IDs
                            Map<Long, Void> customerIds = new HashMap<>();
                            Map<Long, Void> flightIds = new HashMap<>();

                            for (int i = 0; i < reservationsArray.length(); i++) {
                                JSONObject reservationJson = reservationsArray.getJSONObject(i);
                                Long customerId = reservationJson.optLong("customer_id");
                                Long flightId = reservationJson.optLong("flight_id");

                                if (customerId > 0) customerIds.put(customerId, null);
                                if (flightId > 0) flightIds.put(flightId, null);
                            }

                            // Fetch customer and flight details in parallel
                            CompletableFuture<Void> customersFuture = fetchCustomerDetails(customerIds.keySet());
                            CompletableFuture<Void> flightsFuture = fetchFlightDetails(flightIds.keySet());

                            return CompletableFuture.allOf(customersFuture, flightsFuture)
                                    .thenApply(v -> response);

                        } catch (Exception e) {
                            System.err.println("Error processing reservations: " + e.getMessage());
                            return CompletableFuture.completedFuture(response);
                        }
                    } else {
                        return CompletableFuture.completedFuture(response);
                    }
                })
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        showLoading(false);

                        if (response.statusCode() == 200 || response.statusCode() == 206) {
                            try {
                                JSONArray reservationsArray = new JSONArray(response.body());
                                reservations.clear();

                                for (int i = 0; i < reservationsArray.length(); i++) {
                                    JSONObject reservationJson = reservationsArray.getJSONObject(i);

                                    // Apply status filter
                                    String statusFilterValue = statusFilter.getValue();
                                    if (!"All Status".equals(statusFilterValue)) {
                                        String reservationStatus = reservationJson.optString("reservation_status", "");
                                        if (!statusFilterValue.equals(reservationStatus)) {
                                            continue;
                                        }
                                    }

                                    // Extract reservation data
                                    Long reservationId = reservationJson.optLong("reservation_id");
                                    String reservationCode = reservationJson.optString("reservation_code", "N/A");
                                    String seatClass = reservationJson.optString("seat_class", "Economic");
                                    Double totalFare = reservationJson.optDouble("total_fare", 0.0);
                                    String reservationStatus = reservationJson.optString("reservation_status", "Pending");
                                    String paymentStatus = reservationJson.optString("payment_status", "Pending");

                                    // Get customer information from cache
                                    Long customerId = reservationJson.optLong("customer_id");
                                    String customerName = customerNameCache.getOrDefault(customerId, "Customer #" + customerId);

                                    // Get flight information from cache
                                    Long flightId = reservationJson.optLong("flight_id");
                                    String flightCode = "FL" + flightId;
                                    String departureCity = "Unknown";
                                    String destinationCity = "Unknown";
                                    String departureDate = "";
                                    String departureTime = "";

                                    if (flightCache.containsKey(flightId)) {
                                        JSONObject flight = flightCache.get(flightId);
                                        flightCode = flight.optString("flight_code", "FL" + flightId);
                                        departureCity = flight.optString("departure_city", "Unknown");
                                        destinationCity = flight.optString("destination_city", "Unknown");
                                        departureDate = flight.optString("departure_date", "");
                                        departureTime = flight.optString("departure_time", "");
                                    }

                                    System.out.println("Reservation " + i + ": " +
                                            "Code: " + reservationCode +
                                            ", Customer: " + customerName +
                                            ", Flight: " + flightCode +
                                            ", Route: " + departureCity + " → " + destinationCity);

                                    Reservation reservation = new Reservation(
                                            reservationId,
                                            reservationCode,
                                            customerName,
                                            flightCode,
                                            departureCity,
                                            destinationCity,
                                            seatClass,
                                            totalFare,
                                            reservationStatus,
                                            paymentStatus,
                                            departureDate,
                                            departureTime,
                                            flightId,
                                            customerId
                                    );

                                    reservations.add(reservation);
                                }

                                updatePaginationInfo();
                                updateStatus("Loaded " + reservations.size() + " reservations", "success");

                            } catch (Exception e) {
                                System.err.println("Error parsing reservations: " + e.getMessage());
                                e.printStackTrace();
                                updateStatus("Error loading reservations: " + e.getMessage(), "error");
                            }
                        } else {
                            System.err.println("Failed to load reservations. Status: " + response.statusCode() + ", Body: " + response.body());
                            updateStatus("Failed to load reservations. Status: " + response.statusCode(), "error");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        updateStatus("Error: " + ex.getMessage(), "error");
                        System.err.println("Error loading reservations: " + ex.getMessage());
                        ex.printStackTrace();
                    });
                    return null;
                });
    }

    private CompletableFuture<Void> fetchCustomerDetails(java.util.Set<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void>[] futures = customerIds.stream()
                .map(customerId -> supabaseService.getCustomerByIdNumber(String.valueOf(customerId), userAuthToken)
                        .thenAccept(response -> {
                            if (response.statusCode() == 200) {
                                try {
                                    JSONArray customers = new JSONArray(response.body());
                                    if (customers.length() > 0) {
                                        JSONObject customer = customers.getJSONObject(0);
                                        String customerName = customer.optString("full_name", "Customer #" + customerId);
                                        customerNameCache.put(customerId, customerName);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error parsing customer data for ID " + customerId + ": " + e.getMessage());
                                }
                            }
                        })
                )
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> fetchFlightDetails(java.util.Set<Long> flightIds) {
        if (flightIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void>[] futures = flightIds.stream()
                .map(flightId -> supabaseService.getFlightDetails(flightId)
                        .thenAccept(response -> {
                            if (response.statusCode() == 200) {
                                try {
                                    JSONArray flights = new JSONArray(response.body());
                                    if (flights.length() > 0) {
                                        JSONObject flight = flights.getJSONObject(0);
                                        flightCache.put(flightId, flight);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error parsing flight data for ID " + flightId + ": " + e.getMessage());
                                }
                            }
                        })
                )
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    @FXML
    private void handleSearch() {
        currentPage = 0;
        loadReservations();
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        statusFilter.setValue("All Status");
        pageSizeCombo.setValue("10");
        currentPage = 0;
        currentSearchTerm = "";
        pageSize = 10;
        customerNameCache.clear();
        flightCache.clear();
        loadReservations();
    }

    @FXML
    private void handleExport() {
        if (reservations.isEmpty()) {
            updateStatus("No data to export", "warning");
            return;
        }

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
        // Fetch complete reservation details
        showLoading(true);
        updateStatus("Loading reservation details...", "info");

        supabaseService.getReservationById(reservation.getReservationId(), userAuthToken)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        showLoading(false);

                        if (response.statusCode() == 200 || response.statusCode() == 206) {
                            try {
                                JSONArray reservationsArray = new JSONArray(response.body());
                                if (reservationsArray.length() > 0) {
                                    JSONObject reservationDetails = reservationsArray.getJSONObject(0);
                                    showReservationDetails(reservationDetails);
                                } else {
                                    showBasicReservationDetails(reservation);
                                }
                            } catch (Exception e) {
                                System.err.println("Error parsing reservation details: " + e.getMessage());
                                showBasicReservationDetails(reservation);
                            }
                        } else {
                            showBasicReservationDetails(reservation);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showBasicReservationDetails(reservation);
                    });
                    return null;
                });
    }

    private void showReservationDetails(JSONObject reservationDetails) {
        StringBuilder details = new StringBuilder();
        details.append("Reservation: ").append(reservationDetails.optString("reservation_code", "N/A")).append("\n\n");

        // Reservation details
        details.append("Seat Class: ").append(reservationDetails.optString("seat_class", "Economic")).append("\n");
        details.append("Trip Type: ").append(reservationDetails.optString("trip_type", "One-way")).append("\n");
        details.append("Seat Preference: ").append(reservationDetails.optString("seat_preference", "Any")).append("\n");
        details.append("Total Fare: R ").append(String.format("%.2f", reservationDetails.optDouble("total_fare", 0.0))).append("\n");
        details.append("Amount Paid: R ").append(String.format("%.2f", reservationDetails.optDouble("amount_paid", 0.0))).append("\n");
        details.append("Payment Due: R ").append(String.format("%.2f", reservationDetails.optDouble("payment_due", 0.0))).append("\n");
        details.append("Status: ").append(reservationDetails.optString("reservation_status", "Pending")).append("\n");
        details.append("Payment Status: ").append(reservationDetails.optString("payment_status", "Pending")).append("\n");

        if (reservationDetails.has("reservation_date")) {
            details.append("Reservation Date: ").append(reservationDetails.optString("reservation_date", "")).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservation Details");
        alert.setHeaderText("Complete Reservation Details");
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    private void showBasicReservationDetails(Reservation reservation) {
        String details = String.format(
                "Reservation: %s\n\nCustomer: %s\nFlight: %s\nRoute: %s → %s\nClass: %s\nTotal Fare: R %.2f\nStatus: %s\nPayment: %s",
                reservation.getReservationCode(),
                reservation.getCustomerName(),
                reservation.getFlightCode(),
                reservation.getDepartureCity(),
                reservation.getDestinationCity(),
                reservation.getSeatClass(),
                reservation.getTotalFare(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservation Details");
        alert.setHeaderText("Reservation: " + reservation.getReservationCode());
        alert.setContentText(details);
        alert.showAndWait();
    }

    private void editReservation(Reservation reservation) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(reservation.getSeatClass(),
                FXCollections.observableArrayList("Economic", "Business", "First Class"));
        dialog.setTitle("Edit Reservation");
        dialog.setHeaderText("Edit Seat Class for " + reservation.getReservationCode());
        dialog.setContentText("Select new seat class:");

        dialog.showAndWait().ifPresent(newSeatClass -> {
            if (!newSeatClass.equals(reservation.getSeatClass())) {
                updateStatus("Updating reservation...", "info");

                JSONObject updateData = new JSONObject();
                updateData.put("seat_class", newSeatClass);

                // You might want to update the fare based on the new seat class
                // This would require additional logic to calculate the new fare

                supabaseService.updateReservation(reservation.getReservationId().intValue(), updateData, userAuthToken)
                        .thenAccept(updateResponse -> {
                            Platform.runLater(() -> {
                                if (updateResponse.statusCode() == 200 || updateResponse.statusCode() == 204) {
                                    updateStatus("Reservation updated successfully", "success");
                                    loadReservations(); // Refresh the table
                                } else {
                                    updateStatus("Failed to update reservation", "error");
                                }
                            });
                        })
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                updateStatus("Error updating reservation: " + ex.getMessage(), "error");
                            });
                            return null;
                        });
            }
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
                                if (updateResponse.statusCode() == 200 || updateResponse.statusCode() == 204) {
                                    updateStatus("Reservation cancelled successfully", "success");
                                    loadReservations();
                                } else {
                                    updateStatus("Failed to cancel reservation", "error");
                                    System.err.println("Cancel response: " + updateResponse.body());
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
        showLoading(true);
        updateStatus("Generating ticket for " + reservation.getReservationCode() + "...", "info");

        // Fetch ticket details from the database
        supabaseService.getTicketByReservationId(reservation.getReservationId(), userAuthToken)
                .thenAccept(ticketResponse -> {
                    Platform.runLater(() -> {
                        showLoading(false);

                        if (ticketResponse.statusCode() == 200 || ticketResponse.statusCode() == 206) {
                            try {
                                JSONArray tickets = new JSONArray(ticketResponse.body());
                                if (tickets.length() > 0) {
                                    JSONObject ticket = tickets.getJSONObject(0);
                                    String ticketNumber = ticket.optString("ticket_number", "N/A");
                                    updateStatus("Ticket " + ticketNumber + " printed successfully", "success");

                                    // Show ticket details
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Ticket Printed");
                                    alert.setHeaderText("Ticket: " + ticketNumber);
                                    alert.setContentText("Ticket for " + reservation.getReservationCode() + " has been generated successfully.");
                                    alert.showAndWait();
                                } else {
                                    updateStatus("No ticket found for this reservation", "warning");
                                }
                            } catch (Exception e) {
                                updateStatus("Error processing ticket: " + e.getMessage(), "error");
                            }
                        } else {
                            updateStatus("No ticket available for this reservation", "warning");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        updateStatus("Error generating ticket: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    // ==================== NAVIGATION METHODS ====================

    private void navigateToDashboard() {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        SceneManager.navigateToDashboard(btnDashboard.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToReservation() {
        System.out.println("=== NAVIGATING TO RESERVATION FORM ===");
        SceneManager.navigateToReservationForm(btnReservation.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToManageReservations() {
        System.out.println("=== REFRESHING MANAGE RESERVATIONS ===");
        loadReservations();
    }

    private void navigateToFeedback() {
        System.out.println("=== NAVIGATING TO FEEDBACK ===");
        SceneManager.navigateToFeedback(btnFeedback.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToSupport() {
        System.out.println("=== NAVIGATING TO SUPPORT ===");
        SceneManager.navigateToSupport(btnSupport.getScene(), userAuthToken, userId, userEmail);
    }

    private void navigateToSettings() {
        System.out.println("=== NAVIGATING TO SETTINGS ===");
        SceneManager.navigateToSettings(btnSettings.getScene(), userAuthToken, userId, userEmail);
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
        reservationsTable.setDisable(show);
    }

    private void updateStatus(String message, String type) {
        statusMessageLabel.setText(message);
        statusMessageLabel.getStyleClass().removeAll("success", "error", "warning", "info");
        statusMessageLabel.getStyleClass().add(type);
    }

    private void updatePaginationInfo() {
        int total = reservations.size();
        paginationInfo.setText(String.format("Showing %d reservations (Page %d)", total, currentPage + 1));
    }
}