package groupassingment.airlinesreservations.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PrintTicketController {

    @FXML private Label lbl_customerName;
    @FXML private Label lbl_customerEmail;
    @FXML private Label lbl_customerPhone;
    @FXML private Label lbl_customerId;
    @FXML private Label lbl_fareCategory;

    @FXML private Label lbl_flightCode;
    @FXML private Label lbl_travelDate;
    @FXML private Label lbl_travelFrom;
    @FXML private Label lbl_travelTo;

    @FXML private Label lbl_seatClass;
    @FXML private Label lbl_seatPref;

    @FXML private Label lbl_totalPrice;
    @FXML private Label lbl_issuedBy;

    private String userEmail; // To show who made the booking

    /**
     * Called by ReservationFormController after a successful booking.
     */
    public void setTicketData(
            String name,
            String email,
            String phone,
            String idNumber,
            String fareCategory,
            String flightCode,
            String date,
            String from,
            String to,
            String seatClass,
            String seatPref,
            double totalPrice,
            String userEmail
    ) {
        lbl_customerName.setText(name);
        lbl_customerEmail.setText(email);
        lbl_customerPhone.setText(phone);
        lbl_customerId.setText(idNumber);
        lbl_fareCategory.setText(fareCategory);

        lbl_flightCode.setText(flightCode);
        lbl_travelDate.setText(date);
        lbl_travelFrom.setText(from);
        lbl_travelTo.setText(to);

        lbl_seatClass.setText(seatClass);
        lbl_seatPref.setText(seatPref);

        lbl_totalPrice.setText(String.format("$%.2f", totalPrice));
        lbl_issuedBy.setText(userEmail);
    }
}
