

import java.time.LocalDateTime;

public final class CancelFlow {

    private CancelFlow() {
    }

    public static void run() {
        ConsoleStyle.header("Cancel Booking");
        Booking booking = ManageBookingFlow.findBooking();
        cancel(booking);
    }

    public static void cancel(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        if (!booking.isActive()) {
            System.out.println(ConsoleStyle.error("This booking is already cancelled."));
            return;
        }
        Flight flight;
        try {
            flight = system.findFlight(booking.getFlightNumber());
        } catch (FlightNotFoundException e) {
            System.out.println(ConsoleStyle.error(e.getMessage()));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (flight.hasDeparted(now)) {
            System.out.println(ConsoleStyle.error("This flight has already departed and can't be cancelled."));
            return;
        }

        double paid = booking.getTotalPaid();
        double refund = booking.getFareStrategy().calculateRefund(paid, booking.getSeatedTravelers().size());

        ConsoleStyle.section("Refund");
        System.out.println(ConsoleStyle.row("Total paid", paid));
        System.out.println(ConsoleStyle.row("Cancellation fee", paid - refund));
        System.out.println(ConsoleStyle.row("Refund", refund));
        System.out.println(booking.getFareStrategy().getRefundRule());
        if (!InputHelper.confirm("Cancel booking " + booking.getPnr() + "? (Y/N): ")) {
            System.out.println("Booking not cancelled.");
            return;
        }

        for (Traveler t : booking.getSeatedTravelers()) {
            flight.releaseSeat(t.getSeatLabel());
            if (t.getPet() != null) {
                flight.removePet();
            }
        }
        booking.cancel(refund, now);
        system.save();
        System.out.println("Booking " + booking.getPnr() + " cancelled. " + ConsoleStyle.peso(refund)
                + " will be refunded to the original payment method.");
    }
}
