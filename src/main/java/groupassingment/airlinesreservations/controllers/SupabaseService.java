package groupassingment.airlinesreservations.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public class SupabaseService {

    private static final String SUPABASE_URL = "https://mikkxbspbhuwbczttopo.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1pa2t4YnNwYmh1d2JjenR0b3BvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3NTA4NzgsImV4cCI6MjA3ODMyNjg3OH0.W0Yt5oKK15D3dpQ_F23WjHQgSLcNLMuV64032f9cTxA";

    private static final HttpClient client = HttpClient.newHttpClient();

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
            String firstName, String lastName, String email, String phone) {

        JSONObject passenger = new JSONObject();
        passenger.put("first_name", firstName);
        passenger.put("last_name", lastName);
        passenger.put("email", email);
        passenger.put("phone", phone);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/passenger_info"))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString("[" + passenger.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> getPassengerByEmail(String email) {
        String url = SUPABASE_URL + "/rest/v1/passenger_info?email=eq." + email;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------- AIRPORTS ----------------

    public CompletableFuture<HttpResponse<String>> getAirports() {
        String url = SUPABASE_URL + "/rest/v1/Airport%20Tables";

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
        String url = SUPABASE_URL + "/rest/v1/Flights%20Table";

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

    public CompletableFuture<HttpResponse<String>> insertFlightBooking(JSONObject bookingData) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/Flight%20bookings"))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString("[" + bookingData.toString() + "]"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
