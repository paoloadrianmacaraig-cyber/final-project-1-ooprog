import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class BaggageFlow {

    private BaggageFlow() {
    }

    public static void chooseFor(Airline airline, List<Traveler> travelers) {
        ConsoleStyle.section("Checked Baggage");
        System.out.println("Every seated traveler already gets " + BaggageOption.HAND_CARRY_KG + " kg of hand-carry.");
        if (airline.getFreeBag() == BaggageOption.NONE) {
            System.out.println(airline.getName() + " doesn't include a checked bag, so any bag is paid.");
        } else {
            System.out.println(airline.getName() + " includes a " + airline.getFreeBag().getKilograms()
                    + " kg checked bag. Bigger bags pay only the difference.");
        }
        List<BaggageOption> options = airline.getBaggageChoices();
        for (Traveler t : travelers) {
            if (!t.isSeated()) {
                System.out.println(t.getFullName() + " (infant) has no checked baggage allowance.");
                continue;
            }
            System.out.println();
            System.out.println("Checked baggage for " + t.getFullName() + ":");
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 1) + " - " + airline.bagMenuLabel(options.get(i)));
            }
            t.setBaggage(options.get(InputHelper.readChoice("Choice: ", 1, options.size()) - 1));
        }
    }

    public static void addLater(Booking booking) {
        AirlineSystem system = AirlineSystem.getInstance();
        Flight flight = system.findFlightOrNull(booking.getFlightNumber());
        if (flight == null || !booking.isActive() || flight.getStatus() == FlightStatus.CANCELLED) {
            System.out.println(ConsoleStyle
                    .error("Baggage can only be added to an active booking on a flight that isn't cancelled."));
            return;
        }
        if (!LocalDateTime.now().isBefore(flight.getDeparture().minusHours(CheckInFlow.CLOSES_HOURS_BEFORE))) {
            System.out.println(ConsoleStyle.error(
                    "Online baggage purchase closed 1 hour before departure. Please ask at the airport counter."));
            return;
        }

        Airline airline = flight.getAirline();
        List<Traveler> eligible = new ArrayList<>();
        for (Traveler t : booking.getSeatedTravelers()) {
            if (!airline.isBiggestBag(t.getBaggage()) && !t.isCheckedIn()) {
                eligible.add(t);
            }
        }
        if (eligible.isEmpty()) {
            System.out.println("No one in this booking can add baggage online (already at " + airline.getName()
                    + "'s biggest bag or already checked in).");
            return;
        }

        ConsoleStyle.section("Add Checked Baggage");
        for (int i = 0; i < eligible.size(); i++) {
            Traveler t = eligible.get(i);
            System.out.println(
                    "  " + (i + 1) + " - " + t.getFullName() + " (now: " + t.getBaggage().getShortLabel() + ")");
        }
        Traveler traveler = eligible.get(InputHelper.readChoice("Choice: ", 1, eligible.size()) - 1);

        List<BaggageOption> upgrades = new ArrayList<>();
        for (BaggageOption option : airline.getBaggageChoices()) {
            if (option.getKilograms() > traveler.getBaggage().getKilograms()) {
                upgrades.add(option);
            }
        }
        System.out.println();
        for (int i = 0; i < upgrades.size(); i++) {
            double difference = airline.priceOf(upgrades.get(i)) - airline.priceOf(traveler.getBaggage());
            System.out.println("  " + (i + 1) + " - " + upgrades.get(i).getKilograms() + " kg   pay "
                    + ConsoleStyle.peso(difference));
        }
        BaggageOption chosen = upgrades.get(InputHelper.readChoice("Choice: ", 1, upgrades.size()) - 1);
        double difference = airline.priceOf(chosen) - airline.priceOf(traveler.getBaggage());

        PaymentRecord payment = PaymentFlow.collectPayment(difference, "Baggage " + chosen.getKilograms() + " kg");
        if (payment == null) {
            System.out.println("Baggage not added.");
            return;
        }
        traveler.setBaggage(chosen);
        booking.addPayment(payment);
        system.save();
        System.out.println(traveler.getFullName() + " now has " + chosen.getKilograms() + " kg of checked baggage.");
    }
}