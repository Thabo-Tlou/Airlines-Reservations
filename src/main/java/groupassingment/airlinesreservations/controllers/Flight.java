package groupassingment.airlinesreservations.controllers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.json.JSONObject;

/**
 * Model class for a Flight.
 * Uses JavaFX properties for easy binding with TableView.
 * Includes a constructor to parse a JSONObject from Supabase.
 */
public class Flight {

    private final IntegerProperty id;
    private final StringProperty flightName;
    private final StringProperty flightCode;
    private final StringProperty source;
    private final StringProperty destination;
    private final StringProperty departureTime;
    private final StringProperty arrivalTime;
    private final IntegerProperty availableSeats;

    /**
     * Constructs a Flight object from a JSONObject.
     * NOTE: Adjust keys ("flight_name", "flight_code", etc.)
     * to match your *exact* column names in the Supabase table.
     */
    public Flight(JSONObject json) {
        // Use .optInt and .optString for safety (handles nulls)
        this.id = new SimpleIntegerProperty(json.optInt("id", 0));
        this.flightName = new SimpleStringProperty(json.optString("flight_name", "N/A"));
        this.flightCode = new SimpleStringProperty(json.optString("flight_code", "N/A"));
        this.source = new SimpleStringProperty(json.optString("source", "N/A"));
        this.destination = new SimpleStringProperty(json.optString("destination", "N/A"));
        this.departureTime = new SimpleStringProperty(json.optString("departure_time", "N/A"));
        this.arrivalTime = new SimpleStringProperty(json.optString("arrival_time", "N/A"));
        this.availableSeats = new SimpleIntegerProperty(json.optInt("available_seats", 0));
    }

    // --- Getters for values ---
    public int getId() { return id.get(); }
    public String getFlightName() { return flightName.get(); }
    public String getFlightCode() { return flightCode.get(); }
    public String getSource() { return source.get(); }
    public String getDestination() { return destination.get(); }
    public String getDepartureTime() { return departureTime.get(); }
    public String getArrivalTime() { return arrivalTime.get(); }
    public int getAvailableSeats() { return availableSeats.get(); }

    // --- Getters for properties (for JavaFX) ---
    public IntegerProperty idProperty() { return id; }
    public StringProperty flightNameProperty() { return flightName.get() != null ? new SimpleStringProperty(flightName.get()) : new SimpleStringProperty("N/A"); }
    public StringProperty flightCodeProperty() { return flightCode.get() != null ? new SimpleStringProperty(flightCode.get()) : new SimpleStringProperty("N/A"); }
    public StringProperty sourceProperty() { return source.get() != null ? new SimpleStringProperty(source.get()) : new SimpleStringProperty("N/A"); }
    public StringProperty destinationProperty() { return destination.get() != null ? new SimpleStringProperty(destination.get()) : new SimpleStringProperty("N/A"); }
    public StringProperty departureTimeProperty() { return departureTime.get() != null ? new SimpleStringProperty(departureTime.get()) : new SimpleStringProperty("N/A"); }
    public StringProperty arrivalTimeProperty() { return arrivalTime.get() != null ? new SimpleStringProperty(arrivalTime.get()) : new SimpleStringProperty("N/A"); }
    public IntegerProperty availableSeatsProperty() { return availableSeats.get() != 0 ? new SimpleIntegerProperty(availableSeats.get()) : new SimpleIntegerProperty(0); }

    /**
     * Creates a JSONObject suitable for inserting or updating in Supabase.
     * Adjust keys to match your table column names.
     */
    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("flight_name", getFlightName());
        json.put("flight_code", getFlightCode());
        json.put("source", getSource());
        json.put("destination", getDestination());
        json.put("departure_time", getDepartureTime());
        json.put("arrival_time", getArrivalTime());
        json.put("available_seats", getAvailableSeats());
        // We don't include 'id' as it's not sent in the body for add/update
        return json;
    }
}