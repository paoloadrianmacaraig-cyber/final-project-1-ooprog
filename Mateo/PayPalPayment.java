

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PayPalPayment implements PaymentStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private String email;

    @Override
    public String getMethodName() {
        return "PayPal";
    }

    @Override
    public void collectDetails() {
        email = InputHelper.read("PayPal email: ", PayPalPayment::validateEmail);
    }

    @Override
    public String getMaskedDetails() {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public String processPayment(double amount) throws InvalidPaymentException {
        if (amount <= 0) {
            throw new InvalidPaymentException("Payment amount must be above 0.");
        }
        if (email == null) {
            throw new InvalidPaymentException("PayPal email is missing.");
        }
        return "PP-" + (100000 + RANDOM.nextInt(900000));
    }

    public static String validateEmail(String raw) throws InvalidInputException {
        if (raw.length() > 100 || !EMAIL.matcher(raw).matches()) {
            throw new InvalidInputException("Enter a valid email address.");
        }
        return raw.toLowerCase();
    }
}