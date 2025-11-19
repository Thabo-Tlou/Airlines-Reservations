package groupassingment.airlinesreservations.controllers;

public class SessionManager {
    // Static fields hold the session data globally
    private static String authToken;
    private static String userId;
    private static String userEmail;

    // --- CRITICAL FIELDS FOR RESERVATION ---
    private static Long customerID;
    private static String customerFareCategory;

    /**
     * Sets the basic session data upon successful login
     */
    public static void setSessionData(String token, String id, String email) {
        authToken = token != null ? token.trim() : null;
        userId = id;
        userEmail = email;
        System.out.println("SessionManager: Session data successfully set.");
    }

    /**
     * Sets customer-specific data for reservations
     */
    public static void setCustomerData(Long custId, String fareCategory) {
        customerID = custId;
        customerFareCategory = fareCategory;
        System.out.println("SessionManager: Customer data set. ID: " + custId + ", Category: " + fareCategory);
    }

    /**
     * Clears the session data upon logout.
     */
    public static void clearSessionData() {
        authToken = null;
        userId = null;
        userEmail = null;
        customerID = null;
        customerFareCategory = null;
        System.out.println("SessionManager: Session data cleared.");
    }

    // --- Getters for Controllers ---
    public static boolean isAuthenticated() {
        return authToken != null;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static String getUserId() {
        return userId;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    // --- CRITICAL Getters for Reservation Controller ---
    public static Long getCustomerID() {
        return customerID;
    }

    public static String getCustomerFareCategory() {
        return customerFareCategory;
    }

    /**
     * Check if customer data is available for reservations
     */
    public static boolean hasCustomerData() {
        return customerID != null && customerFareCategory != null;
    }
}