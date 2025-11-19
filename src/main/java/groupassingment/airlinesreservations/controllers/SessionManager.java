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
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Session data set successfully:");
        System.out.println("User ID: " + userId);
        System.out.println("User Email: " + userEmail);
        System.out.println("Auth Token: " + (authToken != null ? "PRESENT (" + authToken.length() + " chars)" : "NULL"));
        System.out.println("=== END DEBUG ===");
    }

    /**
     * Sets customer-specific data for reservations
     */
    public static void setCustomerData(Long custId, String fareCategory) {
        customerID = custId;
        customerFareCategory = fareCategory;
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Customer data set:");
        System.out.println("Customer ID: " + custId);
        System.out.println("Fare Category: " + fareCategory);
        System.out.println("=== END DEBUG ===");
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
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Session data cleared");
        System.out.println("=== END DEBUG ===");
    }

    // --- Getters for Controllers ---
    public static boolean isAuthenticated() {
        boolean authenticated = authToken != null;
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Authentication check: " + authenticated);
        System.out.println("=== END DEBUG ===");
        return authenticated;
    }

    public static String getAuthToken() {
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Getting auth token: " + (authToken != null ? "PRESENT" : "NULL"));
        System.out.println("=== END DEBUG ===");
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
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Getting Customer ID: " + customerID);
        System.out.println("=== END DEBUG ===");
        return customerID;
    }

    public static String getCustomerFareCategory() {
        return customerFareCategory;
    }

    // ADD THIS MISSING METHOD - this might be what's causing the issue
    public static String getCustomerCategory() {
        return customerFareCategory; // Alias for compatibility
    }

    /**
     * Check if customer data is available for reservations
     */
    public static boolean hasCustomerData() {
        boolean hasData = customerID != null && customerFareCategory != null;
        System.out.println("=== SESSION MANAGER DEBUG ===");
        System.out.println("Has customer data: " + hasData);
        System.out.println("Customer ID: " + customerID);
        System.out.println("Fare Category: " + customerFareCategory);
        System.out.println("=== END DEBUG ===");
        return hasData;
    }

    /**
     * Debug method to print current session state
     */
    public static void debugSessionState() {
        System.out.println("=== CURRENT SESSION STATE ===");
        System.out.println("User ID: " + userId);
        System.out.println("User Email: " + userEmail);
        System.out.println("Auth Token: " + (authToken != null ? "PRESENT" : "NULL"));
        System.out.println("Customer ID: " + customerID);
        System.out.println("Customer Fare Category: " + customerFareCategory);
        System.out.println("=== END SESSION STATE ===");
    }
}