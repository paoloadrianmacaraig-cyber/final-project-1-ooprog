import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Works out every peso of a booking, line by line, so the review screen and
 * the e-ticket can show exactly what the customer pays for.
 *
 * Per seated traveler: base fare, minus Senior/PWD discount, plus 12% VAT
 * (unless VAT-exempt), plus fuel surcharge and terminal fee, plus any
 * checked baggage and pet. Each infant on a lap pays a flat infant fee.
 */
public class FareBreakdown {

    public static final double VAT_RATE = 0.12;
    public static final double FUEL_SURCHARGE = 300.0;
    public static final double TERMINAL_FEE = 200.0;
    public static final double INFANT_FEE = 500.0;

    /** One printed line. A null amount means a heading line (a traveler's name). */
    public static class Line {
        private final String label;
        private final Double amount;

        Line(String label, Double amount) {
            this.label = label;
            this.amount = amount;
        }

        public String getLabel() { return label; }
        public Double getAmount() { return amount; }
        public boolean isHeading() { return amount == null; }
    }

    private final List<Line> lines = new ArrayList<>();
    private double total;

    private FareBreakdown() {
    }

    public static FareBreakdown forBooking(FareStrategy fare, Flight flight, List<Traveler> travelers) {
        FareBreakdown b = new FareBreakdown();
        double base = fare.calculateBaseFare(flight.getBaseFare());
        Airline airline = flight.getAirline();

        for (Traveler t : travelers) {
            if (!t.isSeated()) {
                b.heading(t.getFullName() + " (Infant on lap)");
                b.add("Infant fee", INFANT_FEE);
                continue;
            }
            b.heading(t.getFullName() + " (" + t.getPassengerType().getLabel() + ")");
            b.add("Base fare, " + fare.getCabinName(), base);

            double discount = round(base * t.getDiscount().getRate());
            if (discount > 0) {
                b.add(t.getDiscount().getLabel() + " discount (20%)", -discount);
            }
            if (t.getDiscount().isVatExempt()) {
                b.add("VAT (exempt)", 0.0);
            } else {
                b.add("VAT (12%)", round((base - discount) * VAT_RATE));
            }
            b.add("Fuel surcharge", FUEL_SURCHARGE);
            b.add("Terminal fee", TERMINAL_FEE);
            if (t.getBaggage() != BaggageOption.NONE) {
                double bagPrice = airline.priceOf(t.getBaggage());
                String included = bagPrice == 0 ? " (included)" : "";
                b.add("Checked baggage " + t.getBaggage().getKilograms() + " kg" + included, bagPrice);
            }
            if (t.getPet() != null) {
                b.add("Pet in cabin", t.getPet().getFee());
            }
        }
        b.total = round(b.total);
        return b;
    }

    /** Only the ticket part of a booking's price: the total without checked baggage and pets. */
    public static double ticketFare(FareStrategy fare, Flight flight, List<Traveler> travelers) {
        double total = forBooking(fare, flight, travelers).getTotal();
        for (Traveler t : travelers) {
            if (t.isSeated()) {
                total -= flight.getAirline().priceOf(t.getBaggage());
            }
            if (t.getPet() != null) {
                total -= t.getPet().getFee();
            }
        }
        return round(total);
    }

    private void heading(String label) {
        lines.add(new Line(label, null));
    }

    private void add(String label, double amount) {
        lines.add(new Line(label, amount));
        total += amount;
    }

    public List<Line> getLines() { return Collections.unmodifiableList(lines); }
    public double getTotal() { return total; }

    /** Rounds to centavos, so totals never show 0.30000000000000004. */
    public static double round(double amount) {
        return Math.round(amount * 100) / 100.0;
    }
}
