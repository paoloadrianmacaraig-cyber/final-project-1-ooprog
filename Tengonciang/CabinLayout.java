import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * One cabin of an aircraft: its rows, its seat letters and aisle positions,
 * and any row numbers that are skipped (most airlines skip row 13).
 */
public class CabinLayout implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String cabinName;
    private final int firstRow;
    private final int lastRow;
    private final List<ColumnSlot> template;
    private final Set<Integer> skippedRows;

    public CabinLayout(String cabinName, int firstRow, int lastRow, List<ColumnSlot> template, Set<Integer> skippedRows) {
        this.cabinName = cabinName;
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.template = Collections.unmodifiableList(new ArrayList<>(template));
        this.skippedRows = skippedRows;
    }

    public String getCabinName() { return cabinName; }
    public int getFirstRow() { return firstRow; }
    public int getLastRow() { return lastRow; }
    public List<ColumnSlot> getTemplate() { return template; }

    /** Row numbers that really exist, e.g. 1-12 then 14-30 when 13 is skipped. */
    public List<Integer> actualRowNumbers() {
        List<Integer> rows = new ArrayList<>();
        for (int row = firstRow; row <= lastRow; row++) {
            if (!skippedRows.contains(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    public int seatsPerRow() {
        int count = 0;
        for (ColumnSlot slot : template) {
            if (slot.isSeat()) {
                count++;
            }
        }
        return count;
    }
}
