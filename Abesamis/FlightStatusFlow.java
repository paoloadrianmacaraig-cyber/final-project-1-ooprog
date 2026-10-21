import java.time.LocalDateTime;

/**
 * Flight Status: anyone can check a flight from the main menu without
 * logging in, the way airline websites have a "Flight Status" page.
 */
public final class FlightStatusFlow {

    private FlightStatusFlow() {
    }

    public static void run() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Flight Status");
        ConsoleStyle.cancelHint();
        Flight flight = InputHelper.read("Flight number (e.g. MW201): ", raw -> {
            String number = Flight.validateFlightNumber(raw);
            try {
                return system.findFlight(number);
            } catch (FlightNotFoundException e) {
                throw new InvalidInputException(e.getMessage());
            }
        });
        printStatus(flight, LocalDateTime.now());
    }

    public static void printStatus(Flight flight, LocalDateTime now) {
        FlightStatus status = flight.getStatusAt(now);
        System.out.println();
        System.out.println(flight);   // Flight.toString(): number, route, departure
        System.out.println(ConsoleStyle.row("Airline", flight.getAirline().getName()));
        System.out.println(ConsoleStyle.row("Status", status.getLabel()));
        if (status == FlightStatus.CANCELLED) {
            System.out.println("  This flight is cancelled. Bookings on it were refunded in full.");
            return;
        }
        System.out.println(ConsoleStyle.row("Departs", ConsoleStyle.dateTime(flight.getDeparture())));
        System.out.println(ConsoleStyle.row("Arrives", ConsoleStyle.dateTime(flight.getArrival())));
        System.out.println(ConsoleStyle.row("Aircraft", flight.getAircraftType().getName()));
        System.out.println(ConsoleStyle.row("Gate", flight.getGate()));
        if (status == FlightStatus.DEPARTED) {
            return;
        }
        LocalDateTime opens = flight.getDeparture().minusHours(CheckInFlow.OPENS_HOURS_BEFORE);
        LocalDateTime closes = flight.getDeparture().minusHours(CheckInFlow.CLOSES_HOURS_BEFORE);
        String checkIn;
        if (now.isBefore(opens)) {
            checkIn = "Opens " + ConsoleStyle.dateTime(opens);
        } else if (now.isBefore(closes)) {
            checkIn = "Open until " + ConsoleStyle.time(closes);
        } else {
            checkIn = "Closed";
        }
        System.out.println(ConsoleStyle.row("Online check-in", checkIn));
    }
}