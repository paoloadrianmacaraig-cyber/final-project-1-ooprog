/**
 * Premium Economy: 1.6x the base fare, with a smaller cancellation fee.
 */
public class PremiumEconomyFare implements FareStrategy {

    private static final long serialVersionUID = 1L;

    public static final double CHANGE_FEE_PER_TRAVELER = 500.0;

    public static final double CANCELLATION_FEE_PER_TRAVELER = 500.0;

    @Override
    public String getCabinName() {
        return "Premium Economy";
    }

    @Override
    public double getFareMultiplier() {
        return 1.6;
    }

    @Override
    public double calculateRefund(double amountPaid, int seatedTravelers) {
        return Math.max(0, amountPaid - CANCELLATION_FEE_PER_TRAVELER * seatedTravelers);
    }

    @Override
    public String getRefundRule() {
        return "Premium Economy: refund minus " + ConsoleStyle.peso(CANCELLATION_FEE_PER_TRAVELER)
                + " per seated traveler.";
    }

    @Override
    public double getChangeFeePerTraveler() {
        return CHANGE_FEE_PER_TRAVELER;
    }
}
