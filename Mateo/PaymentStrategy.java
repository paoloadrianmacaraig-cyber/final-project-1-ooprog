package Mateo;

public interface PaymentStrategy {

    String getMethodName();

    void collectDetails();

    String getMaskedDetails();

    String processPayment(double amount) throws InvalidPaymentException;
}
