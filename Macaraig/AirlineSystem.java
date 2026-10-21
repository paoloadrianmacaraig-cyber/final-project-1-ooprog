import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class AirlineSystem {

    public static final String AIRLINE_NAME = "Airline Reservation System";
    public static final String TAGLINE = "Fast Booking. Smooth Flying.";

    private static AirlineSystem instance;

    private final SystemData data;

    private AirlineSystem() {
        SystemData loaded = DataStore.load();
        if (loaded == null) {
            data = new SystemData();
            seedDemoData();
            save();
        } else {
            data = loaded;
        }
    }

    public static AirlineSystem getInstance() {
        if (instance == null) {
            instance = new AirlineSystem();
        }
        return instance;
    }

    public void save() {
        DataStore.save(data);
    }

    public User findUser(String username) {
        return data.getUsers().get(username.toLowerCase());
    }

    public boolean usernameTaken(String username) {
        return findUser(username) != null;
    }

    public void addUser(User user) {
        data.getUsers().put(user.getUsername().toLowerCase(), user);
    }

    public Collection<User> getUsers() {
        return data.getUsers().values();
    }

    public boolean hasSampleData() {
        return data.hasSampleData();
    }

    public int countActiveAdmins() {
        int count = 0;
        for (User u : getUsers()) {
            if (u instanceof Admin && u.isActive()) {
                count++;
            }
        }
        return count;
    }

    public User authenticate(String username, String password, boolean staffPortal) throws InvalidLoginException {
        User user = findUser(username);
        if (user == null || user.isEmployee() != staffPortal) {
            throw new InvalidLoginException("Incorrect username or password.");
        }
        if (user.isLocked()) {
            throw new InvalidLoginException("This account is locked after " + User.MAX_FAILED_LOGINS
                    + " wrong passwords. Ask an Admin to unlock it.");
        }
        if (!user.checkPassword(password)) {
            boolean nowLocked = user.recordFailedLogin();
            save();
            if (nowLocked) {
                throw new InvalidLoginException("Too many wrong passwords. This account is now locked."
                        + " Ask an Admin to unlock it.");
            }
            throw new InvalidLoginException("Incorrect username or password.");
        }
        if (!user.isActive()) {
            throw new InvalidLoginException("This account is deactivated. Contact an Admin.");
        }
        user.resetFailedLogins();
        save();
        return user;
    }

    public List<Flight> getFlights() {
        List<Flight> flights = new ArrayList<>(data.getFlights().values());
        flights.sort(Comparator.comparing(Flight::getDeparture));
        return flights;
    }

    public Flight findFlight(String flightNumber) throws FlightNotFoundException {
        Flight flight = data.getFlights().get(flightNumber.toUpperCase());
        if (flight == null) {
            throw new FlightNotFoundException(flightNumber.toUpperCase());
        }
        return flight;
    }

    public Flight findFlightOrNull(String flightNumber) {
        return data.getFlights().get(flightNumber.toUpperCase());
    }

    public boolean flightNumberTaken(String flightNumber) {
        return data.getFlights().containsKey(flightNumber.toUpperCase());
    }

    public void addFlight(Flight flight) {
        data.getFlights().put(flight.getFlightNumber(), flight);
    }

    public void removeFlight(String flightNumber) {
        data.getFlights().remove(flightNumber.toUpperCase());
    }

    public List<Booking> getBookings() {
        List<Booking> bookings = new ArrayList<>(data.getBookings().values());
        bookings.sort(Comparator.comparing(Booking::getCreatedAt));
        return bookings;
    }

    public void addBooking(Booking booking) {
        data.getBookings().put(booking.getPnr(), booking);
    }

    public String generatePnr() {
        return Booking.generatePnr(pnr -> data.getBookings().containsKey(pnr));
    }

    public Booking findBooking(String pnr, String lastName) throws BookingNotFoundException {
        Booking booking = data.getBookings().get(pnr.toUpperCase());
        if (booking == null || !booking.matchesLastName(lastName)) {
            throw new BookingNotFoundException();
        }
        return booking;
    }

    /** Bookings made by one account, newest first. */
    public List<Booking> bookingsBy(String username) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : data.getBookings().values()) {
            if (b.getBookedBy().equalsIgnoreCase(username)) {
                result.add(b);
            }
        }
        result.sort(Comparator.comparing(Booking::getCreatedAt).reversed());
        return result;
    }

    public List<Booking> bookingsOnFlight(String flightNumber) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : data.getBookings().values()) {
            if (b.getFlightNumber().equalsIgnoreCase(flightNumber)) {
                result.add(b);
            }
        }
        return result;
    }

    public List<Booking> activeBookingsOn(String flightNumber) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookingsOnFlight(flightNumber)) {
            if (b.isActive()) {
                result.add(b);
            }
        }
        return result;
    }

    private void seedDemoData() {
        data.setSampleData(true);
        addUser(new Admin("admin", "admin123", "System Administrator"));
        addUser(new Staff("staff01", "staff123", "Carla Mendoza"));
        addUser(new Passenger("passenger01", "passenger123", "Miguel Reyes"));

        LocalDate today = LocalDate.now();
        LocalDateTime soon = LocalDateTime.now().plusHours(6).truncatedTo(ChronoUnit.HOURS);

        Flight ab101 = new Flight(Airline.AIR_ABESAMIS, "AB101", Airport.MNL, Airport.CEB, soon, 80,
                AircraftType.a321(), 3200);
        Flight mw201 = new Flight(Airline.MATEO_AIRWAYS, "MW201", Airport.MNL, Airport.DVO, at(today, 2, 9, 15), 115,
                AircraftType.a350(), 4800);
        List<Flight> flights = Arrays.asList(
                ab101,
                new Flight(Airline.AIR_ABESAMIS, "AB102", Airport.CEB, Airport.MNL, at(today, 1, 7, 30), 80,
                        AircraftType.a321(), 3200),
                new Flight(Airline.TENGOFLY, "TF111", Airport.MNL, Airport.CEB, at(today, 2, 6, 0), 80,
                        AircraftType.a321AllEconomy(), 2400),
                new Flight(Airline.AIR_ABESAMIS, "AB105", Airport.MNL, Airport.CEB, at(today, 2, 12, 0), 80,
                        AircraftType.a321(), 3300),
                new Flight(Airline.MATEO_AIRWAYS, "MW701", Airport.MNL, Airport.CEB, at(today, 2, 18, 30), 80,
                        AircraftType.a350(), 3600),
                new Flight(Airline.TENGOFLY, "TF115", Airport.MNL, Airport.CEB, at(today, 2, 21, 0), 80,
                        AircraftType.a321AllEconomy(), 2700),
                mw201,
                new Flight(Airline.PAM_AVIATION, "PM301", Airport.MNL, Airport.ILO, at(today, 2, 14, 0), 65,
                        AircraftType.atr72(), 2600),
                new Flight(Airline.MATEO_AIRWAYS, "MW203", Airport.MNL, Airport.DVO, at(today, 4, 8, 0), 115,
                        AircraftType.a350(), 4600),
                new Flight(Airline.MATEO_AIRWAYS, "MW205", Airport.DVO, Airport.MNL, at(today, 3, 13, 10), 115,
                        AircraftType.a350(), 4800),
                new Flight(Airline.PAM_AVIATION, "PM305", Airport.MNL, Airport.PPS, at(today, 3, 6, 45), 75,
                        AircraftType.atr72(), 2900),
                new Flight(Airline.PAM_AVIATION, "PM401", Airport.MNL, Airport.TAG, at(today, 4, 10, 30), 75,
                        AircraftType.atr72(), 3100),
                new Flight(Airline.AIR_ABESAMIS, "AB901", Airport.MNL, Airport.ZAM, at(today, 4, 12, 0), 100,
                        AircraftType.a321(), 3400),
                new Flight(Airline.TENGOFLY, "TF501", Airport.CEB, Airport.DVO, at(today, 5, 16, 20), 60,
                        AircraftType.a321AllEconomy(), 2200),
                new Flight(Airline.PAM_AVIATION, "PM601", Airport.MNL, Airport.KLO, at(today, 6, 8, 0), 60,
                        AircraftType.atr72(), 2800));
        for (Flight f : flights) {
            addFlight(f);
        }
        findFlightOrNull("MW205").setStatus(FlightStatus.DELAYED);
        findFlightOrNull("AB901").setStatus(FlightStatus.CANCELLED);

        Traveler miguel = new Traveler("Miguel Reyes", LocalDate.of(1990, 5, 14), PassengerType.ADULT);
        Traveler bea = new Traveler("Bea Reyes", today.minusYears(7), PassengerType.CHILD);
        Traveler lia = new Traveler("Lia Reyes", today.minusMonths(10), PassengerType.INFANT);
        miguel.setSeatLabel("12A");
        miguel.setBaggage(BaggageOption.KG20);
        bea.setSeatLabel("12B");
        seedBooking("RYS4K7", ab101, "passenger01", new EconomyFare(), Arrays.asList(miguel, bea, lia),
                "GCash", "0917 *** 4567", "GC-482913");

        Traveler lourdes = new Traveler("Lourdes Garcia", LocalDate.of(1955, 2, 11), PassengerType.ADULT,
                FareDiscount.SENIOR, "SC-0451233");
        lourdes.setSeatLabel("2A");
        seedBooking("GRC8M2", mw201, "staff01", new BusinessFare(), Arrays.asList(lourdes),
                "Credit/Debit Card", "**** 1111", "CC-730215");
    }

    private static LocalDateTime at(LocalDate today, int daysAhead, int hour, int minute) {
        return LocalDateTime.of(today.plusDays(daysAhead), LocalTime.of(hour, minute));
    }

    private void seedBooking(String pnr, Flight flight, String bookedBy, FareStrategy fare, List<Traveler> travelers,
            String method, String masked, String reference) {
        try {
            for (Traveler t : travelers) {
                if (t.isSeated()) {
                    flight.bookSeat(t.getSeatLabel(), fare.getCabinName());
                }
            }
        } catch (SeatUnavailableException e) {
            throw new IllegalStateException("Demo data has a bad seat: " + e.getMessage(), e);
        }
        double total = FareBreakdown.forBooking(fare, flight, travelers).getTotal();
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(pnr, flight.getFlightNumber(), bookedBy, fare, travelers, now);
        booking.addPayment(new PaymentRecord("Booking", method, masked, reference, total, now));
        booking.setTicketFare(FareBreakdown.ticketFare(fare, flight, travelers));
        addBooking(booking);
    }
}