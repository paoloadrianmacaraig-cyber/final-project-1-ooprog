import java.io.Serializable;

/**
 * A small dog flying in the cabin with its owner, modelled on PAL's in-cabin
 * pet rules: up to 10 kg in a carrier under the seat, a flat fee one way,
 * one per passenger, three per flight.
 */
public class PetAddOn implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final double FEE = 2500.0;
    public static final double MAX_WEIGHT_KG = 10.0;

    private final double weightKg;

    public PetAddOn(double weightKg) {
        this.weightKg = weightKg;
    }

    public double getWeightKg() { return weightKg; }
    public double getFee() { return FEE; }

    public static double validateWeight(String raw) throws InvalidInputException {
        double kg = InputHelper.parseAmount(raw, 1000, "Pet weight");
        if (kg > MAX_WEIGHT_KG) {
            throw new InvalidInputException("Only dogs up to 10 kg can fly in the cabin.");
        }
        return kg;
    }
}