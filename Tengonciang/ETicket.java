import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The e-ticket / itinerary receipt. It's built once as a list of lines, so
 * the exact same ticket can be printed on screen or saved as a .txt file.
 */
public final class ETicket {

    private ETicket() {
    }

    public static List<String> lines(Booking booking) {
        Flight flight = AirlineSystem.getInstance().findFlightOrNull(booking.getFlightNumber());
        String line = "-".repeat(ConsoleStyle.WIDTH);
        String thick = "=".repeat(ConsoleStyle.WIDTH);
        List<String> out = new ArrayList<>();

        out.add(thick);
        String issuer = flight == null ? AirlineSystem.AIRLINE_NAME : flight.getAirline().getName();
        String title = "  E-TICKET / ITINERARY RECEIPT";
        out.add(title + " ".repeat(Math.max(2, ConsoleStyle.WIDTH - title.length() - issuer.length() - 2)) + issuer);
        out.add(thick);
        out.add(ConsoleStyle.row("PNR", booking.getPnr() + "   (" + booking.getStatus().getLabel() + ")"));
        if (flight != null) {
            out.add(ConsoleStyle.row("Airline", flight.getAirline().getName()));
            out.add(ConsoleStyle.row("Flight", flight.getFlightNumber() + "   " + flight.getRouteLabel()));
            out.add(ConsoleStyle.row("Departure", ConsoleStyle.dateTime(flight.getDeparture())
                    + "   arrives " + ConsoleStyle.time(flight.getArrival())));
            out.add(ConsoleStyle.row("Aircraft", flight.getAircraftType().getName()
                    + "   Cabin: " + booking.getCabinName()));
        }
        out.add(line);
        out.add(String.format("  %-28s %-8s %-6s %-9s %s", "Traveler", "Type", "Seat", "Baggage", "Extras"));
        for (Traveler t : booking.getTravelers()) {
            String seat = t.isSeated() ? t.getSeatLabel() : "lap";
            String bag = t.isSeated() ? t.getBaggage().getShortLabel() : "-";
            String extras = t.getPet() != null ? "pet" : "";
            if (t.getDiscount() != FareDiscount.NONE) {
                extras = (extras.isEmpty() ? "" : extras + ", ") + t.getDiscount().getLabel();
            }
            out.add(String.format("  %-28s %-8s %-6s %-9s %s", t.getFullName(), t.getPassengerType().getLabel(),
                    seat, bag, extras));
        }
        out.add(line);
        for (PaymentRecord p : booking.getPayments()) {
            out.add(String.format("  %-18s %14s  %s %s  Ref %s", p.getDescription(), ConsoleStyle.peso(p.getAmount()),
                    p.getMethodName(), p.getMaskedDetails(), p.getTransactionRef()));
        }
        out.add(ConsoleStyle.row("Total paid", booking.getTotalPaid()));
        if (!booking.isActive()) {
            out.add(ConsoleStyle.row("Refunded", booking.getRefundAmount()));
        }
        out.add("  Hand-carry: " + BaggageOption.HAND_CARRY_KG + " kg per seated traveler."
                + " Check-in opens 24 hours before departure.");
        out.add(thick);
        return out;
    }

    public static void print(Booking booking) {
        System.out.println();
        for (String line : lines(booking)) {
            System.out.println(line);
        }
    }

    /** Writes the e-ticket to e-ticket-PNR.txt in the program's folder. */
    public static void save(Booking booking) {
        Path file = Paths.get("e-ticket-" + booking.getPnr() + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String line : lines(booking)) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("E-ticket saved as " + file.getFileName() + " in the program's folder.");
        } catch (IOException e) {
            System.out.println(ConsoleStyle.error("The e-ticket couldn't be saved (" + e.getMessage() + ")."));
        }
    }
}
