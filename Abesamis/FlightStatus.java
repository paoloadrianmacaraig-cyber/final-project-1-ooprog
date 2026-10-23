/**
 * Departed is never set by hand: a flight counts as departed once its
 * departure time has passed (see Flight.getStatusAt).
 */
public enum FlightStatus {
    SCHEDULED("Scheduled"),
    DELAYED("Delayed"),
    CANCELLED("Cancelled"),
    DEPARTED("Departed");

    private final String label;

    FlightStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}