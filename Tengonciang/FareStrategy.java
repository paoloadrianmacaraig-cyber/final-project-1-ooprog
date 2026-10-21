import java.io.Serializable;

/**
 * Strategy pattern for fares.
 *
 * Every cabin prices its seats and handles refunds its own way, but the
 * booking and cancel flows only talk to this interface. A new cabin is a new
 * class; no if/else chain anywhere has to change.
 */
public interface FareStrategy extends Serializable {

    String getCabinName();

    /** How many times the flight's Economy base fare this cabin costs. */
    double getFareMultiplier();

    /** Refund for a cancelled booking, given what was paid and how many seats it had. */
    double calculateRefund(double amountPaid, int seatedTravelers);

    /** The rule in one line, shown before the user confirms a cancellation. */
    String getRefundRule();

    /** Fee per seated traveler for moving the booking to another flight. */
    double getChangeFeePerTraveler();

    /** This cabin's base fare per seated traveler. */
    default double calculateBaseFare(double economyBaseFare) {
        return FareBreakdown.round(economyBaseFare * getFareMultiplier());
    }

    /** Picks the strategy for a cabin name from the aircraft layout. */
    static FareStrategy forCabin(String cabinName) {
        switch (cabinName) {
            case "Business": return new BusinessFare();
            case "Premium Economy": return new PremiumEconomyFare();
            default: return new EconomyFare();
        }
    }
}
