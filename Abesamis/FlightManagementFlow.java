import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Admin: add, view, update, reschedule, change the fare of, cancel, and remove flights.
 * Cancelling a flight cancels every booking on it with a full refund.
 */
public final class FlightManagementFlow {

    private FlightManagementFlow() {
    }

    public static void run() {
        ConsoleStyle.enter("Manage Flights");
        try {
            runMenu();
        } finally {
            ConsoleStyle.leave();
        }
    }

    private static void runMenu() {
        while (true) {
            ConsoleStyle.menuHeader("Manage Flights");
            System.out.println("  1 - Add flight");
            System.out.println("  2 - View all flights");
            System.out.println("  3 - Update flight status");
            System.out.println("  4 - Reschedule flight");
            System.out.println("  5 - Change base fare");
            System.out.println("  6 - Cancel flight");
            System.out.println("  7 - Remove flight");
            System.out.println("  8 - Back");
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 8);
            if (choice == 8) {
                return;
            }
            try {
                switch (choice) {
                    case 1: addFlight(); break;
                    case 2: viewAll(); break;
                    case 3: updateStatus(); break;
                    case 4: reschedule(); break;
                    case 5: changeBaseFare(); break;
                    case 6: cancelFlight(); break;
                    default: removeFlight(); break;
                }
            } catch (OperationCancelledException e) {
                System.out.println();
                System.out.println(ConsoleStyle.error(e.getMessage()));
            }
        }
    }

    // ------------------------------------------------------------------

    private static void addFlight() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Add Flight");
        ConsoleStyle.cancelHint();

        Airline airline = SearchFlow.pickAirline(false);
        System.out.println();
        String number = InputHelper.read("Flight number (" + airline.getCode() + " + 1-4 digits): ", raw -> {
            String value = Flight.validateFlightNumber(raw, airline);
            if (system.flightNumberTaken(value)) {
                throw new InvalidInputException("Flight " + value + " already exists.");
            }
            return value;
        });
        System.out.println();
        Airport origin = SearchFlow.pickAirport("From", null);
        System.out.println();
        Airport destination = SearchFlow.pickAirport("To", origin);
        LocalDateTime departure = readFutureDeparture();
        int duration = InputHelper.read("Flight time in minutes (" + Flight.MIN_DURATION_MINUTES + "-"
                + Flight.MAX_DURATION_MINUTES + "): ", Flight::validateDuration);

        // Each airline only flies its own planes.
        List<AircraftType> types = airline.getAircraftTypes();
        AircraftType aircraft;
        if (types.size() == 1) {
            aircraft = types.get(0);
            System.out.println("Aircraft: " + describe(aircraft) + "   (" + airline.getName() + " only flies this plane)");
        } else {
            System.out.println("Aircraft:");
            for (int i = 0; i < types.size(); i++) {
                System.out.println("  " + (i + 1) + " - " + describe(types.get(i)));
            }
            System.out.println();
            aircraft = types.get(InputHelper.readChoice("Aircraft (1-" + types.size() + "): ", 1, types.size()) - 1);
        }
        double fare = InputHelper.read("Economy base fare (Php): ", Flight::validateBaseFare);

        Flight flight = new Flight(airline, number, origin, destination, departure, duration, aircraft, fare);
        ConsoleStyle.section("Review Flight");
        System.out.println(ConsoleStyle.row("Airline", airline.getName()));
        System.out.println(ConsoleStyle.row("Flight", number + "   " + flight.getRouteLabel()));
        System.out.println(ConsoleStyle.row("Departure", ConsoleStyle.dateTime(departure)
                + "   arrives " + ConsoleStyle.time(flight.getArrival())));
        System.out.println(ConsoleStyle.row("Aircraft", aircraft.getName() + " (" + flight.totalSeats() + " seats)"));
        System.out.println(ConsoleStyle.row("Economy base fare", fare));
        if (!InputHelper.confirm("Add this flight? (Y/N): ")) {
            System.out.println("Flight not added.");
            return;
        }
        system.addFlight(flight);
        system.save();
        System.out.println("Flight " + number + " added.");
    }

    private static void viewAll() {
        ConsoleStyle.header("All Flights");
        List<Flight> flights = AirlineSystem.getInstance().getFlights();
        if (flights.isEmpty()) {
            System.out.println("No flights yet.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        System.out.printf("%-7s %-9s %-17s %-16s %-11s %s%n", "Flight", "Route", "Departure", "Aircraft", "Booked", "Status");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (Flight f : flights) {
            String booked = (f.totalSeats() - f.availableSeats()) + "/" + f.totalSeats();
            System.out.printf("%-7s %-9s %-17s %-16s %-11s %s%n", f.getFlightNumber(),
                    f.getOrigin().name() + "-" + f.getDestination().name(), ConsoleStyle.dateTime(f.getDeparture()),
                    f.getAircraftType().getName(), booked, f.getStatusAt(now).getLabel());
        }
    }

    private static void updateStatus() {
        ConsoleStyle.header("Update Flight Status");
        ConsoleStyle.cancelHint();
        Flight flight = readChangeableFlight();
        if (flight == null) {
            return;
        }
        System.out.println("Current status: " + flight.getStatus().getLabel());
        System.out.println("  1 - Scheduled");
        System.out.println("  2 - Delayed");
        FlightStatus status = InputHelper.readChoice("Choice: ", 1, 2) == 1 ? FlightStatus.SCHEDULED : FlightStatus.DELAYED;
        if (status == flight.getStatus()) {
            System.out.println("Flight " + flight.getFlightNumber() + " is already " + status.getLabel() + ".");
            return;
        }
        flight.setStatus(status);
        AirlineSystem.getInstance().save();
        System.out.println("Flight " + flight.getFlightNumber() + " is now " + status.getLabel() + ".");
        System.out.println("(To cancel a flight, use Cancel flight so every booking gets refunded.)");
    }

    private static void reschedule() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Reschedule Flight");
        ConsoleStyle.cancelHint();
        Flight flight = readChangeableFlight();
        if (flight == null) {
            return;
        }
        System.out.println("Current departure: " + ConsoleStyle.dateTime(flight.getDeparture()));
        LocalDateTime newDeparture = readFutureDeparture();
        if (newDeparture.equals(flight.getDeparture())) {
            System.out.println("That's the same departure time. Nothing changed.");
            return;
        }
        int affected = system.activeBookingsOn(flight.getFlightNumber()).size();
        if (!InputHelper.confirm("Move " + flight.getFlightNumber() + " to " + ConsoleStyle.dateTime(newDeparture)
                + "? " + affected + " booking(s) keep their seats. (Y/N): ")) {
            System.out.println("Flight not rescheduled.");
            return;
        }
        flight.reschedule(newDeparture);
        system.save();
        System.out.println("Flight " + flight.getFlightNumber() + " rescheduled to " + ConsoleStyle.dateTime(newDeparture)
                + " (" + flight.getStatus().getLabel() + ").");
    }

    /** Fare changes only affect new bookings, like a real airline's price update. */
    private static void changeBaseFare() {
        ConsoleStyle.header("Change Base Fare");
        ConsoleStyle.cancelHint();
        Flight flight = readChangeableFlight();
        if (flight == null) {
            return;
        }
        System.out.println("Current Economy base fare: " + ConsoleStyle.peso(flight.getBaseFare())
                + "   (Premium Economy x1.6, Business x3.0)");
        double newFare = InputHelper.read("New Economy base fare: ", Flight::validateBaseFare);
        if (newFare == flight.getBaseFare()) {
            System.out.println("That's the same fare. Nothing changed.");
            return;
        }
        if (!InputHelper.confirm("Change " + flight.getFlightNumber() + " to " + ConsoleStyle.peso(newFare)
                + "? Existing bookings keep the price they paid. (Y/N): ")) {
            System.out.println("Fare not changed.");
            return;
        }
        flight.setBaseFare(newFare);
        AirlineSystem.getInstance().save();
        System.out.println("Base fare of " + flight.getFlightNumber() + " is now " + ConsoleStyle.peso(newFare)
                + ". New bookings use this fare.");
    }

    private static void cancelFlight() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Cancel Flight");
        ConsoleStyle.cancelHint();
        Flight flight = readChangeableFlight();
        if (flight == null) {
            return;
        }
        List<Booking> active = system.activeBookingsOn(flight.getFlightNumber());
        if (!InputHelper.confirm("Cancel " + flight.getFlightNumber() + " and refund " + active.size()
                + " booking(s) in full? (Y/N): ")) {
            System.out.println("Flight not cancelled.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Booking b : active) {
            for (Traveler t : b.getSeatedTravelers()) {
                flight.releaseSeat(t.getSeatLabel());
                if (t.getPet() != null) {
                    flight.removePet();
                }
            }
            b.cancel(b.getTotalPaid(), now);
        }
        flight.setStatus(FlightStatus.CANCELLED);
        system.save();
        System.out.println("Flight " + flight.getFlightNumber() + " cancelled. " + active.size()
                + " booking(s) refunded in full.");
    }

    private static void removeFlight() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Remove Flight");
        ConsoleStyle.cancelHint();
        Flight flight = readExistingFlight();
        int active = system.activeBookingsOn(flight.getFlightNumber()).size();
        if (active > 0) {
            System.out.println(ConsoleStyle.error("Flight " + flight.getFlightNumber() + " has " + active
                    + " active booking(s). Cancel the flight first so they get refunded."));
            return;
        }
        if (!InputHelper.confirm("Remove " + flight.getFlightNumber() + " permanently? (Y/N): ")) {
            System.out.println("Flight not removed.");
            return;
        }
        system.removeFlight(flight.getFlightNumber());
        system.save();
        System.out.println("Flight " + flight.getFlightNumber() + " removed.");
    }

    // ------------------------------------------------------------------
    // Shared prompts
    // ------------------------------------------------------------------

    private static Flight readExistingFlight() {
        AirlineSystem system = AirlineSystem.getInstance();
        return InputHelper.read("Flight number: ", raw -> {
            String number = raw.replace(" ", "").toUpperCase();
            try {
                return system.findFlight(number);
            } catch (FlightNotFoundException e) {
                throw new InvalidInputException(e.getMessage());
            }
        });
    }

    /** A flight that can still be changed: not cancelled and not departed. Returns null otherwise. */
    private static Flight readChangeableFlight() {
        Flight flight = readExistingFlight();
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            System.out.println(ConsoleStyle.error("Flight " + flight.getFlightNumber() + " is already cancelled."));
            return null;
        }
        if (flight.hasDeparted(LocalDateTime.now())) {
            System.out.println(ConsoleStyle.error("Flight " + flight.getFlightNumber() + " has already departed."));
            return null;
        }
        return flight;
    }

    private static LocalDateTime readFutureDeparture() {
        while (true) {
            LocalDate date = InputHelper.read("Departure date (YYYY-MM-DD): ", SearchFlow::validateTravelDate);
            LocalTime time = InputHelper.readTime("Departure time (HH:MM, 24-hour): ");
            LocalDateTime departure = LocalDateTime.of(date, time);
            if (departure.isAfter(LocalDateTime.now())) {
                return departure;
            }
            System.out.println(ConsoleStyle.error("That time has already passed. Enter a future date and time."));
            System.out.println();
        }
    }

    private static String describe(AircraftType type) {
        StringBuilder sb = new StringBuilder(type.getName() + ": ");
        List<CabinLayout> cabins = type.getCabins();
        for (int i = 0; i < cabins.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(cabins.get(i).getCabinName());
        }
        return sb.toString();
    }
}