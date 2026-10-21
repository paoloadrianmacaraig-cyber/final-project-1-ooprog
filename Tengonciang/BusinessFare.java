/**
 * Business: 3x the base fare and fully refundable.
 */
public class BusinessFare implements FareStrategy {

    private static final long serialVersionUID = 1L;

    public static final double CHANGE_FEE_PER_TRAVELER = 0.0;

    @Override
    public String getCabinName() {
        return "Business";
    }

    @Override
    public double getFareMultiplier() {
        return 3.0;
    }

    @Override
    public double calculateRefund(double amountPaid, int seatedTravelers) {
        return amountPaid;
    }

    @Override
    public String getRefundRule() {
        return "Business: fully refundable.";
    }

    @Override
    public double getChangeFeePerTraveler() {
        return CHANGE_FEE_PER_TRAVELER;
    }
}
