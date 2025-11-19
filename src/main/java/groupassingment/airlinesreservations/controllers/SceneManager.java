package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    // FXML file paths
    private static final String DASHBOARD_FXML = "/groupassingment/airlinesreservations/Dashboard.fxml";
    private static final String RESERVATION_FORM_FXML = "/groupassingment/airlinesreservations/Reservation-Form.fxml";
    private static final String MANAGE_RESERVATIONS_FXML = "/groupassingment/airlinesreservations/ManageReservations.fxml";
    private static final String FEEDBACK_FXML = "/groupassingment/airlinesreservations/Feedback.fxml";
    private static final String SUPPORT_FXML = "/groupassingment/airlinesreservations/Support.fxml";
    private static final String SETTINGS_FXML = "/groupassingment/airlinesreservations/Settings.fxml";
    private static final String LOGIN_FXML = "/groupassingment/airlinesreservations/Login.fxml";
    private static final String SIGNUP_FXML = "/groupassingment/airlinesreservations/SignUp.fxml";
    private static final String WELCOME_SCREEN_FXML = "/groupassingment/airlinesreservations/WelcomeScreen.fxml";
    private static final String PRINT_TICKET_FXML = "/groupassingment/airlinesreservations/PrintTicket.fxml";
    private static final String LOGOUT_FXML = "/groupassingment/airlinesreservations/Logout.fxml";

    /**
     * Main method to load any scene with session data
     */
    public static void loadScene(String fxmlPath, Scene currentScene, String authToken, String userId, String userEmail) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Pass session data to the controller if it has the method
            initializeControllerSessionData(loader.getController(), authToken, userId, userEmail);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load: " + fxmlPath);
        }
    }

    /**
     * Initialize session data for controllers that support it
     */
    private static void initializeControllerSessionData(Object controller, String authToken, String userId, String userEmail) {
        if (controller instanceof ManageReservationsController) {
            ((ManageReservationsController) controller).initializeSessionData(authToken, userId, userEmail);
        } else if (controller instanceof ReservationFormController) {
            ((ReservationFormController) controller).initializeSessionData(authToken, userId, userEmail);
        } else if (controller instanceof DashboardController) {
            ((DashboardController) controller).initializeSessionData(authToken, userId, userEmail);
        }
        // Add other controllers as needed
    }

    // ==================== SPECIFIC NAVIGATION METHODS ====================

    /**
     * Navigate to Dashboard
     */
    public static void navigateToDashboard(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(DASHBOARD_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Reservation Form
     */
    public static void navigateToReservationForm(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(RESERVATION_FORM_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Manage Reservations
     */
    public static void navigateToManageReservations(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(MANAGE_RESERVATIONS_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Feedback
     */
    public static void navigateToFeedback(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(FEEDBACK_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Support
     */
    public static void navigateToSupport(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(SUPPORT_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Settings
     */
    public static void navigateToSettings(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(SETTINGS_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Login (no session data needed)
     */
    public static void navigateToLogin(Scene currentScene) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            Parent root = FXMLLoader.load(SceneManager.class.getResource(LOGIN_FXML));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load login screen");
        }
    }

    /**
     * Navigate to Sign Up (no session data needed)
     */
    public static void navigateToSignUp(Scene currentScene) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            Parent root = FXMLLoader.load(SceneManager.class.getResource(SIGNUP_FXML));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load sign up screen");
        }
    }

    /**
     * Navigate to Welcome Screen (no session data needed)
     */
    public static void navigateToWelcomeScreen(Scene currentScene) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            Parent root = FXMLLoader.load(SceneManager.class.getResource(WELCOME_SCREEN_FXML));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load welcome screen");
        }
    }

    /**
     * Navigate to Print Ticket
     */
    public static void navigateToPrintTicket(Scene currentScene, String authToken, String userId, String userEmail) {
        loadScene(PRINT_TICKET_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Handle Logout - Clear session and go to login
     */
    public static void handleLogout(Scene currentScene) {
        // Clear session data
        SessionManager.clearSessionData();

        // Navigate to login screen
        navigateToLogin(currentScene);
    }

    /**
     * Simple scene loading without session data (for public pages)
     */
    public static void loadSceneWithoutSession(String fxmlPath, Scene currentScene) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unable to load: " + fxmlPath);
        }
    }

    /**
     * Show information alert (for features coming soon, etc.)
     */
    public static void showInfoAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private static void showErrorAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}