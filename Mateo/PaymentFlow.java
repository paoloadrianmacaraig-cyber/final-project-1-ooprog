

import java.time.LocalDateTime;

public final class PaymentFlow {

    private PaymentFlow() {
    }
    public static PaymentRecord collectPayment(double amount, String description) {
        ConsoleStyle.section("Payment");
        System.out.println("Amount due: " + ConsoleStyle.peso(amount));
        System.out.println();
        System.out.println("  1 - Credit/Debit Card");
        System.out.println("  2 - GCash");
        System.out.println("  3 - PayPal");
        PaymentStrategy strategy;
        switch (InputHelper.readChoice("Choice: ", 1, 3)) {
            case 1: strategy = new CreditCardPayment(); break;
            case 2: strategy = new GCashPayment(); break;
            default: strategy = new PayPalPayment(); break;
        }

        strategy.collectDetails();
        if (!InputHelper.confirm("Pay " + ConsoleStyle.peso(amount) + " with "
                + strategy.getMethodName() + " (" + strategy.getMaskedDetails() + ")? (Y/N): ")) {
            System.out.println("Payment not made.");
            return null;
        }

        try {
            String reference = strategy.processPayment(amount);
            System.out.println("Payment approved. Reference: " + reference);
            return new PaymentRecord(description, strategy.getMethodName(), strategy.getMaskedDetails(),
                    reference, amount, LocalDateTime.now());
        } catch (InvalidPaymentException e) {
            System.out.println(ConsoleStyle.error("Payment failed: " + e.getMessage()));
            return null;
        }
    }
}
