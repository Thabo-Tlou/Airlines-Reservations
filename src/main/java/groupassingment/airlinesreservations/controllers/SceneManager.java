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
     * NEW: Load scene using stored session data from SessionManager
     * This is the key fix for maintaining session between screens
     */
    public static void loadSceneWithStoredSession(String fxmlPath, Scene currentScene) {
        try {
            Stage stage = (Stage) currentScene.getWindow();
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Get stored session data from SessionManager
            String authToken = SessionManager.getAuthToken();
            String userId = SessionManager.getUserId();
            String userEmail = SessionManager.getUserEmail();

            System.out.println("=== SCENE MANAGER DEBUG ===");
            System.out.println("Loading scene: " + fxmlPath);
            System.out.println("Using stored session data:");
            System.out.println("User Email: " + userEmail);
            System.out.println("Auth Token: " + (authToken != null ? "PRESENT" : "NULL"));
            System.out.println("User ID: " + userId);
            System.out.println("=== END DEBUG ===");

            // Pass session data to the controller
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
        System.out.println("=== INITIALIZING CONTROLLER SESSION ===");
        System.out.println("Controller type: " + controller.getClass().getSimpleName());
        System.out.println("User Email: " + userEmail);

        if (controller instanceof ManageReservationsController) {
            ((ManageReservationsController) controller).initializeSessionData(authToken, userId, userEmail);
        } else if (controller instanceof ReservationFormController) {
            ((ReservationFormController) controller).initializeSessionData(authToken, userId, userEmail);
        } else if (controller instanceof DashboardController) {
            ((DashboardController) controller).initializeSessionData(authToken, userId, userEmail);
        }
        // Add other controllers as needed

        System.out.println("Controller session initialization complete");
        System.out.println("=== END INITIALIZATION ===");
    }

    // ==================== UPDATED NAVIGATION METHODS ====================

    /**
     * Navigate to Dashboard using stored session data
     */
    public static void navigateToDashboard(Scene currentScene) {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        SessionManager.debugSessionState(); // Debug current session state
        loadSceneWithStoredSession(DASHBOARD_FXML, currentScene);
    }

    /**
     * Navigate to Reservation Form using stored session data
     */
    public static void navigateToReservationForm(Scene currentScene) {
        System.out.println("=== NAVIGATING TO RESERVATION FORM ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(RESERVATION_FORM_FXML, currentScene);
    }

    /**
     * Navigate to Manage Reservations using stored session data
     */
    public static void navigateToManageReservations(Scene currentScene) {
        System.out.println("=== NAVIGATING TO MANAGE RESERVATIONS ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(MANAGE_RESERVATIONS_FXML, currentScene);
    }

    /**
     * Navigate to Feedback using stored session data
     */
    public static void navigateToFeedback(Scene currentScene) {
        System.out.println("=== NAVIGATING TO FEEDBACK ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(FEEDBACK_FXML, currentScene);
    }

    /**
     * Navigate to Support using stored session data
     */
    public static void navigateToSupport(Scene currentScene) {
        System.out.println("=== NAVIGATING TO SUPPORT ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(SUPPORT_FXML, currentScene);
    }

    /**
     * Navigate to Settings using stored session data
     */
    public static void navigateToSettings(Scene currentScene) {
        System.out.println("=== NAVIGATING TO SETTINGS ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(SETTINGS_FXML, currentScene);
    }

    /**
     * Navigate to Print Ticket using stored session data
     */
    public static void navigateToPrintTicket(Scene currentScene) {
        System.out.println("=== NAVIGATING TO PRINT TICKET ===");
        SessionManager.debugSessionState();
        loadSceneWithStoredSession(PRINT_TICKET_FXML, currentScene);
    }

    // ==================== BACKWARD COMPATIBILITY METHODS ====================

    /**
     * Navigate to Dashboard (backward compatibility)
     */
    public static void navigateToDashboard(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(DASHBOARD_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Reservation Form (backward compatibility)
     */
    public static void navigateToReservationForm(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(RESERVATION_FORM_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Manage Reservations (backward compatibility)
     */
    public static void navigateToManageReservations(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(MANAGE_RESERVATIONS_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Feedback (backward compatibility)
     */
    public static void navigateToFeedback(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(FEEDBACK_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Support (backward compatibility)
     */
    public static void navigateToSupport(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(SUPPORT_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Settings (backward compatibility)
     */
    public static void navigateToSettings(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(SETTINGS_FXML, currentScene, authToken, userId, userEmail);
    }

    /**
     * Navigate to Print Ticket (backward compatibility)
     */
    public static void navigateToPrintTicket(Scene currentScene, String authToken, String userId, String userEmail) {
        System.out.println("=== USING LEGACY NAVIGATION (with parameters) ===");
        loadScene(PRINT_TICKET_FXML, currentScene, authToken, userId, userEmail);
    }

    // ==================== PUBLIC PAGES (NO SESSION NEEDED) ====================

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
     * Handle Logout - Clear session and go to login
     */
    public static void handleLogout(Scene currentScene) {
        System.out.println("=== HANDLING LOGOUT ===");
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

    // ==================== ALERT METHODS ====================

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