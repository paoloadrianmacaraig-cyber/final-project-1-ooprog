import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Moving one traveler to another free seat in the same cabin, free of
 * charge. Not allowed after check-in (the boarding pass already shows the
 * seat), on a cancelled booking, or once the flight has departed.
 */
public final class ChangeSeatFlow {

    private ChangeSeatFlow() {
    }

    public static void run(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        Flight flight = system.findFlightOrNull(booking.getFlightNumber());
        if (!booking.isActive()) {
            System.out.println(ConsoleStyle.error("This booking is cancelled."));
            return;
        }
        if (flight == null) {
            System.out.println(ConsoleStyle.error("Flight " + booking.getFlightNumber() + " not found."));
            return;
        }
        if (flight.getStatus() == FlightStatus.CANCELLED || flight.hasDeparted(LocalDateTime.now())) {
            System.out.println(ConsoleStyle.error("Seats can't be changed on a cancelled or departed flight."));
            return;
        }

        ConsoleStyle.section("Change Seat");
        List<Traveler> seated = booking.getSeatedTravelers();
        for (int i = 0; i < seated.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + seated.get(i).getFullName() + " (seat " + seated.get(i).getSeatLabel() + ")");
        }
        Traveler traveler = seated.get(InputHelper.readChoice("Choice: ", 1, seated.size()) - 1);
        if (traveler.isCheckedIn()) {
            System.out.println(ConsoleStyle.error(traveler.getFullName() + " is already checked in. Seat changes after"
                    + " check-in are done at the airport counter."));
            return;
        }

        System.out.println();
        BookingFlow.printSeatMap(flight, booking.getCabinName(), new HashSet<>());
        String oldSeat = traveler.getSeatLabel();
        String newSeat = InputHelper.read("New seat for " + traveler.getFullName() + ": ", raw -> {
            String label = Seat.normalizeLabel(raw);
            if (label.equals(oldSeat)) {
                throw new InvalidInputException("That's already " + traveler.getFullName() + "'s seat.");
            }
            return BookingFlow.checkSeatChoice(flight, booking.getCabinName(), label, new HashSet<>());
        });

        if (!InputHelper.confirm("Move " + traveler.getFullName() + " from " + oldSeat + " to " + newSeat + "? (Y/N): ")) {
            System.out.println("Seat not changed.");
            return;
        }
        try {
            flight.bookSeat(newSeat, booking.getCabinName());
        } catch (SeatUnavailableException e) {
            System.out.println(ConsoleStyle.error(e.getMessage()));
            return;
        }
        flight.releaseSeat(oldSeat);
        traveler.setSeatLabel(newSeat);
        system.save();
        System.out.println("Seat changed to " + newSeat + ".");
    }
}
