/**
 * Thrown when no flight has the flight number that was entered.
 */
public class FlightNotFoundException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public FlightNotFoundException(String flightNumber) {
        super("Flight " + flightNumber + " not found.");
    }
}