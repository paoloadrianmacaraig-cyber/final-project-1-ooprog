import java.io.Serializable;

/**
 * One position across a cabin row: either a seat letter or an aisle.
 * A row template like A B C | D E F is six seats and one aisle.
 */
public class ColumnSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final char letter; // ' ' for an aisle
    private final boolean aisle;

    private ColumnSlot(char letter, boolean aisle) {
        this.letter = letter;
        this.aisle = aisle;
    }

    public static ColumnSlot seat(char letter) {
        return new ColumnSlot(letter, false);
    }

    public static ColumnSlot aisle() {
        return new ColumnSlot(' ', true);
    }

    public boolean isAisle() { return aisle; }
    public boolean isSeat() { return !aisle; }
    public char getLetter() { return letter; }
}
