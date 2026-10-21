/**
 * Thrown when check-in isn't possible right now: too early, too late,
 * or the booking or flight is cancelled.
 */
public class CheckInNotAllowedException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public CheckInNotAllowedException(String message) {
        super(message);
    }
}