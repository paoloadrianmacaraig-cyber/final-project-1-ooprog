/**
 * Airports the airline serves, by IATA code. Admin and passengers pick from
 * this list instead of typing city names, so "Cebu", "cebu city" and "Cebuu"
 * can never break a search.
 */
public enum Airport {
    MNL("Manila"),
    CEB("Cebu"),
    DVO("Davao"),
    ILO("Iloilo"),
    BCD("Bacolod"),
    PPS("Puerto Princesa"),
    TAG("Bohol-Panglao"),
    KLO("Kalibo"),
    CGY("Cagayan de Oro"),
    ZAM("Zamboanga");

    private final String city;

    Airport(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    /** "Cebu (CEB)" */
    public String getLabel() {
        return city + " (" + name() + ")";
    }
}