package groupassingment.airlinesreservations.controllers;

/**
 * Utility class to hold intermediate data during the multi-step booking transaction.
 * This object is used to pass context (IDs and calculated fare) between
 * the CompletableFuture stages.
 */
public class BookingTransactionInfo {
    // Fields are final for immutability where possible

    // FIX: Changed from 'int' to 'Long' to match standard PostgreSQL BIGINT/serial ID type
    public final Long customerId;
    public final Long flightId;
    public final Long seatId;
    public final double totalFare;

    // Mutable, assigned after the reservation record is successfully created
    public Long reservationId;

    /**
     * Constructor used after finding customer, flight, and seat, but before reservation creation.
     */
    public BookingTransactionInfo(Long customerId, Long flightId, Long seatId, double totalFare) {
        this.customerId = customerId;
        this.flightId = flightId;
        this.seatId = seatId;
        this.totalFare = totalFare;
    }

    /**
     * Constructor used after reservation creation, including the generated reservation ID.
     */
    public BookingTransactionInfo(Long customerId, Long flightId, Long seatId, double totalFare, Long reservationId) {
        this(customerId, flightId, seatId, totalFare);
        this.reservationId = reservationId;
    }
}