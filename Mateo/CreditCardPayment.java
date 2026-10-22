

import java.security.SecureRandom;
import java.time.YearMonth;

public class CreditCardPayment implements PaymentStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();

    private String cardNumber;
    private YearMonth expiry;
    private String cvv;

    @Override
    public String getMethodName() {
        return "Credit/Debit Card";
    }

    @Override
    public void collectDetails() {
        cardNumber = InputHelper.read("Card number (16 digits): ", CreditCardPayment::validateCardNumber);
        expiry = InputHelper.read("Expiry (MM/YY): ", raw -> validateExpiry(raw, YearMonth.now()));
        cvv = InputHelper.read("CVV (3 digits): ", CreditCardPayment::validateCvv);
    }

    @Override
    public String getMaskedDetails() {
        return cardNumber == null ? "" : "**** " + cardNumber.substring(12);
    }

    @Override
    public String processPayment(double amount) throws InvalidPaymentException {
        if (amount <= 0) {
            throw new InvalidPaymentException("Payment amount must be above 0.");
        }
        if (cardNumber == null || expiry == null || cvv == null) {
            throw new InvalidPaymentException("Card details are missing.");
        }
        cvv = null; 
        return "CC-" + (100000 + RANDOM.nextInt(900000));
    }

    public static String validateCardNumber(String raw) throws InvalidInputException {
        String digits = raw.replaceAll("[ -]", "");
        if (!digits.matches("\\d{16}")) {
            throw new InvalidInputException("Card number must be 16 digits.");
        }
        if (!passesLuhn(digits)) {
            throw new InvalidInputException("That card number isn't valid.");
        }
        return digits;
    }

    public static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public static YearMonth validateExpiry(String raw, YearMonth now) throws InvalidInputException {
        if (!raw.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            throw new InvalidInputException("Use MM/YY, e.g. 08/28.");
        }
        int month = Integer.parseInt(raw.substring(0, 2));
        int year = 2000 + Integer.parseInt(raw.substring(3));
        YearMonth value = YearMonth.of(year, month);
        if (value.isBefore(now)) {
            throw new InvalidInputException("This card has expired.");
        }
        if (value.isAfter(now.plusYears(20))) {
            throw new InvalidInputException("Check the expiry year.");
        }
        return value;
    }

    public static String validateCvv(String raw) throws InvalidInputException {
        if (!raw.matches("^\\d{3}$")) {
            throw new InvalidInputException("CVV must be 3 digits.");
        }
        return raw;
    }
}
