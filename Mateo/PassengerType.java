

import java.time.LocalDate;
import java.time.Period;

public enum PassengerType {
    ADULT("Adult"),
    CHILD("Child"),
    INFANT("Infant");

    public static final int INFANT_UNDER_AGE = 2;
    public static final int CHILD_UNDER_AGE = 12;
    public static final int ESCORT_MIN_AGE = 18;
    public static final int SENIOR_MIN_AGE = 60;

    private final String label;

    PassengerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static int ageOn(LocalDate birthDate, LocalDate onDate) {
        return Period.between(birthDate, onDate).getYears();
    }

    public static PassengerType fromBirthdate(LocalDate birthDate, LocalDate travelDate) {
        int age = ageOn(birthDate, travelDate);
        if (age < INFANT_UNDER_AGE) {
            return INFANT;
        }
        if (age < CHILD_UNDER_AGE) {
            return CHILD;
        }
        return ADULT;
    }
}
