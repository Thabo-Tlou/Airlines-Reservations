package groupassingment.airlinesreservations.controllers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.json.JSONArray;

public class SupabaseService {

    // MADE PUBLIC for access outside the current package
    public static final String SUPABASE_URL = "https://mikkxbspbhuwbczttopo.supabase.co";
    // NOTE: In a real application, the key should be loaded securely from environment variables.
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1pa2t4YnNwYmh1d2JjenR0b3BvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3NTA4NzgsImV4cCI6MjA3ODMyNjg3OH0.W0Yt5oKK15D3dpQ_F23WjHQgSLcNLMuV64032f9cTxA";

    // MADE PUBLIC for access outside the current package
    public static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Helper method to validate the user's JWT before making an authenticated request.
     * This prevents 403 errors when a null/empty token is passed to RLS-protected endpoints.
     */
    private CompletableFuture<HttpResponse<String>> validateAuthToken(String userAuthToken) {
        if (userAuthToken == null || userAuthToken.trim().isEmpty()) {
            String errorMsg = "Authentication token is missing or invalid. Cannot proceed with authenticated request.";
            System.err.println("Error: " + errorMsg);
            return CompletableFuture.failedFuture(new IllegalArgumentException(errorMsg));
        }
        return null; // Return null if valid, indicating request can proceed
    }

    // ---------------- AUTH ----------------

