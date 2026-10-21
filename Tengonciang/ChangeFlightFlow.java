import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Change Flight (rebooking): move a whole booking to another flight of the
 * same airline, on the same route, in the same cabin.
 *
 * Price: a change fee per seated traveler (the cabin's fee from its
 * FareStrategy, made higher or lower by the airline) plus the fare difference if the new flight costs more. If it costs less,
 * the difference isn't refunded, which is how most airlines do it.
 *
 * Not allowed after anyone has checked in, once check-in has closed, or if
 * a traveler's age group or discount would change on the new date (then the
 * booking has to be cancelled and booked again).
 */
public final class ChangeFlightFlow {

    private ChangeFlightFlow() {
    }

    public static void run(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        LocalDateTime now = LocalDateTime.now();
        Flight oldFlight = system.findFlightOrNull(booking.getFlightNumber());

        String blocked = whyNotChangeable(booking, oldFlight, now);
        if (blocked != null) {
            System.out.println(ConsoleStyle.error(blocked));
            return;
        }

        String cabin = booking.getCabinName();
        List<Flight> options = findOptions(booking, oldFlight, now);
        ConsoleStyle.section("Change Flight");
        System.out.println("Now on: " + oldFlight + "   " + cabin);   // Flight.toString()
        if (options.isEmpty()) {
            System.out.println("No other " + oldFlight.getAirline().getName() + " flights on this route have enough "
                    + cabin + " seats for this booking right now.");
            return;
        }
        System.out.println();
        System.out.println("Other " + oldFlight.getAirline().getName() + " flights on this route with " + cabin + " seats:");
        FareStrategy fare = booking.getFareStrategy();
        for (int i = 0; i < options.size(); i++) {
            Flight f = options.get(i);
            System.out.printf("  %d - %s   base fare %s%n", i + 1, f,
                    ConsoleStyle.peso(fare.calculateBaseFare(f.getBaseFare())));
        }
        Flight newFlight = options.get(InputHelper.readChoice("Choice: ", 1, options.size()) - 1);

        String ageProblem = ageProblemOn(booking, newFlight.getDeparture().toLocalDate());
        if (ageProblem != null) {
            System.out.println(ConsoleStyle.error(ageProblem));
            System.out.println("Please cancel this booking and book the new date instead.");
            return;
        }

        Map<Traveler, String> newSeats = pickSeats(booking, newFlight);

        // Price: change fee + fare difference (only if the new fare is higher).
        int seatedCount = booking.getSeatedTravelers().size();
        double feeEach = oldFlight.getAirline().changeFeeFor(fare);   // cabin's fee, adjusted by the airline
        double changeFee = feeEach * seatedCount;
        double oldTicket = booking.getTicketFare() > 0 ? booking.getTicketFare()
                : FareBreakdown.ticketFare(fare, oldFlight, booking.getTravelers());
        double newTicket = FareBreakdown.ticketFare(fare, newFlight, booking.getTravelers());
        double difference = FareBreakdown.round(newTicket - oldTicket);
        double amountDue = FareBreakdown.round(changeFee + Math.max(0, difference));

        printReview(oldFlight, newFlight, newSeats, fare, seatedCount, feeEach, changeFee, difference, amountDue);
        if (!InputHelper.readYesNo("Move booking " + booking.getPnr() + " to " + newFlight.getFlightNumber()
                + "? (Y/N): ")) {
            System.out.println();
            System.out.println("Flight not changed.");
            return;
        }

        PaymentRecord payment = null;
        if (amountDue > 0) {
            payment = PaymentFlow.collectPayment(amountDue, "Flight change");
            if (payment == null) {
                System.out.println("Flight not changed.");
                return;
            }
        }

        if (!moveSeatsAndPets(booking, oldFlight, newFlight, newSeats)) {
            System.out.println(ConsoleStyle.error("The payment was voided. Please try again."));
            return;
        }
        booking.moveToFlight(newFlight.getFlightNumber());
        booking.setTicketFare(Math.max(oldTicket, newTicket));
        if (payment != null) {
            booking.addPayment(payment);
        }
        system.save();
        System.out.println();
        System.out.println("Flight changed! Your PNR stays " + booking.getPnr() + ".");
        ETicket.print(booking);
    }

    // ------------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------------

    /** Returns the reason the booking can't be changed, or null if it can. */
    static String whyNotChangeable(Booking booking, Flight flight, LocalDateTime now) {
        if (!booking.isActive()) {
            return "This booking is cancelled.";
        }
        if (flight == null) {
            return "Flight " + booking.getFlightNumber() + " not found.";
        }
        if (flight.getStatus() == FlightStatus.CANCELLED || flight.hasDeparted(now)) {
            return "This flight is cancelled or has already departed.";
        }
        if (!now.isBefore(flight.getDeparture().minusHours(CheckInFlow.CLOSES_HOURS_BEFORE))) {
            return "Flight changes close " + CheckInFlow.CLOSES_HOURS_BEFORE + " hour before departure.";
        }
        for (Traveler t : booking.getTravelers()) {
            if (t.isCheckedIn()) {
                return "Someone in this booking is already checked in. Flight changes after check-in"
                        + " are done at the airport counter.";
            }
        }
        return null;
    }

