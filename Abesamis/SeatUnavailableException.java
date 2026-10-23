/**
 * Thrown when a seat doesn't exist, is in another cabin, or is already taken.
 */
public class SeatUnavailableException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public SeatUnavailableException(String message) {
        super(message);
    }
}