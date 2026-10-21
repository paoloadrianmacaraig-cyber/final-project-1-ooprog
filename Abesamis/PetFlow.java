import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pets in the cabin (stretch goal): offered during booking, or added later
 * from Manage Booking with a separate payment. Only a traveler aged 18+ can
 * bring one, one pet each, and the flight takes at most 3.
 */
public final class PetFlow {

    private PetFlow() {
    }

    /** During booking: the fee is added to the booking's total. */
    public static void offer(Flight flight, List<Traveler> travelers) {
        LocalDate travelDate = flight.getDeparture().toLocalDate();
        int spots = flight.petSpotsLeft();
        List<Traveler> owners = eligibleOwners(travelers, travelDate);
        if (owners.isEmpty()) {
            return;
        }
        ConsoleStyle.section("Pet in the Cabin");
        if (!flight.getAirline().allowsPets()) {
            System.out.println(flight.getAirline().getName() + " doesn't carry pets in the cabin.");
            return;
        }
        if (spots <= 0) {
            System.out.println("Pets in the cabin are fully booked on this flight (" + Flight.MAX_PETS + " per flight).");
            return;
        }
        System.out.println("A small dog (up to 10 kg) can ride in the cabin for " + ConsoleStyle.peso(PetAddOn.FEE) + ".");
        while (spots > 0 && !owners.isEmpty()
                && InputHelper.readYesNo("Bring a small dog in the cabin? (Y/N): ")) {
            Traveler owner = pickOwner(owners);
            double weight = InputHelper.read("Dog's weight in kg: ", PetAddOn::validateWeight);
            owner.setPet(new PetAddOn(weight));
            owners.remove(owner);
            spots--;
            System.out.println("Pet added for " + owner.getFullName() + ".");
        }
    }

    /** After booking: paid separately, until check-in closes. */
    public static void addLater(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        Flight flight = system.findFlightOrNull(booking.getFlightNumber());
        if (flight == null || !booking.isActive() || flight.getStatus() == FlightStatus.CANCELLED) {
            System.out.println(ConsoleStyle.error("Pets can only be added to an active booking on a flight that isn't cancelled."));
            return;
        }
        if (!flight.getAirline().allowsPets()) {
            System.out.println(ConsoleStyle.error(flight.getAirline().getName() + " doesn't carry pets in the cabin."));
            return;
        }
        if (!LocalDateTime.now().isBefore(flight.getDeparture().minusHours(CheckInFlow.CLOSES_HOURS_BEFORE))) {
            System.out.println(ConsoleStyle.error("Pets can only be added until check-in closes, 1 hour before departure."));
            return;
        }
        if (flight.petSpotsLeft() <= 0) {
            System.out.println(ConsoleStyle.error("This flight already has " + Flight.MAX_PETS + " pets."));
            return;
        }
        List<Traveler> owners = eligibleOwners(booking.getSeatedTravelers(), flight.getDeparture().toLocalDate());
        if (owners.isEmpty()) {
            System.out.println("Only a traveler aged 18+ without a pet yet can bring one.");
            return;
        }

        ConsoleStyle.section("Add a Pet");
        Traveler owner = pickOwner(owners);
        double weight = InputHelper.read("Dog's weight in kg: ", PetAddOn::validateWeight);
        PaymentRecord payment = PaymentFlow.collectPayment(PetAddOn.FEE, "Pet in cabin");
        if (payment == null) {
            System.out.println("Pet not added.");
            return;
        }
        owner.setPet(new PetAddOn(weight));
        flight.addPet();
        booking.addPayment(payment);
        system.save();
        System.out.println("Pet added for " + owner.getFullName() + ".");
    }

    private static List<Traveler> eligibleOwners(List<Traveler> travelers, LocalDate travelDate) {
        List<Traveler> owners = new ArrayList<>();
        for (Traveler t : travelers) {
            if (t.isSeated() && t.getPet() == null
                    && PassengerType.ageOn(t.getBirthDate(), travelDate) >= PassengerType.ESCORT_MIN_AGE) {
                owners.add(t);
            }
        }
        return owners;
    }

    private static Traveler pickOwner(List<Traveler> owners) {
        if (owners.size() == 1) {
            return owners.get(0);
        }
        System.out.println("Who is the pet traveling with?");
        for (int i = 0; i < owners.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + owners.get(i).getFullName());
        }
        return owners.get(InputHelper.readChoice("Choice: ", 1, owners.size()) - 1);
    }
}