    /** Same route, same cabin, still bookable, enough seats and pet spots, and not the current flight. */
    static List<Flight> findOptions(Booking booking, Flight oldFlight, LocalDateTime now) {
        String cabin = booking.getCabinName();
        int seatsNeeded = booking.getSeatedTravelers().size();
        int petsNeeded = 0;
        for (Traveler t : booking.getTravelers()) {
            if (t.getPet() != null) {
                petsNeeded++;
            }
        }
        LocalDateTime latestDeparture = now.plusHours(CheckInFlow.CLOSES_HOURS_BEFORE);
        List<Flight> options = new ArrayList<>();
        for (Flight f : AirlineSystem.getInstance().getFlights()) {
            boolean sameRoute = f.getOrigin() == oldFlight.getOrigin() && f.getDestination() == oldFlight.getDestination();
            // A ticket can only move to another flight of the same airline, like with real airlines.
            boolean sameAirline = f.getAirline() == oldFlight.getAirline();
            if (!sameRoute || !sameAirline || f.getFlightNumber().equals(oldFlight.getFlightNumber())) {
                continue;
            }
            if (!f.isBookable(now) || !f.getDeparture().isAfter(latestDeparture)) {
                continue;
            }
            if (f.getCabinLayout(cabin) == null || f.availableSeatsIn(cabin) < seatsNeeded
                    || f.petSpotsLeft() < petsNeeded) {
                continue;
            }
            options.add(f);
        }
        return options;
    }

    /**
     * Ages are worked out on the departure date, so a new date can change
     * them. Returns what would change, or null if nothing does.
     */
    static String ageProblemOn(Booking booking, LocalDate newDate) {
        int escorts = 0;
        boolean hasChild = false;
        for (Traveler t : booking.getTravelers()) {
            PassengerType typeThen = PassengerType.fromBirthdate(t.getBirthDate(), newDate);
            if (typeThen != t.getPassengerType()) {
                return t.getFullName() + " would count as " + typeThen.getLabel() + " instead of "
                        + t.getPassengerType().getLabel() + " on " + ConsoleStyle.date(newDate) + ".";
            }
            int age = PassengerType.ageOn(t.getBirthDate(), newDate);
            if (t.getDiscount() == FareDiscount.SENIOR && age < PassengerType.SENIOR_MIN_AGE) {
                return t.getFullName() + " wouldn't be 60 yet on " + ConsoleStyle.date(newDate)
                        + ", so the Senior discount wouldn't apply.";
            }
            if (t.isSeated() && age >= PassengerType.ESCORT_MIN_AGE) {
                escorts++;
            }
            if (t.getPassengerType() == PassengerType.CHILD) {
                hasChild = true;
            }
        }
        if (escorts < booking.getInfants().size() || (hasChild && escorts == 0)) {
            return "On " + ConsoleStyle.date(newDate) + " there wouldn't be enough adults (18+) for the"
                    + " children and infants in this booking.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------

    /** New seat for each seated traveler. Nothing is held until the change is paid for. */
    private static Map<Traveler, String> pickSeats(Booking booking, Flight newFlight) {
        String cabin = booking.getCabinName();
        Set<String> heldNow = new HashSet<>();
        Map<Traveler, String> picks = new LinkedHashMap<>();
        System.out.println();
        BookingFlow.printSeatMap(newFlight, cabin, heldNow);
        for (Traveler t : booking.getSeatedTravelers()) {
            String label = InputHelper.read("Seat for " + t.getFullName() + " on " + newFlight.getFlightNumber() + ": ",
                    raw -> BookingFlow.checkSeatChoice(newFlight, cabin, raw, heldNow));
            heldNow.add(label);
            picks.put(t, label);
        }
        return picks;
    }

    private static void printReview(Flight oldFlight, Flight newFlight, Map<Traveler, String> newSeats,
                                    FareStrategy fare, int seatedCount, double feeEach, double changeFee,
                                    double difference, double amountDue) {
        ConsoleStyle.section("Review Flight Change");
        System.out.println(ConsoleStyle.row("From", oldFlight.toString()));
        System.out.println(ConsoleStyle.row("To", newFlight.toString()));
        for (Map.Entry<Traveler, String> pick : newSeats.entrySet()) {
            System.out.println("  " + pick.getKey().getFullName() + ": seat " + pick.getKey().getSeatLabel()
                    + " -> " + pick.getValue());
        }
        System.out.println();
        System.out.println(ConsoleStyle.row("Change fee", changeFee) + "   (" + seatedCount + " x "
                + ConsoleStyle.peso(feeEach) + ", " + oldFlight.getAirline().getName() + " " + fare.getCabinName() + ")");
        if (difference > 0) {
            System.out.println(ConsoleStyle.row("Fare difference", difference));
        } else if (difference < 0) {
            System.out.println(ConsoleStyle.row("Fare difference", 0.0) + "   (the new fare is lower;"
                    + " the difference isn't refunded)");
        } else {
            System.out.println(ConsoleStyle.row("Fare difference", 0.0));
        }
        System.out.println(ConsoleStyle.row("Amount due", amountDue));
        System.out.println();
        System.out.println("Baggage and pets carry over. Check-in opens 24 hours before the new flight.");
    }

    /** Books the new seats, then frees the old ones and moves any pets. Returns false if a seat was taken meanwhile. */
    private static boolean moveSeatsAndPets(Booking booking, Flight oldFlight, Flight newFlight,
                                            Map<Traveler, String> newSeats) {
        String cabin = booking.getCabinName();
        List<String> locked = new ArrayList<>();
        try {
            for (String label : newSeats.values()) {
                newFlight.bookSeat(label, cabin);
                locked.add(label);
            }
        } catch (SeatUnavailableException e) {
            for (String label : locked) {
                newFlight.releaseSeat(label);
            }
            System.out.println(ConsoleStyle.error(e.getMessage()));
            return false;
        }
        for (Map.Entry<Traveler, String> pick : newSeats.entrySet()) {
            Traveler t = pick.getKey();
            oldFlight.releaseSeat(t.getSeatLabel());
            t.setSeatLabel(pick.getValue());
            if (t.getPet() != null) {
                oldFlight.removePet();
                newFlight.addPet();
            }
        }
        return true;
    }
}
