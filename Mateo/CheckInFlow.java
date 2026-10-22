package Mateo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CheckInFlow {

    public static final int OPENS_HOURS_BEFORE = 24;
    public static final int CLOSES_HOURS_BEFORE = 1;
    public static final int BOARDING_MINUTES_BEFORE = 40;

    private CheckInFlow() {
    }

    public static void run() {
        ConsoleStyle.header("Check In");
        Booking booking = ManageBookingFlow.findBooking();
        checkIn(booking);
    }

    public static void checkIn(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        if (!booking.isActive()) {
            System.out.println(ConsoleStyle.error("This booking is cancelled."));
            return;
        }
        Flight flight;
        try {
            flight = system.findFlight(booking.getFlightNumber());
            checkAllowed(booking, flight, LocalDateTime.now());
        } catch (FlightNotFoundException | CheckInNotAllowedException e) {
            System.out.println(ConsoleStyle.error(e.getMessage()));
            return;
        }

        List<Traveler> pending = new ArrayList<>();
        for (Traveler t : booking.getSeatedTravelers()) {
            if (!t.isCheckedIn()) {
                pending.add(t);
            }
        }
        if (pending.isEmpty()) {
            System.out.println();
            System.out.println("Everyone in this booking is already checked in.");
            if (InputHelper.readYesNo("Print the boarding passes again? (Y/N): ")) {
                printBoardingPasses(booking, flight, booking.getSeatedTravelers());
            }
            return;
        }

        ConsoleStyle.section("Check In");
        for (int i = 0; i < pending.size(); i++) {
            Traveler t = pending.get(i);
            System.out.println("  " + (i + 1) + " - " + t.getFullName() + " (seat " + t.getSeatLabel() + ")");
        }
        int everyone = pending.size() + 1;
        System.out.println("  " + everyone + " - Everyone above");
        int choice = InputHelper.readChoice("Choice: ", 1, everyone);
        List<Traveler> chosen = new ArrayList<>();
        if (choice == everyone) {
            chosen.addAll(pending);
        } else {
            chosen.add(pending.get(choice - 1));
        }

        if (!InputHelper.confirm("Check in " + chosen.size() + " traveler(s)? (Y/N): ")) {
            System.out.println("Check-in not done.");
            return;
        }
        for (Traveler t : chosen) {
            t.checkIn();
            Traveler infant = booking.getInfantWith(t, flight.getDeparture().toLocalDate());
            if (infant != null) {
                infant.checkIn();
            }
        }
        system.save();
        System.out.println("Check-in complete.");
        printBoardingPasses(booking, flight, chosen);
    }

    /** Throws with a reason if check-in isn't possible at this moment. */
    public static void checkAllowed(Booking booking, Flight flight, LocalDateTime now) throws CheckInNotAllowedException {
        if (!booking.isActive()) {
            throw new CheckInNotAllowedException("This booking is cancelled.");
        }
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new CheckInNotAllowedException("This flight is cancelled.");
        }
        LocalDateTime opens = flight.getDeparture().minusHours(OPENS_HOURS_BEFORE);
        LocalDateTime closes = flight.getDeparture().minusHours(CLOSES_HOURS_BEFORE);
        if (now.isBefore(opens)) {
            throw new CheckInNotAllowedException("Check-in opens 24 hours before departure, at "
                    + ConsoleStyle.dateTime(opens) + ".");
        }
        if (!now.isBefore(closes)) {
            throw new CheckInNotAllowedException("Check-in closed 1 hour before departure.");
        }
    }

    public static int boardingZone(Flight flight, String cabinName, String seatLabel) {
        if (!cabinName.equals("Economy")) {
            return 1;
        }
        CabinLayout layout = flight.getCabinLayout(cabinName);
        int row = flight.getSeat(seatLabel).getRow();
        int middle = (layout.getFirstRow() + layout.getLastRow()) / 2;
        return row > middle ? 2 : 3;
    }

    private static void printBoardingPasses(Booking booking, Flight flight, List<Traveler> travelers) {
        for (Traveler t : travelers) {
            Traveler infant = booking.getInfantWith(t, flight.getDeparture().toLocalDate());
            System.out.println();
            ConsoleStyle.divider();
            String title = "  BOARDING PASS";
            String airline = flight.getAirline().getName().toUpperCase();
            System.out.println(title + " ".repeat(ConsoleStyle.WIDTH - title.length() - airline.length() - 2) + airline);
            ConsoleStyle.divider();
            System.out.println(ConsoleStyle.row("Passenger", t.getFullName().toUpperCase()));
            if (infant != null) {
                System.out.println(ConsoleStyle.row("With infant", infant.getFullName().toUpperCase()));
            }
            System.out.println(ConsoleStyle.row("Flight", flight.getFlightNumber() + "   " + flight.getRouteLabel()));
            System.out.println(ConsoleStyle.row("Date", ConsoleStyle.date(flight.getDeparture().toLocalDate())));
            System.out.println(ConsoleStyle.row("Departs", ConsoleStyle.time(flight.getDeparture())
                    + "   Boarding " + ConsoleStyle.time(flight.getDeparture().minusMinutes(BOARDING_MINUTES_BEFORE))));
            System.out.println(ConsoleStyle.row("Gate", flight.getGate()));
            System.out.println(ConsoleStyle.row("Seat", t.getSeatLabel() + "   " + booking.getCabinName()
                    + "   Zone " + boardingZone(flight, booking.getCabinName(), t.getSeatLabel())));
            System.out.println(ConsoleStyle.row("PNR", booking.getPnr()));
            ConsoleStyle.divider();
            System.out.println("  Gate closes 15 minutes before departure.");
        }
    }
