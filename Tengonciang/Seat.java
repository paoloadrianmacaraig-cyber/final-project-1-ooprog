import java.io.Serializable;

/**
 * One seat on one flight, e.g. 12A in Economy.
 */
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int row;
    private final char letter;
    private final String cabinName;
    private boolean booked;

    public Seat(int row, char letter, String cabinName) {
        this.row = row;
        this.letter = letter;
        this.cabinName = cabinName;
    }

    public String getLabel() { return row + String.valueOf(letter); }
    public int getRow() { return row; }
    public char getLetter() { return letter; }
    public String getCabinName() { return cabinName; }
    public boolean isBooked() { return booked; }

    public void book() { booked = true; }
    public void release() { booked = false; }

    /** "12a" -> "12A". Only checks the shape; whether the seat exists is the flight's job. */
    public static String normalizeLabel(String raw) throws InvalidInputException {
        String label = raw.replace(" ", "").toUpperCase();
        if (!label.matches("^\\d{1,2}[A-Z]$")) {
            throw new InvalidInputException("Enter a seat like 12A.");
        }
        return label;
    }
}
