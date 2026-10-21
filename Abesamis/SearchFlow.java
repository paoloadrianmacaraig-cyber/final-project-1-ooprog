import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Searching flights by route and date. Used on its own (Search Flights)
 * and as the first step of booking (searchAndPick).
 */
public final class SearchFlow {

    private static final int NEARBY_DAYS = 3;

    private SearchFlow() {
    }

    public static void searchOnly() {
        ConsoleStyle.header("Search Flights");
        ConsoleStyle.cancelHint();
        List<Flight> results = searchOnce();
        if (!results.isEmpty()) {
            System.out.println("To book, choose Book a Flight from the menu.");
        }
    }

    /** Keeps searching until the user picks a bookable flight; "cancel" backs out. */
    public static Flight searchAndPick() {
        while (true) {
            List<Flight> results = searchOnce();
            if (results.isEmpty()) {
                System.out.println("Try another route, date, or airline.");
                System.out.println();
                continue;
            }
            int choice = InputHelper.readChoice("Pick a flight (1-" + results.size() + "): ", 1, results.size());
            Flight flight = results.get(choice - 1);
            LocalDateTime now = LocalDateTime.now();
            if (flight.isBookable(now)) {
                return flight;
            }
            System.out.println();
            if (flight.getStatus() == FlightStatus.CANCELLED) {
                System.out.println("Flight " + flight.getFlightNumber() + " is cancelled and can't be booked.");
            } else if (flight.hasDeparted(now)) {
                System.out.println("Flight " + flight.getFlightNumber() + " has already departed.");
            } else {
                System.out.println("Flight " + flight.getFlightNumber() + " is fully booked.");
            }
            System.out.println("Search again.");
        }
    }

    /** Asks for route and date, prints matching flights, and returns them in the printed order. */
    private static List<Flight> searchOnce() {
        Airport origin = pickAirport("From", null);
        System.out.println();
        Airport destination = pickAirport("To", origin);
        LocalDate date = InputHelper.read("Travel date (YYYY-MM-DD): ", SearchFlow::validateTravelDate);
        System.out.println();
        Airline airline = pickAirline(true);   // null = any airline

        AirlineSystem system = AirlineSystem.getInstance();
        List<Flight> results = new ArrayList<>();
        for (Flight f : system.getFlights()) {
            if (f.getOrigin() == origin && f.getDestination() == destination
                    && f.getDeparture().toLocalDate().equals(date)
                    && (airline == null || f.getAirline() == airline)) {
                results.add(f);
            }
        }

        System.out.println();
        if (results.isEmpty()) {
            String who = airline == null ? "No flights" : "No " + airline.getName() + " flights";
            System.out.println(who + " from " + origin.getLabel() + " to " + destination.getLabel() + " on "
                    + ConsoleStyle.date(date) + ".");
            printNearbyDates(system, origin, destination, date);
            return results;
        }
        printResults(results);
        return results;
    }

    public static void printResults(List<Flight> flights) {
        LocalDateTime now = LocalDateTime.now();
        System.out.printf("%-3s %-6s %-14s %-7s %-7s %-6s %-14s %s%n",
                "No.", "Flight", "Airline", "Departs", "Arrives", "Seats", "Economy from", "Status");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            System.out.printf("%-3d %-6s %-14s %-7s %-7s %-6d %-14s %s%n", i + 1, f.getFlightNumber(),
                    f.getAirline().getName(), ConsoleStyle.time(f.getDeparture()), ConsoleStyle.time(f.getArrival()),
                    f.availableSeats(), ConsoleStyle.peso(f.getBaseFare()), f.getStatusAt(now).getLabel());
        }
        System.out.println();
        // One line per airline in the results, so the differences are clear before picking.
        List<Airline> shown = new ArrayList<>();
        for (Flight f : flights) {
            if (!shown.contains(f.getAirline())) {
                shown.add(f.getAirline());
                System.out.println("  " + f.getAirline().getPerksLine());
            }
        }
        System.out.println();
    }

    private static void printNearbyDates(AirlineSystem system, Airport origin, Airport destination, LocalDate date) {
        LocalDate today = LocalDate.now();
        List<Flight> nearby = new ArrayList<>();
        for (Flight f : system.getFlights()) {
            LocalDate d = f.getDeparture().toLocalDate();
            if (f.getOrigin() == origin && f.getDestination() == destination && !d.isBefore(today)
                    && !d.isBefore(date.minusDays(NEARBY_DAYS)) && !d.isAfter(date.plusDays(NEARBY_DAYS))
                    && f.getStatus() != FlightStatus.CANCELLED) {
                nearby.add(f);
            }
        }
        if (!nearby.isEmpty()) {
            System.out.println("Same route within " + NEARBY_DAYS + " days:");
            for (Flight f : nearby) {
                System.out.println("  " + ConsoleStyle.dateTime(f.getDeparture()) + "  " + f.getFlightNumber()
                        + "  " + f.getAirline().getName());
            }
        }
    }

    /** Numbered airline list. With allowAny, choice 1 is "Any airline" and returns null. */
    public static Airline pickAirline(boolean allowAny) {
        Airline[] airlines = Airline.values();
        int offset = allowAny ? 1 : 0;
        System.out.println("Airline:");
        if (allowAny) {
            System.out.println("   1 - Any airline");
        }
        for (int i = 0; i < airlines.length; i++) {
            System.out.printf("  %2d - %-20s %s%n", i + 1 + offset, airlines[i].getLabel(), airlines[i].getKind());
        }
        System.out.println();
        int max = airlines.length + offset;
        int choice = InputHelper.readChoice("Airline (1-" + max + "): ", 1, max);
        return (allowAny && choice == 1) ? null : airlines[choice - 1 - offset];
    }

    /** Numbered airport list; "exclude" removes the origin from the destination list. */
    public static Airport pickAirport(String label, Airport exclude) {
        List<Airport> options = new ArrayList<>();
        for (Airport a : Airport.values()) {
            if (a != exclude) {
                options.add(a);
            }
        }
        System.out.println(label + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %2d - %s%n", i + 1, options.get(i).getLabel());
        }
        System.out.println();
        return options.get(InputHelper.readChoice(label + " (1-" + options.size() + "): ", 1, options.size()) - 1);
    }

    /** Today up to 1 year ahead. */
    public static LocalDate validateTravelDate(String raw) throws InvalidInputException {
        LocalDate date = InputHelper.parseDate(raw);
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new InvalidInputException("The date can't be in the past.");
        }
        if (date.isAfter(today.plusYears(1))) {
            throw new InvalidInputException("The date can only be up to 1 year ahead.");
        }
        return date;
    }
}