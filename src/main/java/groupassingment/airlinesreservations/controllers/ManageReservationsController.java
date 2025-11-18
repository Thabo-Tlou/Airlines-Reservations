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
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnReservation;
    @FXML
    private Button btnFeedback;
    @FXML
    private Button btnManageReservations;
    @FXML
    private Button btnSupport;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnLogout;

    // === FXML Injections (Main Content) ===
    @FXML
    private Button btnAddFlight;
    @FXML
    private Button btnUpdateFlight;
    @FXML
    private Button btnDeleteFlight;
    @FXML
    private TextField txtSearch;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TableView<Flight> tblFlights;
    @FXML
    private TableColumn<Flight, String> colFlightName;
    @FXML
    private TableColumn<Flight, String> colFlightCode;
    @FXML
    private TableColumn<Flight, String> colRoute;
    @FXML
    private TableColumn<Flight, String> colDepartureTime;
    @FXML
    private TableColumn<Flight, String> colArrivalTime;
    @FXML
    private TableColumn<Flight, Integer> colAvailableSeats;
    @FXML
    private Button btnFirst;
    @FXML
    private Button btnPrevious;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Button btnNext;
    @FXML
    private Button btnLast;
    @FXML
    private Pagination pagination;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void initializeSessionData(String currentUserToken, String currentUserId, String currentUserEmail) {

    }
}