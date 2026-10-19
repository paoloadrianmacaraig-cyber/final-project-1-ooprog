package Mateo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public final class TravelerDetailsFlow {

    public static final int MAX_SEATED = 4;

    private TravelerDetailsFlow() {
    }

    public static List<Traveler> collect(Flight flight) {
        LocalDate travelDate = flight.getDeparture().toLocalDate();

        while (true) {
            ConsoleStyle.section("Travelers");
            int seatedCount = InputHelper.read("How many travelers need a seat (1-" + MAX_SEATED + ")? ",
                    raw -> parseSeatedCount(raw));

            List<Traveler> travelers = new ArrayList<>();
            for (int i = 1; i <= seatedCount; i++) {
                System.out.println();
                System.out.println("Traveler " + i + " of " + seatedCount);
                travelers.add(readSeatedTraveler(travelDate));
            }

            int adults = countEscortAdults(travelers, travelDate);
            boolean hasChild = travelers.stream().anyMatch(t -> t.getPassengerType() == PassengerType.CHILD);
            if (hasChild && adults == 0) {
                System.out.println();
                System.out.println(ConsoleStyle.error("Children must travel with an adult (18+). Please enter the travelers again."));
                continue;
            }

            if (adults > 0) {
                System.out.println();
                int infants = InputHelper.read("Infants under 2 flying on a lap (0-" + adults + "): ",
                        raw -> parseInfantCount(raw, adults));
                for (int i = 1; i <= infants; i++) {
                    System.out.println();
                    System.out.println("Infant " + i + " of " + infants);
                    travelers.add(readInfant(travelDate));
                }
            }
            return travelers;
        }
    }

    private static Traveler readSeatedTraveler(LocalDate travelDate) {
        String name = InputHelper.read("Full name: ", User::validateFullName);
        LocalDate birthDate = InputHelper.read("Birthdate (YYYY-MM-DD): ",
                raw -> validateSeatedBirthdate(raw, travelDate));
        PassengerType type = PassengerType.fromBirthdate(birthDate, travelDate);
        int age = PassengerType.ageOn(birthDate, travelDate);

        FareDiscount discount = askDiscount(age);
        String discountId = null;
        if (discount != FareDiscount.NONE) {
            discountId = InputHelper.read(discount.getLabel() + " ID number: ", FareDiscount::validateId);
        }
        return new Traveler(name, birthDate, type, discount, discountId);
    }

    private static Traveler readInfant(LocalDate travelDate) {
        String name = InputHelper.read("Infant's full name: ", User::validateFullName);
        LocalDate birthDate = InputHelper.read("Infant's birthdate (YYYY-MM-DD): ",
                raw -> validateInfantBirthdate(raw, travelDate));
        return new Traveler(name, birthDate, PassengerType.INFANT);
    }

    private static FareDiscount askDiscount(int ageOnTravelDate) {
        boolean seniorAllowed = ageOnTravelDate >= PassengerType.SENIOR_MIN_AGE;
        System.out.println("Discount:");
        System.out.println("  1 - None");
        System.out.println("  2 - PWD (20% off + VAT exempt, ID needed)");
        if (seniorAllowed) {
            System.out.println("  3 - Senior Citizen (20% off + VAT exempt, ID needed)");
        }
        int choice = InputHelper.readChoice("Choice: ", 1, seniorAllowed ? 3 : 2);
        if (choice == 2) {
            return FareDiscount.PWD;
        }
        if (choice == 3) {
            return FareDiscount.SENIOR;
        }
        return FareDiscount.NONE;
    }

    private static int countEscortAdults(List<Traveler> travelers, LocalDate travelDate) {
        int count = 0;
        for (Traveler t : travelers) {
            if (PassengerType.ageOn(t.getBirthDate(), travelDate) >= PassengerType.ESCORT_MIN_AGE) {
                count++;
            }
        }
        return count;
    }

    public static int parseSeatedCount(String raw) throws InvalidInputException {
        try {
            return InputHelper.parseWholeNumber(raw, 1, MAX_SEATED, "Number of travelers");
        } catch (InvalidInputException e) {
            throw new InvalidInputException("A booking holds 1 to " + MAX_SEATED + " seated travelers.");
        }
    }

    public static int parseInfantCount(String raw, int adults) throws InvalidInputException {
        try {
            return InputHelper.parseWholeNumber(raw, 0, adults, "Number of infants");
        } catch (InvalidInputException e) {
            throw new InvalidInputException("Each infant needs their own adult (18+) in the booking, so enter 0 to "
                    + adults + ".");
        }
    }


    public static LocalDate validateBirthdate(String raw) throws InvalidInputException {
        LocalDate date = InputHelper.parseDate(raw);
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new InvalidInputException("Birthdate can't be in the future.");
        }
        if (date.isBefore(today.minusYears(120))) {
            throw new InvalidInputException("Birthdate can't be more than 120 years ago.");
        }
        return date;
    }

    public static LocalDate validateSeatedBirthdate(String raw, LocalDate travelDate) throws InvalidInputException {
        LocalDate date = validateBirthdate(raw);
        if (PassengerType.fromBirthdate(date, travelDate) == PassengerType.INFANT) {
            throw new InvalidInputException("This traveler is under 2 on the departure date."
                    + " Infants are added after the seated travelers, without a seat.");
        }
        return date;
    }

    public static LocalDate validateInfantBirthdate(String raw, LocalDate travelDate) throws InvalidInputException {
        LocalDate date = validateBirthdate(raw);
        if (PassengerType.fromBirthdate(date, travelDate) != PassengerType.INFANT) {
            throw new InvalidInputException("This child is 2 or older on the departure date,"
                    + " so they need their own seat.");
        }
        return date;
    }
}