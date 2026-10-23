/**
 * Thrown when a payment can't go through (missing details or a bad amount).
 */
public class InvalidPaymentException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public InvalidPaymentException(String message) {
        super(message);
    }
}