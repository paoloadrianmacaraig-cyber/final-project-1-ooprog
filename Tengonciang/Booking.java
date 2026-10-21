import java.io.Serializable;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * One reservation under one PNR: a flight, a cabin, 1-4 seated travelers
 * plus any infants, and every payment made for it.
 */
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    // No I, O, 0 or 1, so a PNR read aloud or handwritten can't be misread.
    private static final String PNR_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PNR_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String pnr;
    private String flightNumber;
    private final String bookedBy;
    private final FareStrategy fareStrategy;
    private final List<Traveler> travelers;
    private final List<PaymentRecord> payments = new ArrayList<>();
    private final LocalDateTime createdAt;
    private BookingStatus status = BookingStatus.CONFIRMED;
    private double refundAmount;
    private LocalDateTime cancelledAt;
    private double ticketFare;   // fare part of what was paid (no baggage or pets), used for flight changes

    public Booking(String pnr, String flightNumber, String bookedBy, FareStrategy fareStrategy,
                   List<Traveler> travelers, LocalDateTime createdAt) {
        this.pnr = pnr;
        this.flightNumber = flightNumber;
        this.bookedBy = bookedBy;
        this.fareStrategy = fareStrategy;
        this.travelers = new ArrayList<>(travelers);
        this.createdAt = createdAt;
    }

    public String getPnr() { return pnr; }
    public String getFlightNumber() { return flightNumber; }
    public String getBookedBy() { return bookedBy; }
    public FareStrategy getFareStrategy() { return fareStrategy; }
    public String getCabinName() { return fareStrategy.getCabinName(); }
    public List<Traveler> getTravelers() { return Collections.unmodifiableList(travelers); }
    public List<PaymentRecord> getPayments() { return Collections.unmodifiableList(payments); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BookingStatus getStatus() { return status; }
    public double getRefundAmount() { return refundAmount; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public double getTicketFare() { return ticketFare; }

    public void setTicketFare(double ticketFare) {
        this.ticketFare = ticketFare;
    }

    /** Moves this booking to another flight (Change Flight). Seats are handled by the flow. */
    public void moveToFlight(String newFlightNumber) {
        if (!isActive()) {
            throw new IllegalStateException("Booking " + pnr + " is cancelled.");
        }
        this.flightNumber = newFlightNumber;
    }

    public boolean isActive() {
        return status == BookingStatus.CONFIRMED;
    }

    public List<Traveler> getSeatedTravelers() {
        List<Traveler> seated = new ArrayList<>();
        for (Traveler t : travelers) {
            if (t.isSeated()) {
                seated.add(t);
            }
        }
        return seated;
    }

    public List<Traveler> getInfants() {
        List<Traveler> infants = new ArrayList<>();
        for (Traveler t : travelers) {
            if (!t.isSeated()) {
                infants.add(t);
            }
        }
        return infants;
    }

    /** The first seated traveler: the name used with the PNR in Manage Booking. */
    public Traveler getLeadTraveler() {
        return getSeatedTravelers().get(0);
    }

    /**
     * The infant flying on this adult's lap, if any. Infants are paired with
     * the travelers aged 18+ in the order they were entered.
     */
    public Traveler getInfantWith(Traveler adult, LocalDate travelDate) {
        List<Traveler> escorts = new ArrayList<>();
        for (Traveler t : getSeatedTravelers()) {
            if (PassengerType.ageOn(t.getBirthDate(), travelDate) >= PassengerType.ESCORT_MIN_AGE) {
                escorts.add(t);
            }
        }
        int index = escorts.indexOf(adult);
        List<Traveler> infants = getInfants();
        return (index >= 0 && index < infants.size()) ? infants.get(index) : null;
    }

    public double getTotalPaid() {
        double sum = 0;
        for (PaymentRecord p : payments) {
            sum += p.getAmount();
        }
        return FareBreakdown.round(sum);
    }

    public void addPayment(PaymentRecord payment) {
        payments.add(payment);
    }

    public boolean matchesLastName(String lastName) {
        return getLeadTraveler().hasLastName(lastName);
    }

    public void cancel(double refund, LocalDateTime when) {
        if (!isActive()) {
            throw new IllegalStateException("Booking " + pnr + " is already cancelled.");
        }
        status = BookingStatus.CANCELLED;
        refundAmount = refund;
        cancelledAt = when;
    }

    // ------------------------------------------------------------------
    // PNR
    // ------------------------------------------------------------------

    /** A new random PNR that the given check says isn't taken yet. */
    public static String generatePnr(Predicate<String> isTaken) {
        String pnr;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < PNR_LENGTH; i++) {
                sb.append(PNR_CHARACTERS.charAt(RANDOM.nextInt(PNR_CHARACTERS.length())));
            }
            pnr = sb.toString();
        } while (isTaken.test(pnr));
        return pnr;
    }

    public static String validatePnr(String raw) throws InvalidInputException {
        String pnr = raw.toUpperCase();
        if (!pnr.matches("^[A-Z0-9]{6}$")) {
            throw new InvalidInputException("PNR must be 6 letters or numbers.");
        }
        return pnr;
    }
}
