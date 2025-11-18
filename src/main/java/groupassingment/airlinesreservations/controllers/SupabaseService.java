package groupassingment.airlinesreservations.controllers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public class SupabaseService {

    private static final String SUPABASE_URL = "https://mikkxbspbhuwbczttopo.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1pa2t4YnNwYmh1d2JjenR0b3BvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3NTA4NzgsImV4cCI6MjA3ODMyNjg3OH0.W0Yt5oKK15D3dpQ_F23WjHQgSLcNLMuV64032f9cTxA";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    /**
     * Updated to accept and use the userAuthToken for authenticated retrieval.
     */
    public CompletableFuture<HttpResponse<String>> getPassengerByEmail(String email, String userAuthToken) {
        String url = SUPABASE_URL + "/rest/v1/passenger_info?email=eq." + email;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- CUSTOMERS ----------------

    /**
     * Retrieves a customer record from the 'customers' table by their ID Number.
     * TARGETS: 'customers?id_num=eq.{idNum}'
     */
    public CompletableFuture<HttpResponse<String>> getCustomerByIdNumber(String idNum, String userAuthToken) {
        // Ensure ID number is URL-encoded for safety, although digits shouldn't need it.
        String encodedIdNum = URLEncoder.encode(idNum, StandardCharsets.UTF_8);
        String url = SUPABASE_URL + "/rest/v1/customers?id_num=eq." + encodedIdNum;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Inserts a new customer record into the 'customers' table.
     * 🔑 FIX APPLIED: Updated to match the provided 'customers' table schema,
     * removing 'concess' and 'user_id' and adding 'category_fare'.
     */
    public CompletableFuture<HttpResponse<String>> insertCustomer(
            String name, String idNum, String address, String email, String phone, String categoryFare,
            String userAuthToken) {

        JSONObject customer = new JSONObject();
        customer.put("name", name);
        customer.put("id_num", idNum);
        customer.put("address", address);
        customer.put("email", email);
        customer.put("phone", phone);
        customer.put("category_fare", categoryFare); // ✅ Correct column
        // Removed: 'concess' and 'user_id' which are not in the schema

        String url = SUPABASE_URL + "/rest/v1/customers"; // TARGETS 'customers'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString("[" + customer.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- AIRPORTS ----------------

    public CompletableFuture<HttpResponse<String>> getAirports() {
        String url = SUPABASE_URL + "/rest/v1/airports?select=airport_code,airport_name,city,country";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- FLIGHTS ----------------

    public CompletableFuture<HttpResponse<String>> getFlights() {
        String url = SUPABASE_URL + "/rest/v1/flights?select=*";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- FLIGHT BOOKINGS ----------------

    public CompletableFuture<HttpResponse<String>> insertFlightBooking(
            JSONObject bookingData, String userAuthToken) {

        String url = SUPABASE_URL + "/rest/v1/flight_bookings"; // TARGETS 'flight_bookings'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString("[" + bookingData.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Retrieves flight bookings linked to the user's email.
     */
    public CompletableFuture<HttpResponse<String>> getUserBookings(String userEmail, String userAuthToken) {
        String url = SUPABASE_URL + "/rest/v1/flight_bookings?user_email=eq." + userEmail; // TARGETS 'flight_bookings'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // --- START: RESERVATION MANAGEMENT ---

    /**
     * Fetches a paginated and searchable list of reservations for the management screen.
     * TARGETS: 'reservations'
     */
    public CompletableFuture<HttpResponse<String>> fetchManagedReservations(
            int page, int pageSize, String searchTerm, String userAuthToken) { // Renamed method

        int offset = page * pageSize;

        // Build the query string
        StringBuilder query = new StringBuilder("?select=*");
        query.append("&offset=").append(offset);
        query.append("&limit=").append(pageSize);
        query.append("&order=reservation_date.desc");

        // Add search filter if provided
        if (searchTerm != null && !searchTerm.isBlank()) {
            String encodedSearch = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
            query.append("&reservation_code=ilike.*").append(encodedSearch).append("*");
        }

        String url = SUPABASE_URL + "/rest/v1/reservations" + query.toString(); // TARGETS 'reservations'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "count=exact")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Adds a new reservation.
     * TARGETS: 'reservations'
     */
    public CompletableFuture<HttpResponse<String>> addReservation(
            JSONObject reservationData, String userAuthToken) { // Renamed method

        String url = SUPABASE_URL + "/rest/v1/reservations"; // TARGETS 'reservations'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString("[" + reservationData.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Updates an existing reservation by its ID.
     * TARGETS: 'reservations'
     */
    public CompletableFuture<HttpResponse<String>> updateReservation(
            int reservationId, JSONObject reservationData, String userAuthToken) { // Renamed method

        String url = SUPABASE_URL + "/rest/v1/reservations?id=eq." + reservationId; // TARGETS 'reservations'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reservationData.toString()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Deletes a reservation by its ID.
     * TARGETS: 'reservations'
     */
    public CompletableFuture<HttpResponse<String>> deleteReservation(
            int reservationId, String userAuthToken) { // Renamed method

        String url = SUPABASE_URL + "/rest/v1/reservations?id=eq." + reservationId; // TARGETS 'reservations'

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken)
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // --- END: RESERVATION MANAGEMENT ---


    // ---------------- CONNECTION TEST ----------------

    public CompletableFuture<Boolean> testConnection() {
        String testUrl = SUPABASE_URL + "/rest/v1/";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }
}