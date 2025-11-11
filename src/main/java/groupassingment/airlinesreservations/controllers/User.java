package groupassingment.airlinesreservations.controllers;

/**
 * Model class to represent a user/passenger record retrieved from Supabase.
 */
public class User {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    public User(String firstName, String lastName, String email, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Full name is useful for the welcome message and profile display
    public String getFullName() {
        return firstName + " " + lastName;
    }
}