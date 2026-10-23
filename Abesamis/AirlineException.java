/**
 * Parent of every checked exception in the system. Each specific problem
 * (flight not found, seat taken, bad input...) extends this, so the chain is
 * Exception -> AirlineException -> FlightNotFoundException (multilevel
 * inheritance). Code that doesn't care which problem happened can catch
 * AirlineException once instead of every type.
 */
public class AirlineException extends Exception {

    private static final long serialVersionUID = 1L;

    public AirlineException(String message) {
        super(message);
    }
}