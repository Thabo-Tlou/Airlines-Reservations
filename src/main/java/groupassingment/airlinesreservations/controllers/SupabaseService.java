package groupassingment.airlinesreservations.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
     * Inserts a new customer record into the 'customers' table.
     * CRITICAL: Assumes the 'user_id' column has been added to the SQL schema.
     */
    public CompletableFuture<HttpResponse<String>> insertCustomer(
            String name, String idNum, String address, String email, String phone, String concess,
            String userId, // User ID for RLS linkage in payload
            String userAuthToken) { // JWT for Authorization header

        JSONObject customer = new JSONObject();
        customer.put("name", name);
        customer.put("id_num", idNum);
        customer.put("address", address);
        customer.put("email", email);
        customer.put("phone", phone);
        customer.put("concess", concess);
        customer.put("user_id", userId); // Added to payload for RLS

        String url = SUPABASE_URL + "/rest/v1/customers"; // Target the 'customers' table

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken) // Correctly uses the JWT
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString("[" + customer.toString() + "]")) // Supabase expects an array of objects
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- AIRPORTS ----------------

    public CompletableFuture<HttpResponse<String>> getAirports() {
        String url = SUPABASE_URL + "/rest/v1/airports?select=airport_code,airport_name,city,country";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY) // Airports are public, so service key is fine
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
                .header("Authorization", "Bearer " + SUPABASE_KEY) // Flights are public
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    // ---------------- FLIGHT BOOKINGS ----------------

    public CompletableFuture<HttpResponse<String>> insertFlightBooking(
            JSONObject bookingData, String userAuthToken) {

        String url = SUPABASE_URL + "/rest/v1/flight_bookings";

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
     * 🔑 FIX: Signature changed to accept the userAuthToken and used for RLS.
     * You will need to update the calling code in DashboardController to pass the token.
     */
    public CompletableFuture<HttpResponse<String>> getUserBookings(String userEmail, String userAuthToken) {
        String url = SUPABASE_URL + "/rest/v1/flight_bookings?user_email=eq." + userEmail;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + userAuthToken) // ⬅️ FIX APPLIED
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

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