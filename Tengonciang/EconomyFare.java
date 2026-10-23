/**
 * Economy: the flight's base fare. Cancelling costs a fee per seated traveler.
 */
public class EconomyFare implements FareStrategy {

    private static final long serialVersionUID = 1L;

    public static final double CHANGE_FEE_PER_TRAVELER = 1000.0;

    public static final double CANCELLATION_FEE_PER_TRAVELER = 1000.0;

    @Override
    public String getCabinName() {
        return "Economy";
    }

    @Override
    public double getFareMultiplier() {
        return 1.0;
    }

    @Override
    public double calculateRefund(double amountPaid, int seatedTravelers) {
        return Math.max(0, amountPaid - CANCELLATION_FEE_PER_TRAVELER * seatedTravelers);
    }

    @Override
    public String getRefundRule() {
        return "Economy: refund minus " + ConsoleStyle.peso(CANCELLATION_FEE_PER_TRAVELER) + " per seated traveler.";
    }

    @Override
    public double getChangeFeePerTraveler() {
        return CHANGE_FEE_PER_TRAVELER;
    }
}