    public CompletableFuture<HttpResponse<String>> signUp(String email, String password) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/signup"))
                .header("apikey", SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> login(String email, String password) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/token?grant_type=password"))
                .header("apikey", SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- PASSENGERS ----------------

    public CompletableFuture<HttpResponse<String>> insertPassenger(
            String firstName, String lastName, String email, String phone, String userId, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        JSONObject passenger = new JSONObject();
        passenger.put("first_name", firstName);
        passenger.put("last_name", lastName);
        passenger.put("email", email);
        passenger.put("phone", phone);
        passenger.put("user_id", userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/passenger_info"))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString("[" + passenger.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> getPassengerByEmail(String email, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            String url = SUPABASE_URL + "/rest/v1/passenger_info?email=eq." + encodedEmail;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in getPassengerByEmail: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ---------------- CUSTOMERS ----------------

    /**
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> getCustomerByIdNumber(String idNum, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String encodedIdNum = URLEncoder.encode(idNum, StandardCharsets.UTF_8.toString());
            // Using 'id_number' as per the schema
            String url = SUPABASE_URL + "/rest/v1/customers?id_number=eq." + encodedIdNum;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in getCustomerByIdNumber: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> getCustomerByEmail(String email, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            String url = SUPABASE_URL + "/rest/v1/customers?email=eq." + encodedEmail;

            System.out.println("DEBUG: Customer query URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Prefer", "return=representation")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("DEBUG: Error in getCustomerByEmail: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * FIXED: Includes token validation to prevent 403 Forbidden on RLS-protected insert.
     */
    public CompletableFuture<HttpResponse<String>> insertCustomer(
            String fullName, String idNumber, String address, String email, String phone, String categoryFare,
            String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            JSONObject customer = new JSONObject();
            customer.put("full_name", fullName); // Fixed from 'name'
            customer.put("id_number", idNumber); // Fixed from 'id_num'
            customer.put("address", address);
            customer.put("email", email);
            customer.put("phone", phone);
            customer.put("category_fare", categoryFare);

            String url = SUPABASE_URL + "/rest/v1/customers";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + customer.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in insertCustomer: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ---------------- FARE CATEGORIES ----------------
    // Uses anon key, assuming public read access. No userAuthToken check needed.

    public CompletableFuture<HttpResponse<String>> getAllFareCategories() {
        String url = SUPABASE_URL + "/rest/v1/fare_categories?select=*";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                // Authorization header is often unnecessary for public reads, but retained existing logic.
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- AIRPORTS ----------------
    // Uses anon key, assuming public read access. No userAuthToken check needed.

    public CompletableFuture<HttpResponse<String>> getAirports() {
        String url = SUPABASE_URL + "/rest/v1/airports?select=airport_code,airport_name,city,country";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                // Authorization header is often unnecessary for public reads, but retained existing logic.
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- FLIGHTS ----------------
    // Uses anon key, assuming public read access. No userAuthToken check needed.

    public CompletableFuture<HttpResponse<String>> getFlights() {
        String url = SUPABASE_URL + "/rest/v1/flights?select=*&order=departure_date.asc,departure_time.asc";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                // Authorization header is often unnecessary for public reads, but retained existing logic.
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> getFlightDetails(Long flightId) {
        String url = SUPABASE_URL + "/rest/v1/flights?flight_id=eq." + flightId + "&select=*";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                // Authorization header is often unnecessary for public reads, but retained existing logic.
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- SEATS ----------------

    /**
     * UPDATED: Uses database function for accurate seat availability checking
     */
    public CompletableFuture<HttpResponse<String>> getAvailableSeatsByFlight(Long flightId) {
        return getActuallyAvailableSeats(flightId);
    }

    /**
     * NEW: Calls the database function get_available_seats to get actually available seats
     * (excluding those with confirmed reservations)
     */
    public CompletableFuture<HttpResponse<String>> getActuallyAvailableSeats(Long flightId) {
        try {
            System.out.println("DEBUG: Calling database function get_available_seats for flight: " + flightId);

            String url = SUPABASE_URL + "/rest/v1/rpc/get_available_seats";

            JSONObject requestBody = new JSONObject();
            requestBody.put("p_flight_id", flightId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("DEBUG: Database function response status: " + response.statusCode());
                        if (response.statusCode() != 200) {
                            System.err.println("DEBUG: Database function error: " + response.body());
                        }
                        return response;
                    });

        } catch (Exception e) {
            System.err.println("Error calling get_available_seats function: " + e.getMessage());
            CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Fallback method using direct query (keep as backup)
     */
    public CompletableFuture<HttpResponse<String>> getAvailableSeatsByFlightFallback(Long flightId) {
        System.out.println("DEBUG: Using fallback seat availability check for flight: " + flightId);

        String url = SUPABASE_URL + "/rest/v1/seats?flight_id=eq." + flightId +
                "&is_available=eq.true&select=seat_id,seat_number,seat_class,seat_position,is_available,premium_seat_surcharge";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- RESERVATIONS ----------------

    // Note: The insertFlightBooking and getUserBookings methods are kept but might be redundant
    // if we use the official reservations table below.

    public CompletableFuture<HttpResponse<String>> insertFlightBooking(
            JSONObject bookingData, String userAuthToken) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Using deprecated flight_bookings table. Use addReservation instead."));
    }

    public CompletableFuture<HttpResponse<String>> getUserBookings(String userEmail, String userAuthToken) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Using deprecated flight_bookings table. Use getReservationsByCustomerEmail instead."));
    }

    /**
     * FIXED: Gets ALL reservation columns for a specific customer ID
     */
    public CompletableFuture<HttpResponse<String>> getReservationsByCustomerId(Long customerId, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        // Selecting ALL columns from the reservations table
        String url = SUPABASE_URL + "/rest/v1/reservations?customer_id=eq." + customerId +
                "&select=*&order=reservation_date.desc";

        System.out.println("DEBUG: Full reservations query URL: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * NEW: Gets ALL reservation columns for a specific customer email
     */
    public CompletableFuture<HttpResponse<String>> getReservationsByCustomerEmail(String email, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            // This relies on a relationship or RLS policy to resolve customer ID from email
            String url = SUPABASE_URL + "/rest/v1/reservations?customer_id.email=eq." + encodedEmail +
                    "&select=*&order=reservation_date.desc";

            System.out.println("DEBUG: Full reservations by email query URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in getReservationsByCustomerEmail: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * NEW: Gets ALL reservations with ALL columns (for admin/manage reservations view)
     */
    public CompletableFuture<HttpResponse<String>> getAllReservations(String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/reservations?select=*&order=reservation_date.desc";

        System.out.println("DEBUG: All reservations query URL: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * NEW: Gets reservation by ID with ALL columns
     */
    public CompletableFuture<HttpResponse<String>> getReservationById(Long reservationId, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/reservations?reservation_id=eq." + reservationId + "&select=*";

        System.out.println("DEBUG: Reservation by ID query URL: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * FIXED: Includes token validation and fetches ALL columns
     */
    public CompletableFuture<HttpResponse<String>> fetchManagedReservations(
            int page, int pageSize, String searchTerm, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            int offset = page * pageSize;

            StringBuilder query = new StringBuilder("?select=*");
            query.append("&offset=").append(offset);
            query.append("&limit=").append(pageSize);
            query.append("&order=reservation_date.desc");

            if (searchTerm != null && !searchTerm.isBlank()) {
                String encodedSearch = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
                query.append("&or=(reservation_code.ilike.*").append(encodedSearch).append("*,customer_id.full_name.ilike.*").append(encodedSearch).append("*)");
            }

            String url = SUPABASE_URL + "/rest/v1/reservations" + query.toString();

            System.out.println("DEBUG: Managed reservations query URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in fetchManagedReservations: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> addReservation(
            JSONObject reservationData, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/reservations";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + reservationData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in addReservation: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> updateReservation(
            int reservationId, JSONObject reservationData, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/reservations?reservation_id=eq." + reservationId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(reservationData.toString()))
                    .header("Prefer", "return=representation")
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in updateReservation: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> deleteReservation(
            int reservationId, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/reservations?reservation_id=eq." + reservationId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Prefer", "return=minimal")
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- PAYMENTS ----------------

    /**
     * Inserts a new payment record.
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> insertPayment(
            JSONObject paymentData, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/payments";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + paymentData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in insertPayment: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Fetches payment records by reservation ID.
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> getPaymentsByReservationId(Long reservationId, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/payments?reservation_id=eq." + reservationId + "&select=*";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- TICKETS ----------------

    /**
     * Fetches ticket details by reservation ID.
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> getTicketByReservationId(Long reservationId, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/tickets?reservation_id=eq." + reservationId + "&select=*";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // NOTE: Ticket insertion is usually handled by a database function/trigger when a reservation is confirmed.
    /**
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> insertTicket(
            JSONObject ticketData, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/tickets";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + ticketData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in insertTicket: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ---------------- WAITING LIST ----------------

    /**
     * Fetches waiting list entries for a specific flight.
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> getWaitingListByFlight(Long flightId, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        String url = SUPABASE_URL + "/rest/v1/waiting_list?flight_id=eq." + flightId + "&select=*&order=position.asc";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Inserts a new flight record into the 'flights' table.
     */
    public CompletableFuture<HttpResponse<String>> insertFlight(
            JSONObject flightData, String userAuthToken) {

        // Ensure the user has a valid token for RLS-protected insert
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult;

        try {
            String url = SUPABASE_URL + "/rest/v1/flights";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + flightData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in insertFlight: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Inserts a new waiting list entry.
     * FIXED: Includes token validation.
     */
    public CompletableFuture<HttpResponse<String>> insertWaitingListEntry(
            JSONObject waitingListData, String userAuthToken) {

        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/waiting_list";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + waitingListData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in insertWaitingListEntry: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> addToWaitingList(JSONObject waitingListData, String authToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(authToken);
        if (validationResult != null) return validationResult; // Token invalid, fail early

        try {
            String url = SUPABASE_URL + "/rest/v1/waiting_list";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString("[" + waitingListData.toString() + "]"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in addToWaitingList: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> getMaxWaitingPosition(Long flightId, String seatClass, String userAuthToken) {
        CompletableFuture<HttpResponse<String>> validationResult = validateAuthToken(userAuthToken);
        if (validationResult != null) return validationResult;

        try {
            String encodedSeatClass = URLEncoder.encode(seatClass, StandardCharsets.UTF_8.toString());
            String url = SUPABASE_URL + "/rest/v1/reservations?flight_id=eq." + flightId +
                    "&seat_class=eq." + encodedSeatClass +
                    "&select=waiting_list_position&order=waiting_list_position.desc&limit=1";

            System.out.println("DEBUG: Max waiting position query URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + userAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error in getMaxWaitingPosition: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ---------------- CONNECTION TEST ----------------

    public CompletableFuture<Boolean> testConnection() {
        String testUrl = SUPABASE_URL + "/rest/v1/";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .header("apikey", SUPABASE_KEY)
                // Use the anon key for the connection test
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }
}