/**
 * Thrown when no booking matches the PNR and last name entered.
 * The message is the same whether the PNR or the name was wrong, so nobody
 * can use this screen to find out which PNRs exist.
 */
public class BookingNotFoundException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public BookingNotFoundException() {
        super("No booking found with that PNR and last name.");
    }
}