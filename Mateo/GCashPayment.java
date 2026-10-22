package Mateo;

import java.security.SecureRandom;

public class GCashPayment implements PaymentStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();

    private String mobileNumber;

    @Override
    public String getMethodName() {
        return "GCash";
    }

    @Override
    public void collectDetails() {
        mobileNumber = InputHelper.read("GCash number (09XXXXXXXXX): ", GCashPayment::validateNumber);
    }

    @Override
    public String getMaskedDetails() {
        if (mobileNumber == null) {
            return "";
        }
        return mobileNumber.substring(0, 4) + " *** " + mobileNumber.substring(7);
    }

    @Override
    public String processPayment(double amount) throws InvalidPaymentException {
        if (amount <= 0) {
            throw new InvalidPaymentException("Payment amount must be above 0.");
        }
        if (mobileNumber == null) {
            throw new InvalidPaymentException("GCash number is missing.");
        }
        return "GC-" + (100000 + RANDOM.nextInt(900000));
    }

    public static String validateNumber(String raw) throws InvalidInputException {
        String digits = raw.replaceAll("[ -]", "");
        if (!digits.matches("^09\\d{9}$")) {
            throw new InvalidInputException("GCash numbers start with 09 and have 11 digits.");
        }
        return digits;
    }
}
