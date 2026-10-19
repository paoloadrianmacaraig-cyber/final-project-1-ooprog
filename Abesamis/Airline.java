import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The four airlines that sell flights in our system. Each one has its own
 * flight number prefix, aircraft, baggage rules, change fees, and pet rule,
 * so the same route can feel different depending on who you fly with.
 *
 * An enum works here because the list is fixed: Admin adds flights, not
 * airlines. Each airline is one object with its own values.
 */
public enum Airline {

    //            code  name             kind of airline           free bag            biggest bag         change fee  pets
    MATEO_AIRWAYS("MW", "Mateo Airways", "Premium full-service",   BaggageOption.KG20, BaggageOption.KG32, 0.5,        true),
    AIR_ABESAMIS("AB",  "Air Abesamis",  "Full-service",           BaggageOption.KG20, BaggageOption.KG32, 1.0,        true),
    TENGOFLY("TF",      "TengoFly",      "Low-cost",               BaggageOption.NONE, BaggageOption.KG32, 1.5,        true),
    PAM_AVIATION("PM",  "PAM Aviation",  "Regional island-hopper", BaggageOption.NONE, BaggageOption.KG20, 1.0,        false);

    private final String code;
    private final String name;
    private final String kind;
    private final BaggageOption freeBag;
    private final BaggageOption biggestBag;
    private final double changeFeeRate;
    private final boolean petsAllowed;

    Airline(String code, String name, String kind, BaggageOption freeBag, BaggageOption biggestBag,
            double changeFeeRate, boolean petsAllowed) {
        this.code = code;
        this.name = name;
        this.kind = kind;
        this.freeBag = freeBag;
        this.biggestBag = biggestBag;
        this.changeFeeRate = changeFeeRate;
        this.petsAllowed = petsAllowed;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getKind() { return kind; }
    public BaggageOption getFreeBag() { return freeBag; }
    public boolean allowsPets() { return petsAllowed; }

    /** "Mateo Airways (MW)" */
    public String getLabel() {
        return name + " (" + code + ")";
    }

    /** The planes this airline flies. Admin can only pick from these when adding a flight. */
    public List<AircraftType> getAircraftTypes() {
        switch (this) {
            case MATEO_AIRWAYS: return Arrays.asList(AircraftType.a350());
            case AIR_ABESAMIS:  return Arrays.asList(AircraftType.a321());
            case TENGOFLY:      return Arrays.asList(AircraftType.a321AllEconomy());
            default:            return Arrays.asList(AircraftType.atr72());
        }
    }

    // ------------------------------------------------------------------
    // Baggage
    // ------------------------------------------------------------------

    /** Bag sizes this airline sells, from the free one (or none) up to the biggest it takes. */
    public List<BaggageOption> getBaggageChoices() {
        List<BaggageOption> choices = new ArrayList<>();
        for (BaggageOption option : BaggageOption.values()) {
            if (option.getKilograms() >= freeBag.getKilograms() && option.getKilograms() <= biggestBag.getKilograms()) {
                choices.add(option);
            }
        }
        return choices;
    }

    /** What a bag costs on this airline: the free allowance is taken off the price. */
    public double priceOf(BaggageOption bag) {
        return Math.max(0, bag.getPrice() - freeBag.getPrice());
    }

    public boolean isBiggestBag(BaggageOption bag) {
        return bag.getKilograms() >= biggestBag.getKilograms();
    }

    /** "20 kg checked bag   Included" or "24 kg checked bag   Php 150.00" */
    public String bagMenuLabel(BaggageOption bag) {
        if (bag == BaggageOption.NONE) {
            return "No checked bag";
        }
        double price = priceOf(bag);
        return bag.getKilograms() + " kg checked bag   " + (price == 0 ? "Included" : ConsoleStyle.peso(price));
    }

    /** One line for the booking and search screens, e.g. "20 kg checked bag included, pets allowed". */
    public String getPerksLine() {
        String bag = freeBag == BaggageOption.NONE ? "checked bags are paid" : freeBag.getKilograms() + " kg checked bag included";
        String pets = petsAllowed ? "pets allowed" : "no pets in the cabin";
        return name + ": " + bag + ", " + pets + ".";
    }

    // ------------------------------------------------------------------
    // Change fee
    // ------------------------------------------------------------------

    /** The cabin sets the normal fee; each airline charges more or less than that. */
    public double changeFeeFor(FareStrategy fare) {
        return FareBreakdown.round(fare.getChangeFeePerTraveler() * changeFeeRate);
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    /** The airline a flight number belongs to, from its first two letters. Null if none matches. */
    public static Airline fromFlightNumber(String flightNumber) {
        for (Airline a : values()) {
            if (flightNumber.toUpperCase().startsWith(a.code)) {
                return a;
            }
        }
        return null;
    }
}