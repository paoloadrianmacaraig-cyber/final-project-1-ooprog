import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SystemData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Flight> flights = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private boolean sampleData;

    public Map<String, User> getUsers() {
        return users;
    }

    public Map<String, Flight> getFlights() {
        return flights;
    }

    public Map<String, Booking> getBookings() {
        return bookings;
    }

    public boolean hasSampleData() {
        return sampleData;
    }

    public void setSampleData(boolean sampleData) {
        this.sampleData = sampleData;
    }
}