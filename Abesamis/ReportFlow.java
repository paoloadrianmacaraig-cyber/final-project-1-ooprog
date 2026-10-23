import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Admin reports: every booking, the passenger list (manifest) of one
 * flight, and revenue per flight after refunds.
 */
public final class ReportFlow {

    private ReportFlow() {
    }

    public static void allBookings() {
        ConsoleStyle.header("All Bookings");
        List<Booking> bookings = AirlineSystem.getInstance().getBookings();
        if (bookings.isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        System.out.printf("%-8s %-7s %-14s %-10s %16s  %s%n", "PNR", "Flight", "Booked by", "Travelers", "Paid", "Status");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (Booking b : bookings) {
            System.out.printf("%-8s %-7s %-14s %-10d %16s  %s%n", b.getPnr(), b.getFlightNumber(), b.getBookedBy(),
                    b.getTravelers().size(), ConsoleStyle.peso(b.getTotalPaid()), b.getStatus().getLabel());
        }
        System.out.println("Total bookings: " + bookings.size());
    }

    public static void passengerList() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Passenger List");
        ConsoleStyle.cancelHint();
        Flight flight = InputHelper.read("Flight number: ", raw -> {
            try {
                return system.findFlight(raw.replace(" ", "").toUpperCase());
            } catch (FlightNotFoundException e) {
                throw new InvalidInputException(e.getMessage());
            }
        });

        List<String[]> rows = new ArrayList<>();
        for (Booking b : system.activeBookingsOn(flight.getFlightNumber())) {
            for (Traveler t : b.getTravelers()) {
                String seat = t.isSeated() ? t.getSeatLabel() : "lap";
                String bag = t.isSeated() ? t.getBaggage().getShortLabel() : "-";
                rows.add(new String[] {seat, t.getFullName(), t.getPassengerType().getLabel(),
                        t.getDiscount() == FareDiscount.NONE ? "" : t.getDiscount().getLabel(), bag,
                        t.isCheckedIn() ? "Yes" : "No", b.getPnr()});
            }
        }
        System.out.println();
        System.out.println(flight);   // uses Flight.toString()
        if (rows.isEmpty()) {
            System.out.println("No passengers booked on this flight.");
            return;
        }
        rows.sort(Comparator.comparing((String[] r) -> r[0]));
        System.out.printf("%-5s %-26s %-7s %-15s %-8s %-8s %s%n", "Seat", "Name", "Type", "Discount", "Baggage", "Checked", "PNR");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (String[] r : rows) {
            System.out.printf("%-5s %-26s %-7s %-15s %-8s %-8s %s%n", r[0], r[1], r[2], r[3], r[4], r[5], r[6]);
        }
        System.out.println("Passengers: " + rows.size() + "   Pets in cabin: " + flight.getPetCount());
    }

    public static void revenueReport() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Revenue Report");
        List<Flight> flights = system.getFlights();
        if (flights.isEmpty()) {
            System.out.println("No flights yet.");
            return;
        }
        System.out.printf("%-7s %-9s %-17s %-10s %-9s %16s%n", "Flight", "Route", "Departure", "Seats", "Bookings", "Net revenue");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        double total = 0;
        double refunds = 0;
        Map<Airline, Double> byAirline = new EnumMap<>(Airline.class);
        for (Flight f : flights) {
            double net = 0;
            int active = 0;
            for (Booking b : system.bookingsOnFlight(f.getFlightNumber())) {
                net += b.getTotalPaid() - b.getRefundAmount();
                refunds += b.getRefundAmount();
                if (b.isActive()) {
                    active++;
                }
            }
            total += net;
            byAirline.merge(f.getAirline(), net, Double::sum);
            String seats = (f.totalSeats() - f.availableSeats()) + "/" + f.totalSeats();
            System.out.printf("%-7s %-9s %-17s %-10s %-9d %16s%n", f.getFlightNumber(),
                    f.getOrigin().name() + "-" + f.getDestination().name(), ConsoleStyle.dateTime(f.getDeparture()),
                    seats, active, ConsoleStyle.peso(net));
        }
        // Money kept from bookings on flights that were later removed still counts.
        double removedNet = 0;
        for (Booking b : system.getBookings()) {
            if (system.findFlightOrNull(b.getFlightNumber()) == null) {
                removedNet += b.getTotalPaid() - b.getRefundAmount();
                refunds += b.getRefundAmount();
            }
        }
        if (removedNet != 0) {
            System.out.printf("%-56s %16s%n", "Removed flights", ConsoleStyle.peso(removedNet));
            total += removedNet;
        }
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (Airline a : Airline.values()) {
            System.out.println(ConsoleStyle.row(a.getName(), FareBreakdown.round(byAirline.getOrDefault(a, 0.0))));
        }
        System.out.println();
        System.out.println(ConsoleStyle.row("Refunds paid out", FareBreakdown.round(refunds)));
        System.out.println(ConsoleStyle.row("Net revenue", FareBreakdown.round(total)));
    }
}