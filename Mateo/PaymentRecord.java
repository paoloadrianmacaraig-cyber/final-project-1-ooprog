

import java.io.Serializable;
import java.time.LocalDateTime;

public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String description;
    private final String methodName;
    private final String maskedDetails;
    private final String transactionRef;
    private final double amount;
    private final LocalDateTime paidAt;

    public PaymentRecord(String description, String methodName, String maskedDetails,
                         String transactionRef, double amount, LocalDateTime paidAt) {
        this.description = description;
        this.methodName = methodName;
        this.maskedDetails = maskedDetails;
        this.transactionRef = transactionRef;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getDescription() { return description; }
    public String getMethodName() { return methodName; }
    public String getMaskedDetails() { return maskedDetails; }
    public String getTransactionRef() { return transactionRef; }
    public double getAmount() { return amount; }
    public LocalDateTime getPaidAt() { return paidAt; }
}

