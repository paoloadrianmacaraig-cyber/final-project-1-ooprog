import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A seating blueprint. Each Flight builds its own fresh seat map from one of
 * these, so two flights on the same aircraft type never share seats.
 *
 * The three types are aircraft actually flown on Philippine domestic routes.
 * Seat letters follow IATA habits: no letter I (looks like 1), window seats
 * keep their letters (A and K on wide-bodies) even in narrower cabins.
 */
public class AircraftType implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<CabinLayout> cabins;

    public AircraftType(String name, List<CabinLayout> cabins) {
        this.name = name;
        this.cabins = Collections.unmodifiableList(cabins);
    }

    public String getName() { return name; }
    public List<CabinLayout> getCabins() { return cabins; }

    /** Turboprop for short routes: Economy only, 2-2. */
    public static AircraftType atr72() {
        List<ColumnSlot> economy = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('B'), ColumnSlot.aisle(),
                ColumnSlot.seat('C'), ColumnSlot.seat('D'));
        return new AircraftType("ATR 72-600", Arrays.asList(
                new CabinLayout("Economy", 1, 18, economy, Set.of(13))));
    }

    /** Narrow-body: Business 2-2 up front, Economy 3-3. */
    public static AircraftType a321() {
        List<ColumnSlot> business = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('C'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('F'));
        List<ColumnSlot> economy = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('B'), ColumnSlot.seat('C'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('E'), ColumnSlot.seat('F'));
        return new AircraftType("Airbus A321", Arrays.asList(
                new CabinLayout("Business", 1, 3, business, Set.of()),
                new CabinLayout("Economy", 6, 33, economy, Set.of(13))));
    }

    /** Same body as the A321 but all Economy, 3-3, the way low-cost airlines fit more seats in. */
    public static AircraftType a321AllEconomy() {
        List<ColumnSlot> economy = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('B'), ColumnSlot.seat('C'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('E'), ColumnSlot.seat('F'));
        return new AircraftType("Airbus A321neo", Arrays.asList(
                new CabinLayout("Economy", 1, 38, economy, Set.of(13))));
    }

    /** Wide-body: Business 1-2-1, Premium Economy 2-4-2, Economy 3-3-3. */
    public static AircraftType a350() {
        List<ColumnSlot> business = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('G'), ColumnSlot.aisle(),
                ColumnSlot.seat('K'));
        List<ColumnSlot> premium = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('C'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('E'), ColumnSlot.seat('F'), ColumnSlot.seat('G'), ColumnSlot.aisle(),
                ColumnSlot.seat('H'), ColumnSlot.seat('K'));
        List<ColumnSlot> economy = Arrays.asList(
                ColumnSlot.seat('A'), ColumnSlot.seat('B'), ColumnSlot.seat('C'), ColumnSlot.aisle(),
                ColumnSlot.seat('D'), ColumnSlot.seat('E'), ColumnSlot.seat('F'), ColumnSlot.aisle(),
                ColumnSlot.seat('H'), ColumnSlot.seat('J'), ColumnSlot.seat('K'));
        return new AircraftType("Airbus A350-900", Arrays.asList(
                new CabinLayout("Business", 1, 5, business, Set.of()),
                new CabinLayout("Premium Economy", 11, 14, premium, Set.of(13)),
                new CabinLayout("Economy", 30, 52, economy, Set.of())));
    }

}
