import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Booking a flight, for a passenger or for a walk-in at the Staff counter.
 *
 * Order: search -> cabin -> travelers -> seats -> add-ons -> review ->
 * payment -> confirmed. Seats are only locked once payment succeeds, so
 * quitting anywhere before that leaves nothing held.
 */
public final class BookingFlow {

    private BookingFlow() {
    }

    public static void run(String bookedBy) {
        ConsoleStyle.header("Book a Flight");
        ConsoleStyle.cancelHint();

        AirlineSystem system = AirlineSystem.getInstance();
        Flight flight = SearchFlow.searchAndPick();

        FareStrategy fare;
        List<Traveler> travelers;
        while (true) {
            fare = pickCabin(flight);
            travelers = TravelerDetailsFlow.collect(flight);
            int seatedCount = countSeated(travelers);
            int free = flight.availableSeatsIn(fare.getCabinName());
            if (seatedCount <= free) {
                break;
            }
            System.out.println();
            System.out.println("Only " + free + " seat(s) left in " + fare.getCabinName()
                    + ". Pick another cabin or book fewer travelers.");
        }

        selectSeats(flight, fare.getCabinName(), travelers);
        BaggageFlow.chooseFor(flight.getAirline(), travelers);
        PetFlow.offer(flight, travelers);

        FareBreakdown breakdown = FareBreakdown.forBooking(fare, flight, travelers);
        printReview(flight, fare, travelers, breakdown);
        if (!InputHelper.readYesNo("Continue to payment? (Y/N): ")) {
            System.out.println("Booking not made. No seats were held.");
            return;
        }

        PaymentRecord payment = PaymentFlow.collectPayment(breakdown.getTotal(), "Booking");
        if (payment == null) {
            System.out.println("Booking not made. No seats were held.");
            return;
        }

        // Lock the seats only now that the booking is paid for.
        List<String> locked = new ArrayList<>();
        try {
            for (Traveler t : travelers) {
                if (t.isSeated()) {
                    flight.bookSeat(t.getSeatLabel(), fare.getCabinName());
                    locked.add(t.getSeatLabel());
                }
            }
        } catch (SeatUnavailableException e) {
            for (String label : locked) {
                flight.releaseSeat(label);
            }
            System.out.println(ConsoleStyle.error(e.getMessage() + " The payment was voided. Please book again."));
            return;
        }
        for (Traveler t : travelers) {
            if (t.getPet() != null) {
                flight.addPet();
            }
        }

        String pnr = system.generatePnr();
        Booking booking = new Booking(pnr, flight.getFlightNumber(), bookedBy, fare, travelers, LocalDateTime.now());
        booking.addPayment(payment);
        booking.setTicketFare(FareBreakdown.ticketFare(fare, flight, travelers));
        system.addBooking(booking);
        system.save();

        System.out.println();
        System.out.println("Booking confirmed! Your PNR is " + pnr + ".");
        ETicket.print(booking);
        if (InputHelper.readYesNo("Save a copy of the e-ticket as a text file? (Y/N): ")) {
            System.out.println();
            ETicket.save(booking);
        }
    }

    // ------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------

    private static FareStrategy pickCabin(Flight flight) {
        ConsoleStyle.section("Cabin");
        List<CabinLayout> cabins = flight.getAircraftType().getCabins();
        if (cabins.size() == 1) {
            // Low-cost and regional planes only have Economy, so there's nothing to pick.
            String only = cabins.get(0).getCabinName();
            System.out.println(flight.getAirline().getName() + " flies " + only + " only on this plane.");
            return FareStrategy.forCabin(only);
        }
        for (int i = 0; i < cabins.size(); i++) {
            CabinLayout cabin = cabins.get(i);
            FareStrategy fare = FareStrategy.forCabin(cabin.getCabinName());
            System.out.printf("  %d - %-16s base fare %s   %d seat(s) left%n", i + 1, cabin.getCabinName(),
                    ConsoleStyle.peso(fare.calculateBaseFare(flight.getBaseFare())),
                    flight.availableSeatsIn(cabin.getCabinName()));
        }
        int choice = InputHelper.readChoice("Choice: ", 1, cabins.size());
        return FareStrategy.forCabin(cabins.get(choice - 1).getCabinName());
    }

    private static void selectSeats(Flight flight, String cabinName, List<Traveler> travelers) {
        Set<String> heldNow = new HashSet<>();
        ConsoleStyle.section("Seats");
        printSeatMap(flight, cabinName, heldNow);
        for (Traveler t : travelers) {
            if (!t.isSeated()) {
                continue;
            }
            String label = InputHelper.read("Seat for " + t.getFullName() + ": ",
                    raw -> checkSeatChoice(flight, cabinName, raw, heldNow));
            heldNow.add(label);
            t.setSeatLabel(label);
        }
    }

    /** One seat pick: right shape, exists, right cabin, free, and not already picked in this booking. */
    public static String checkSeatChoice(Flight flight, String cabinName, String raw, Set<String> heldNow)
            throws InvalidInputException {
        String label = Seat.normalizeLabel(raw);
        try {
            flight.checkSeatAvailable(label, cabinName);
        } catch (SeatUnavailableException e) {
            throw new InvalidInputException(e.getMessage());
        }
        if (heldNow.contains(label)) {
            throw new InvalidInputException("Seat " + label + " is already picked for another traveler in this booking.");
        }
        return label;
    }

