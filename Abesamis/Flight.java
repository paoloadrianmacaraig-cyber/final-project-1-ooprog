import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One scheduled flight. It builds its own seat map from its AircraftType
 * when created, and it is the only place seats get booked or released.
 */
public class Flight implements Serializable {

    // Bumped when the airline was added, so an old save file is replaced with fresh sample data.
    private static final long serialVersionUID = 2L;

    public static final int MIN_DURATION_MINUTES = 30;
    public static final int MAX_DURATION_MINUTES = 300;
    public static final double MAX_BASE_FARE = 100000;
    public static final int MAX_PETS = 3;
    private static final int GATE_COUNT = 12;

    private final Airline airline;
    private final String flightNumber;
    private final Airport origin;
    private final Airport destination;
    private LocalDateTime departure;
    private final int durationMinutes;
    private final AircraftType aircraftType;
    private double baseFare;
    private FlightStatus status = FlightStatus.SCHEDULED;
    private final Map<String, Seat> seats = new LinkedHashMap<>();
    private int petCount;

    public Flight(Airline airline, String flightNumber, Airport origin, Airport destination, LocalDateTime departure,
                  int durationMinutes, AircraftType aircraftType, double baseFare) {
        if (origin == destination) {
            throw new IllegalArgumentException("Destination can't be the same as the origin.");
        }
        if (!flightNumber.startsWith(airline.getCode())) {
            throw new IllegalArgumentException(flightNumber + " doesn't start with " + airline.getName() + "'s code.");
        }
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.durationMinutes = durationMinutes;
        this.aircraftType = aircraftType;
        this.baseFare = baseFare;
        buildSeatMap();
    }

