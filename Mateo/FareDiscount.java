

public enum FareDiscount {
    NONE("None", 0.0, false),
    SENIOR("Senior Citizen", 0.20, true),
    PWD("PWD", 0.20, true);

    private final String label;
    private final double rate;
    private final boolean vatExempt;

    FareDiscount(String label, double rate, boolean vatExempt) {
        this.label = label;
        this.rate = rate;
        this.vatExempt = vatExempt;
    }

    public String getLabel() { return label; }
    public double getRate() { return rate; }
    public boolean isVatExempt() { return vatExempt; }

    public static String validateId(String raw) throws InvalidInputException {
        if (!raw.matches("^[A-Za-z0-9-]{4,20}$")) {
            throw new InvalidInputException("Enter a valid ID number (4-20 letters, digits, or -).");
        }
        return raw.toUpperCase();
    }
}