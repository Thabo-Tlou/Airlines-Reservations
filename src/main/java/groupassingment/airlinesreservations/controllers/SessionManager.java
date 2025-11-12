package groupassingment.airlinesreservations.controllers;

public class SessionManager {
    // Static fields hold the session data globally
    private static String authToken;
    private static String userId;
    private static String userEmail;

    /**
     * Sets the session data upon successful login.
     * 🔑 FIX: Trim the token here to ensure the global state always holds a clean JWT.
     */
    public static void setSessionData(String token, String id, String email) {
        authToken = token != null ? token.trim() : null;
        userId = id;
        userEmail = email;
        System.out.println("SessionManager: Session data successfully set.");
    }

    /**
     * Clears the session data upon logout.
     */
    public static void clearSessionData() {
        authToken = null;
        userId = null;
        userEmail = null;
        System.out.println("SessionManager: Session data cleared.");
    }

    // --- Getters for Controllers ---
    public static String getAuthToken() {
        return authToken;
    }

    public static String getUserId() {
        return userId;
    }

    public static String getUserEmail() {
        return userEmail;
    }
}