    private static int countSeated(List<Traveler> travelers) {
        int count = 0;
        for (Traveler t : travelers) {
            if (t.isSeated()) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Screens
    // ------------------------------------------------------------------

    /** Draws one cabin from its layout template, so aisles always line up. */
    public static void printSeatMap(Flight flight, String cabinName, Set<String> heldNow) {
        CabinLayout layout = flight.getCabinLayout(cabinName);
        System.out.println(cabinName + " cabin   [ ] free   [X] taken   [*] your pick");
        StringBuilder header = new StringBuilder("      ");
        for (ColumnSlot slot : layout.getTemplate()) {
            header.append(slot.isSeat() ? " " + slot.getLetter() + "  " : "  ");
        }
        System.out.println(header);
        for (int row : layout.actualRowNumbers()) {
            StringBuilder line = new StringBuilder(String.format("  %2d  ", row));
            for (ColumnSlot slot : layout.getTemplate()) {
                if (slot.isAisle()) {
                    line.append("  ");
                    continue;
                }
                String label = row + String.valueOf(slot.getLetter());
                if (heldNow.contains(label)) {
                    line.append("[*] ");
                } else if (flight.getSeat(label).isBooked()) {
                    line.append("[X] ");
                } else {
                    line.append("[ ] ");
                }
            }
            System.out.println(line);
        }
    }

    private static void printReview(Flight flight, FareStrategy fare, List<Traveler> travelers, FareBreakdown breakdown) {
        ConsoleStyle.section("Review Booking");
        System.out.println(ConsoleStyle.row("Airline", flight.getAirline().getName()));
        System.out.println(ConsoleStyle.row("Flight", flight.getFlightNumber() + "   " + flight.getRouteLabel()));
        System.out.println(ConsoleStyle.row("Departure", ConsoleStyle.dateTime(flight.getDeparture())
                + "   arrives " + ConsoleStyle.time(flight.getArrival())));
        System.out.println(ConsoleStyle.row("Cabin", fare.getCabinName()));
        System.out.println();
        for (Traveler t : travelers) {
            String seat = t.isSeated() ? "seat " + t.getSeatLabel() : "on lap";
            String discount = t.getDiscount() == FareDiscount.NONE ? "" : ", " + t.getDiscount().getLabel();
            System.out.println("  " + t.getFullName() + " (" + t.getPassengerType().getLabel() + discount + "), " + seat);
        }
        System.out.println();
        printBreakdown(breakdown);
    }

    public static void printBreakdown(FareBreakdown breakdown) {
        for (FareBreakdown.Line line : breakdown.getLines()) {
            if (line.isHeading()) {
                System.out.println("  " + line.getLabel());
            } else {
                System.out.printf("      %-34s %16s%n", line.getLabel(), ConsoleStyle.peso(line.getAmount()));
            }
        }
        System.out.println("  " + "-".repeat(56));
        System.out.printf("  %-38s %16s%n", "TOTAL", ConsoleStyle.peso(breakdown.getTotal()));
    }

    public static void printBookingSummary(Booking booking) {
        Flight flight = AirlineSystem.getInstance().findFlightOrNull(booking.getFlightNumber());
        System.out.println("  [ Booking " + booking.getPnr() + " - " + booking.getStatus().getLabel() + " ]");
        if (flight != null) {
            System.out.println("  " + flight + "   " + booking.getCabinName());   // uses Flight.toString()
        }
        System.out.println();
        for (Traveler t : booking.getTravelers()) {
            String seat = t.isSeated() ? t.getSeatLabel() : "lap";
            String checked = t.isCheckedIn() ? "checked in" : "not checked in";
            System.out.printf("    %-28s %-7s %-5s %s%n", t.getFullName(), t.getPassengerType().getLabel(), seat, checked);
        }
        System.out.println();
        System.out.println("  Paid: " + ConsoleStyle.peso(booking.getTotalPaid())
                + (booking.isActive() ? "" : "   Refunded: " + ConsoleStyle.peso(booking.getRefundAmount())));
    }

    /** Passenger: their own bookings, newest first. */
    public static void showMyBookings(String username) {
        ConsoleStyle.header("My Bookings");
        AirlineSystem system = AirlineSystem.getInstance();
        List<Booking> mine = system.bookingsBy(username);
        if (mine.isEmpty()) {
            System.out.println("You have no bookings yet.");
            return;
        }
        System.out.printf("%-8s %-8s %-11s %-17s %-10s %s%n", "PNR", "Flight", "Route", "Departure", "Travelers", "Status");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (Booking b : mine) {
            Flight f = system.findFlightOrNull(b.getFlightNumber());
            String route = f == null ? "-" : f.getOrigin().name() + "-" + f.getDestination().name();
            String departure = f == null ? "-" : ConsoleStyle.dateTime(f.getDeparture());
            System.out.printf("%-8s %-8s %-11s %-17s %-10d %s%n", b.getPnr(), b.getFlightNumber(), route, departure,
                    b.getTravelers().size(), b.getStatus().getLabel());
        }
        System.out.println();
        System.out.println("Use Manage Booking with a PNR and last name to see details.");
    }
}
