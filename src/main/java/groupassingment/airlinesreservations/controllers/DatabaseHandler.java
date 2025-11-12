package groupassingment.airlinesreservations.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {

    // --- JDBC Connection Details (Inferred from your project URL) ---
    // Host is derived from your Supabase URL (db.<project-ref>.supabase.co)
    private static final String DB_HOST = "db.mikkxbspbhuwbczttopo.supabase.co";
    private static final String DB_PORT = "5432"; // Standard Postgres Port
    private static final String DB_NAME = "postgres"; // Standard Supabase database name
    private static final String DB_USER = "postgres"; // Standard Supabase default user

    // ⚠️ CRITICAL: YOU MUST REPLACE THIS WITH YOUR ACTUAL SUPABASE POSTGRES PASSWORD!
    private static final String DB_PASSWORD = "YOUR_SUPABASE_POSTGRES_PASSWORD_HERE";

    // Construct the full connection URL
    private static final String CONNECTION_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    /**
     * Establishes a connection to the PostgreSQL database (Supabase).
     * @return A valid Connection object.
     * @throws SQLException if the connection fails (e.g., wrong credentials, network issue).
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Ensure the PostgreSQL driver is loaded
            Class.forName("org.postgresql.Driver");

            // Establish the connection
            return DriverManager.getConnection(CONNECTION_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Ensure the JAR is in your project's dependencies.");
            throw new SQLException("JDBC Driver not available.", e);
        }
    }
}