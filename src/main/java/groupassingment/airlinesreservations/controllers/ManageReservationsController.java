package groupassingment.airlinesreservations.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ManageReservationsController implements Initializable {

    // === FXML Injections (Sidebar) ===
    @FXML private Button btnDashboard;
    @FXML private Button btnReservation;
    @FXML private Button btnFeedback;
    @FXML private Button btnManageReservations;
    @FXML private Button btnSupport;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    // === FXML Injections (Main Content) ===
    @FXML private Button btnAddFlight;
    @FXML private Button btnUpdateFlight;
    @FXML private Button btnDeleteFlight;
    @FXML private TextField txtSearch;
    @FXML private ScrollPane scrollPane;
    @FXML private TableView<Flight> tblFlights;
    @FXML private TableColumn<Flight, String> colFlightName;
    @FXML private TableColumn<Flight, String> colFlightCode;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colDepartureTime;
    @FXML private TableColumn<Flight, String> colArrivalTime;
    @FXML private TableColumn<Flight, Integer> colAvailableSeats;
    @FXML private Button btnFirst;
    @FXML private Button btnPrevious;
    @FXML private Label lblPageInfo;
    @FXML private Button btnNext;
    @FXML private Button btnLast;
    @FXML private Pagination pagination;

    // === Class Members ===
    private SupabaseService supabaseService;
    private String userAuthToken;
    private final ObservableList<Flight> flightList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE = 15;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.supabaseService = new SupabaseService();

        // FIXED: Use static method directly - no getInstance() needed
        this.userAuthToken = SessionManager.getAuthToken();

        // CRITICAL: Check for auth token.
        if (this.userAuthToken == null || this.userAuthToken.isBlank()) {
            showAlert("Authentication Error", "You are not logged in. Please log in and try again.");
            setControlsDisabled(true);
            return;
        }

        setupTableColumns();
        setupPagination();
        setupEventHandlers();

        // Initial data load
        loadPageData(0);
    }

    /**
     * Binds the Flight model properties to the TableView columns.
     */
    private void setupTableColumns() {
        colFlightName.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        colFlightCode.setCellValueFactory(new PropertyValueFactory<>("flightCode"));
        colDepartureTime.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        colArrivalTime.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        colAvailableSeats.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));

        // Custom cell factory for the "Source → Destination" route
        colRoute.setCellValueFactory(cellData -> {
            String source = cellData.getValue().getSource();
            String dest = cellData.getValue().getDestination();
            return new SimpleStringProperty(source + " → " + dest);
        });

        tblFlights.setItems(flightList);
    }

    /**
     * Configures the Pagination control.
     */
    private void setupPagination() {
        pagination.setPageFactory(pageIndex -> {
            loadPageData(pageIndex);
            return new Label("Loading...");
        });
    }

    /**
     * Wires up all button clicks and text field listeners.
     */
    private void setupEventHandlers() {
        // CRUD buttons
        btnDeleteFlight.setOnAction(event -> handleDeleteFlight());
        btnAddFlight.setOnAction(event -> handleAddFlight());
        btnUpdateFlight.setOnAction(event -> handleUpdateFlight());

        // Search field listener
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            pagination.setCurrentPageIndex(0);
            loadPageData(0);
        });

        // Manual navigation buttons
        btnFirst.setOnAction(event -> pagination.setCurrentPageIndex(0));
        btnLast.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getPageCount() - 1));
        btnNext.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getCurrentPageIndex() + 1));
        btnPrevious.setOnAction(event -> pagination.setCurrentPageIndex(pagination.getCurrentPageIndex() - 1));

        // Sidebar navigation
        btnLogout.setOnAction(event -> handleLogout());
        btnDashboard.setOnAction(event -> handleDashboard());
        btnReservation.setOnAction(event -> handleReservation());
        btnFeedback.setOnAction(event -> handleFeedback());
        btnSupport.setOnAction(event -> handleSupport());
        btnSettings.setOnAction(event -> handleSettings());
    }

    /**
     * Main data loading method.
     */
    private void loadPageData(int pageIndex) {
        String searchTerm = txtSearch.getText().trim();
        tblFlights.setPlaceholder(new Label("Loading flights..."));

        supabaseService.fetchManagedFlights(pageIndex, PAGE_SIZE, searchTerm, userAuthToken)
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == 200) {
                        String rangeHeader = response.headers().firstValue("Content-Range").orElse("0-0/0");
                        int totalCount = Integer.parseInt(rangeHeader.substring(rangeHeader.indexOf('/') + 1));
                        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

                        JSONArray jsonArray = new JSONArray(response.body());
                        List<Flight> pageFlights = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            pageFlights.add(new Flight(jsonArray.getJSONObject(i)));
                        }

                        Platform.runLater(() -> {
                            flightList.setAll(pageFlights);
                            pagination.setPageCount(totalPages);
                            updateNavigationControls(pageIndex, totalPages);
                            tblFlights.setPlaceholder(new Label("No flights found."));
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert("Error", "Failed to load flights: " + response.body());
                            tblFlights.setPlaceholder(new Label("Error loading flights."));
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert("Error", "An error occurred: " + ex.getMessage());
                        tblFlights.setPlaceholder(new Label("Error loading flights."));
                    });
                    return null;
                });
    }

    /**
     * Updates navigation controls state
     */
    private void updateNavigationControls(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            lblPageInfo.setText("Page 0 of 0");
        } else {
            lblPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
        }

        btnFirst.setDisable(currentPage == 0);
        btnPrevious.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
        btnLast.setDisable(currentPage >= totalPages - 1);
    }

    /**
     * Handles the "Delete Flight" button click.
     */
    private void handleDeleteFlight() {
        Flight selectedFlight = tblFlights.getSelectionModel().getSelectedItem();

        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a flight to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Flight: " + selectedFlight.getFlightCode());
        confirmation.setContentText("Are you sure you want to delete this flight? This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                supabaseService.deleteFlight(selectedFlight.getId(), userAuthToken)
                        .thenAcceptAsync(deleteResponse -> {
                            if (deleteResponse.statusCode() == 204) {
                                Platform.runLater(() -> {
                                    loadPageData(pagination.getCurrentPageIndex());
                                    showAlert("Success", "Flight deleted successfully!");
                                });
                            } else {
                                Platform.runLater(() -> {
                                    showAlert("Delete Error", "Failed to delete flight: " + deleteResponse.body());
                                });
                            }
                        })
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                showAlert("Delete Error", "An error occurred: " + ex.getMessage());
                            });
                            return null;
                        });
            }
        });
    }

    /**
     * Handles the "Add Flight" button click.
     */
    private void handleAddFlight() {
        showAlert("Not Implemented", "Add Flight dialog has not been implemented yet.");
    }

    /**
     * Handles the "Update Flight" button click.
     */
    private void handleUpdateFlight() {
        Flight selectedFlight = tblFlights.getSelectionModel().getSelectedItem();

        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a flight to update.");
            return;
        }

        showAlert("Not Implemented", "Update Flight dialog has not been implemented yet.");
    }

    /**
     * Handle logout action
     */
    private void handleLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Logout");
        confirmation.setHeaderText("Logout from System");
        confirmation.setContentText("Are you sure you want to logout?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.clearSessionData();
                showAlert("Success", "You have been logged out successfully.");
                // TODO: Navigate to login screen
            }
        });
    }

    /**
     * Handle dashboard navigation
     */
    private void handleDashboard() {
        System.out.println("Navigating to dashboard");
        // TODO: Add navigation to dashboard
    }

    /**
     * Handle reservation navigation
     */
    private void handleReservation() {
        System.out.println("Navigating to reservation");
        // TODO: Add navigation to reservation page
    }

    /**
     * Handle feedback navigation
     */
    private void handleFeedback() {
        System.out.println("Navigating to feedback");
        // TODO: Add navigation to feedback page
    }

    /**
     * Handle support navigation
     */
    private void handleSupport() {
        System.out.println("Navigating to support");
        // TODO: Add navigation to support page
    }

    /**
     * Handle settings navigation
     */
    private void handleSettings() {
        System.out.println("Navigating to settings");
        // TODO: Add navigation to settings page
    }

    /**
     * Utility method to show alerts
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Helper to disable all controls if auth fails.
     */
    private void setControlsDisabled(boolean disabled) {
        btnAddFlight.setDisable(disabled);
        btnUpdateFlight.setDisable(disabled);
        btnDeleteFlight.setDisable(disabled);
        txtSearch.setDisable(disabled);
        tblFlights.setDisable(disabled);
        pagination.setDisable(disabled);
        btnFirst.setDisable(disabled);
        btnPrevious.setDisable(disabled);
        btnNext.setDisable(disabled);
        btnLast.setDisable(disabled);
    }

    public void initializeSessionData(String currentUserToken, String currentUserId, String currentUserEmail) {
    }
}