    private void buildSeatMap() {
        for (CabinLayout cabin : aircraftType.getCabins()) {
            for (int row : cabin.actualRowNumbers()) {
                for (ColumnSlot slot : cabin.getTemplate()) {
                    if (slot.isSeat()) {
                        Seat seat = new Seat(row, slot.getLetter(), cabin.getCabinName());
                        seats.put(seat.getLabel(), seat);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Details
    // ------------------------------------------------------------------

    public Airline getAirline() { return airline; }
    public String getFlightNumber() { return flightNumber; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
    public LocalDateTime getDeparture() { return departure; }
    public int getDurationMinutes() { return durationMinutes; }
    public AircraftType getAircraftType() { return aircraftType; }
    public double getBaseFare() { return baseFare; }
    public FlightStatus getStatus() { return status; }

    public LocalDateTime getArrival() {
        return departure.plusMinutes(durationMinutes);
    }

    /** "MW101   Manila (MNL) -> Cebu (CEB)   2026-10-18 09:00". Used in lists and on tickets. */
    @Override
    public String toString() {
        return flightNumber + "   " + getRouteLabel() + "   " + ConsoleStyle.dateTime(departure);
    }

    /** "Manila (MNL) -> Cebu (CEB)" */
    public String getRouteLabel() {
        return origin.getLabel() + " -> " + destination.getLabel();
    }

    /** Same gate every time for the same flight number. */
    public String getGate() {
        return String.valueOf(1 + Math.floorMod(flightNumber.hashCode(), GATE_COUNT));
    }

    /** Status as of a moment: once departure time passes, a flight that wasn't cancelled is Departed. */
    public FlightStatus getStatusAt(LocalDateTime now) {
        if (status != FlightStatus.CANCELLED && hasDeparted(now)) {
            return FlightStatus.DEPARTED;
        }
        return status;
    }

    public boolean hasDeparted(LocalDateTime now) {
        return !now.isBefore(departure);
    }

    public boolean isBookable(LocalDateTime now) {
        return status != FlightStatus.CANCELLED && !hasDeparted(now) && availableSeats() > 0;
    }

    /** New Economy base fare for future bookings. Bookings already paid keep their price. */
    public void setBaseFare(double baseFare) {
        if (baseFare <= 0 || baseFare > MAX_BASE_FARE) {
            throw new IllegalArgumentException("Base fare must be above 0 and at most " + MAX_BASE_FARE + ".");
        }
        this.baseFare = baseFare;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    /** Moving a flight later marks it Delayed; moving it earlier keeps it Scheduled. */
    public void reschedule(LocalDateTime newDeparture) {
        status = newDeparture.isAfter(departure) ? FlightStatus.DELAYED : FlightStatus.SCHEDULED;
        departure = newDeparture;
    }

    // ------------------------------------------------------------------
    // Seats
    // ------------------------------------------------------------------

    public Seat getSeat(String label) {
        return seats.get(label);
    }

    public CabinLayout getCabinLayout(String cabinName) {
        for (CabinLayout cabin : aircraftType.getCabins()) {
            if (cabin.getCabinName().equals(cabinName)) {
                return cabin;
            }
        }
        return null;
    }

    public int totalSeats() {
        return seats.size();
    }

    public int availableSeats() {
        int free = 0;
        for (Seat s : seats.values()) {
            if (!s.isBooked()) {
                free++;
            }
        }
        return free;
    }

    public int availableSeatsIn(String cabinName) {
        int free = 0;
        for (Seat s : seats.values()) {
            if (s.getCabinName().equals(cabinName) && !s.isBooked()) {
                free++;
            }
        }
        return free;
    }

    /** Throws with the exact reason if this seat can't be taken in this cabin. */
    public void checkSeatAvailable(String label, String cabinName) throws SeatUnavailableException {
        Seat seat = seats.get(label);
        if (seat == null) {
            throw new SeatUnavailableException("Seat " + label + " doesn't exist on this aircraft.");
        }
        if (!seat.getCabinName().equals(cabinName)) {
            throw new SeatUnavailableException("Seat " + label + " is in " + seat.getCabinName() + ", not " + cabinName + ".");
        }
        if (seat.isBooked()) {
            throw new SeatUnavailableException("Seat " + label + " is already taken.");
        }
    }

    public void bookSeat(String label, String cabinName) throws SeatUnavailableException {
        checkSeatAvailable(label, cabinName);
        seats.get(label).book();
    }

    public void releaseSeat(String label) {
        Seat seat = seats.get(label);
        if (seat != null) {
            seat.release();
        }
    }

    // ------------------------------------------------------------------
    // Pets in the cabin (stretch goal)
    // ------------------------------------------------------------------

    public int getPetCount() { return petCount; }

    public int petSpotsLeft() {
        return MAX_PETS - petCount;
    }

    public void addPet() {
        if (petCount >= MAX_PETS) {
            throw new IllegalStateException("This flight already has " + MAX_PETS + " pets.");
        }
        petCount++;
    }

    public void removePet() {
        if (petCount > 0) {
            petCount--;
        }
    }

    // ------------------------------------------------------------------
    // Validation rules
    // ------------------------------------------------------------------

    /** Any airline's flight number: its 2-letter code and 1-4 digits, e.g. TF204. */
    public static String validateFlightNumber(String raw) throws InvalidInputException {
        String value = raw.replace(" ", "").toUpperCase();
        Airline airline = Airline.fromFlightNumber(value);
        if (airline == null || !value.matches("^[A-Z]{2}\\d{1,4}$")) {
            throw new InvalidInputException("Flight number must be an airline code (MW, AB, TF, PM) and 1-4 digits,"
                    + " e.g. MW101.");
        }
        return value;
    }

    /** Overloaded: when adding a flight, the number must use that airline's own code. */
    public static String validateFlightNumber(String raw, Airline airline) throws InvalidInputException {
        String value = raw.replace(" ", "").toUpperCase();
        if (!value.matches("^" + airline.getCode() + "\\d{1,4}$")) {
            throw new InvalidInputException(airline.getName() + " flight numbers are " + airline.getCode()
                    + " and 1-4 digits, e.g. " + airline.getCode() + "123.");
        }
        return value;
    }

    public static int validateDuration(String raw) throws InvalidInputException {
        return InputHelper.parseWholeNumber(raw, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES, "Flight time (minutes)");
    }

    public static double validateBaseFare(String raw) throws InvalidInputException {
        return InputHelper.parseAmount(raw, MAX_BASE_FARE, "Base fare");
    }
}