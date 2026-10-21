import java.io.Serializable;
import java.time.LocalDate;

/**
 * One person in a booking. Seated travelers get a seat, baggage, and maybe a
 * pet; an infant (under 2) flies on an adult's lap with none of those.
 */
public class Traveler implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fullName;
    private final LocalDate birthDate;
    private final PassengerType passengerType;
    private final FareDiscount discount;
    private final String discountId;
    private String seatLabel;
    private BaggageOption baggage = BaggageOption.NONE;
    private PetAddOn pet;
    private boolean checkedIn;

    /** A traveler with no Senior/PWD discount: most travelers, and every infant. */
    public Traveler(String fullName, LocalDate birthDate, PassengerType passengerType) {
        this(fullName, birthDate, passengerType, FareDiscount.NONE, null);
    }

    /** A traveler with a discount and the ID number that proves it. */
    public Traveler(String fullName, LocalDate birthDate, PassengerType passengerType,
                    FareDiscount discount, String discountId) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.passengerType = passengerType;
        this.discount = discount;
        this.discountId = discountId;
    }

    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public PassengerType getPassengerType() { return passengerType; }
    public FareDiscount getDiscount() { return discount; }
    public String getDiscountId() { return discountId; }
    public String getSeatLabel() { return seatLabel; }
    public BaggageOption getBaggage() { return baggage; }
    public PetAddOn getPet() { return pet; }
    public boolean isCheckedIn() { return checkedIn; }

    public boolean isSeated() {
        return passengerType != PassengerType.INFANT;
    }

    public void setSeatLabel(String seatLabel) {
        if (!isSeated()) {
            throw new IllegalStateException("Infants don't get a seat.");
        }
        this.seatLabel = seatLabel;
    }

    public void setBaggage(BaggageOption baggage) {
        if (!isSeated() && baggage != BaggageOption.NONE) {
            throw new IllegalStateException("Infants have no checked baggage allowance.");
        }
        this.baggage = baggage;
    }

    public void setPet(PetAddOn pet) {
        this.pet = pet;
    }

    public void checkIn() {
        checkedIn = true;
    }

    /** True if the full name ends with the given last name ("Cruz" or "Dela Cruz"), ignoring case. */
    public boolean hasLastName(String lastName) {
        String name = fullName.toLowerCase();
        String last = lastName.trim().replaceAll("\\s+", " ").toLowerCase();
        return !last.isEmpty() && (name.equals(last) || name.endsWith(" " + last));
    }
}
