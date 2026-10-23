public enum BaggageOption {
    NONE(0, 0.0),
    KG20(20, 600.0),
    KG24(24, 750.0),
    KG28(28, 900.0),
    KG32(32, 1050.0);

    public static final int HAND_CARRY_KG = 7;

    private final int kilograms;
    private final double price;

    BaggageOption(int kilograms, double price) {
        this.kilograms = kilograms;
        this.price = price;
    }

    public int getKilograms() {
        return kilograms;
    }

    public double getPrice() {
        return price;
    }

    public String getShortLabel() {
        return this == NONE ? "-" : kilograms + " kg";
    }
}